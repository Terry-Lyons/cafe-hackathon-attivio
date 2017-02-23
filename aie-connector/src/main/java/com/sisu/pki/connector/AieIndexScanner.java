/**
 *
 */
package com.sisu.pki.connector;

import com.attivio.connector.TestModeAware;
import com.attivio.gwt.core.client.util.StringUtils;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.client.AieClientFactory;
import com.attivio.sdk.client.DefaultAieClientFactory;
import com.attivio.sdk.client.SearchClient;
import com.attivio.sdk.client.streaming.StreamingQueryResponse;
import com.attivio.sdk.error.ConnectorError;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.ingest.IngestFieldValue;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.search.*;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOption.OptionLevel;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.annotation.ScannerInfo;
import com.attivio.util.AttivioLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

/**
 * Enables bringing in new documents by executing a user-specified query against
 * an arbitrary AIE instance
 *
 * @author brandon.bogan [original]
 * @author dave [with groovy scripting]
 */
@SuppressWarnings("restriction")
@ScannerInfo(suggestedWorkflow = "ingest", requiresFieldMapping = true)
@ConfigurationOptionInfo(
        displayName = "AIE Index Scanner",
        description = "Executes the given query against the specified AIE index.",
        groups = {
                @ConfigurationOptionInfo.Group(
                        path = ConfigurationOptionInfo.SCANNER,
                        propertyNames = {
                                "hostAddress", "hostBasePort", "query", "queryLanguage", "searchWorkflow", "profile", "joinRollupMode"
                        }),
                @ConfigurationOptionInfo.Group(
                        path = {ConfigurationOptionInfo.SCANNER, "Data Cleansing"},
                        propertyNames = {"scriptFile"})
        })
public class AieIndexScanner extends GroovyScriptedScanner implements TestModeAware {

    private String hostAddress;
    private int hostBasePort;
    private String query;
    private String queryLanguage;
    private String searchWorkflow;
    private String joinRollupMode;
    private String profile;

    private String scriptFile;

    private boolean inTestMode = false;

    private AttivioLogger log = AttivioLogger.getLogger(this);
    private final String[] SYSTEM_FIELDS = new String[]{
            FieldNames.ID, FieldNames.SCORE, FieldNames.SCORE_EXPLAIN, FieldNames.ZONE, FieldNames.FT_RETRY_COUNT,
            FieldNames.FIELD_NAMES, FieldNames.JOIN_CLAUSE_NAME, FieldNames.JOIN_CHILDREN_COUNT, FieldNames.SOURCE_ENGINE,
            FieldNames.STATIC_FIELD_NAME, FieldNames.PROCESSING_FEEDBACK_COMPONENT, FieldNames.PROCESSING_FEEDBACK_ERROR_CODE,
            FieldNames.PROCESSING_FEEDBACK_LEVEL, FieldNames.PROCESSING_FEEDBACK_MESSAGE
    };

    /**
     * @return the hostIP
     */
    @ConfigurationOption(displayName = "AIE Host Address", description = "Address of AIE Instance to query", optionLevel = OptionLevel.Required)
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param hostAddress the hostIP to set
     */
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    /**
     * @return the hostBasePort
     */
    @ConfigurationOption(displayName = "AIE Host Port", description = "Base port of AIE Instance to query", optionLevel = OptionLevel.Required)
    public int getHostBasePort() {
        return hostBasePort;
    }

    /**
     * @param hostBasePort the hostBasePort to set
     */
    public void setHostBasePort(int hostBasePort) {
        this.hostBasePort = hostBasePort;
    }

    /**
     * @return the query
     */
    @ConfigurationOption(displayName = "Query", description = "Query to Execute", formEntryClass = ConfigurationOption.TEXT_AREA, optionLevel = OptionLevel.Required)
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the query language
     */
    @ConfigurationOption(displayName = "Query Language", description = "Use Attivio's SIMPLE or ADVANCED Query Language to execute the query", formEntryClass = ConfigurationOption.FALSE_SWITCH_VALUE, optionLevel = OptionLevel.Required)
    public String getQueryLanguage() {
        return queryLanguage;
    }

    /**
     * @param queryLanguage the Query Language to set
     */
    public void setQueryLanguage(String queryLanguage) {
        if (queryLanguage.toLowerCase().equals(QueryLanguages.SIMPLE.toLowerCase())) {
            this.queryLanguage = QueryLanguages.SIMPLE;
        } else if (queryLanguage.toLowerCase().equals(QueryLanguages.ADVANCED.toLowerCase())) {
            this.queryLanguage = QueryLanguages.ADVANCED;
        } else {
            throw new IllegalArgumentException("Query Language must be either \""
                    + QueryLanguages.SIMPLE + "\" or \"" + QueryLanguages.ADVANCED + "\"");
        }
    }

    /**
     * @return the searchWorkflow
     */
    @ConfigurationOption(displayName = "Search Workflow", description = "The search workflow to use when executing the query", optionLevel = OptionLevel.Required)
    public String getSearchWorkflow() {
        return searchWorkflow;
    }

    /**
     * @param searchWorkflow the searchWorkflow to set
     */
    public void setSearchWorkflow(String searchWorkflow) {
        this.searchWorkflow = searchWorkflow;
    }

    /**
     * @return the profile
     */
    @ConfigurationOption(displayName = "Search Profile", description = "An optional search profile to use when executing the query")
    public String getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public void start() throws AttivioException {

        log.info(
                "AieIndexScanner now executing query: %s "
                        + "with query language: %s, "
                        + "workflow: %s, "
                        + "and search profile: %s",
                this.query, this.queryLanguage, this.searchWorkflow, this.profile);
        // Create a client factory
        AieClientFactory clientFactory = new DefaultAieClientFactory();

        // create an attivio search client
        SearchClient searchClient = clientFactory.createSearchClient(this.hostAddress,
                this.hostBasePort);
        log.trace("Created SearchClient connected to %s:%s", this.hostAddress, this.hostBasePort);

        // Must set a workflow: default workflow is "search"
        searchClient.setClientWorkflow(this.searchWorkflow);

        if (inTestMode) {
            pagedSearch(searchClient);
        } else {
            streamingSearch(searchClient);
        }


    }

    protected QueryRequest buildQueryRequest(long maxRows) {
        QueryRequest request = new QueryRequest(this.query, this.queryLanguage);
        request.setRows(maxRows);

        if (StringUtils.isNotBlank(this.joinRollupMode)) {
            request.setJoinRollupMode(JoinRollupMode.valueOf(joinRollupMode));
            log.trace("Set the query request join rollup mode to : %s", request.getJoinRollupMode());
        }

        // Set the search profile, if one is specified
        if (StringUtils.isNotBlank(this.profile)) {
            request.setSearchProfile(this.profile);
            log.trace("Set the query request search profile to : %s", request.getSearchProfile());
        }

        return request;
    }

    private void streamingSearch(SearchClient searchClient) throws AttivioException {
        StreamingQueryRequest request = new StreamingQueryRequest(buildQueryRequest(Long.MAX_VALUE),
                StreamingQueryRequest.DocumentStreamingMode.FULL_DOCUMENTS);
        request.setStreamFacets(false);

        StreamingQueryResponse response = null;
        try {
            response = searchClient.search(request);
            for (SearchDocument doc : response.getDocuments()) {
                this.feed(this.convertSearchDocToIngestDoc(doc));
            }

        } catch (IOException e) {
            throw new AttivioException(ConnectorError.CRAWL_FAILED, "streaming query failed for: %s", this.query);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    throw new AttivioException(ConnectorError.UNKNOWN_CRAWL_ERROR, "failed to close streaming response");
                }
            }
        }
    }

    private void pagedSearch(SearchClient searchClient) throws AttivioException {
        // Execute search request, get response
        QueryResponse response = searchClient.search(buildQueryRequest(100));
        log.info("Query completed. Result Count: %s", response.getDocuments().getNumberFound());

        // Iterate through documents and feed them
        for (SearchDocument document : response.getDocuments()) {
            this.feed(this.convertSearchDocToIngestDoc(document));
        }
    }

    /**
     * <p>In AIE version 5.x, AttivioDocuments and related components are split
     * into Ingest and Search, so SearchDocuments need to be converted to
     * IngestDocuments before they can be fed into an ingestion worflow.</p>
     * <p>
     * <p>This is accomplished by iterating over each field, then iterating over
     * each field value, and converting that value to an {@code IngestFieldValue}, which
     * is then added to the target {@code IngestDocument} that is being created.</p>
     *
     * @param searchDoc The document to convert
     * @return An {@code IngestDocument} with the same properties and field
     * values as the given {@code SearchDocument}
     */
    protected IngestDocument convertSearchDocToIngestDoc(SearchDocument searchDoc) {
        log.trace("Converting the following SearchDocument to an IngestDocument: %s", searchDoc.toString());
        IngestDocument inDoc = new IngestDocument(searchDoc.getId());

        for (String ignoredField : SYSTEM_FIELDS) {
            searchDoc.removeField(ignoredField);
        }

        for (String field : searchDoc.getFieldNames()) {
            SearchField sf = searchDoc.getField(field);
            IngestField inField = new IngestField(field);
            for (int i = 0; i < sf.size(); i++) {
                IngestFieldValue inVal = IngestFieldValue.valueOf(sf.getValue(i).getValue());
                inField.addValue(inVal);
            }
            inDoc.setField(inField);
        }
        return inDoc;
    }

    @ConfigurationOption(displayName = "Path to a script")
    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    @Override
    public URI getURIToScript() {
        // TODO: for now we assume we're just loading off disk
        if (scriptFile != null) {
            try {
                return Paths.get(scriptFile).toUri();

            } catch (NullPointerException npe) {
                log.error(ConnectorError.CONFIGURATION_WARNING, "couldn't generate URI to scriptFile: " + scriptFile, npe);
            }
        }

        return null;
    }

    @Override
    public void setInTestMode(boolean b) {
        this.inTestMode = b;
    }

    @ConfigurationOption(displayName = "JOIN Rollup Mode")
    public String getJoinRollupMode() {
        return joinRollupMode;
    }

    public void setJoinRollupMode(String joinRollupMode) {
        this.joinRollupMode = joinRollupMode;
    }
}
