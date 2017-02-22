package com.sisu.attivio.scanner;

import com.attivio.connector.MessagePublisher;
import com.attivio.connector.Scanner;
import com.attivio.platform.util.sax.StreamSplitter;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.error.ConnectorError;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.annotation.ScannerInfo;
import com.attivio.util.StringUtils;
import com.attivio.util.stream.NonCopyingByteArrayOutputStream;
import com.sisu.ncbi.EFetchState;
import com.sisu.ncbi.ESearchState;
import com.sisu.ncbi.EntrezClient;
import com.sisu.ncbi.EntrezXMLUtils;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * EntrezScanner implements the NCBI Entrez APIs for querying databases.
 *
 * @author dave@sisu.io
 */
@ScannerInfo(suggestedWorkflow = "pubmedIngest")
@ConfigurationOptionInfo(
        displayName = "NCBI Entrez Database Scanner",
        description = "Queries the NCBI Entrez database using their API. For instance, this can fetch Pubmed articles.",
        groups = {
                @ConfigurationOptionInfo.Group(
                        path = ConfigurationOptionInfo.SCANNER,
                        propertyNames = {"queries", "maxResults", "resultsPerQuery"}),
                @ConfigurationOptionInfo.Group(
                        path = {ConfigurationOptionInfo.SCANNER, "Advanced XML Settings"},
                        propertyNames = {"docRootPath", "docIdPath"}),
                @ConfigurationOptionInfo.Group(
                        path = {ConfigurationOptionInfo.SCANNER, "Advanced Entrez Settings"},
                        propertyNames = {"queryField", "entrezDatabase"})
        })
public class EntrezScanner implements Scanner {

    private final Logger log = LoggerFactory.getLogger(getClass().getName());
    private MessagePublisher messagePublisher;

    /**
     * Max page size per fetch request
     */
    public int resultsPerQuery = 30;

    /**
     * Total results to page through (approximately)
     */
    public int maxResults = 1000;

    /**
     * List of Entrez search queries to select content
     */
    public List<String> queries;

    /**
     * Field to store the originating query against Entrez
     */
    public String queryField = "entrez_query_s";

    /**
     * Name of Entrez database e.g. pubmed
     */
    public String entrezDatabase = "pubmed";

    /**** PUBMED STUFF *****/
    public String docIdPath = "MedlineCitation/PMID";
    public String docRootPath = "/PubmedArticleSet/PubmedArticle";


    @Override
    public void start() throws AttivioException {

        if (queries == null || queries.size() == 0) {
            throw new AttivioException(ConnectorError.CONFIGURATION_WARNING, "You must provide at least one query to execute against Entrez.");
        }

        EntrezClient client = new EntrezClient(entrezDatabase, resultsPerQuery);

        for (String query : queries) {
            log.info(String.format("Processing query: %s", query));

            try {
                ESearchState searchState = client.search(query);

                if (searchState != null) {
                    fetch(client, searchState, resultsPerQuery, maxResults);
                } else {
                    final String error = String.format("Search against Entrez database failed using query: %s", query);
                    final AttivioException attivioException = new AttivioException(ConnectorError.CONFIGURATION_WARNING, error);

                    log.error(error, attivioException);
                    throw attivioException;
                }

            } catch (IOException e) {
                final String error = String.format("caught IO exception. Check error log.");
                final AttivioException attivioException = new AttivioException(ConnectorError.CRAWL_FAILED, e.fillInStackTrace(), "caught IO exception. Check error log.");

                log.error(error);
                throw attivioException;
            }
        }
    }
  
  /* *********************************************************************** */

    private long throttle(long lastRequestInMillis) {

        if (System.currentTimeMillis() - lastRequestInMillis < (3 * 1000L)) {
            try {
                Thread.sleep(3 * 1000L);
            } catch (InterruptedException e) {
                //nop
            }
        }

        return System.currentTimeMillis();
    }

    public void fetch(EntrezClient client, ESearchState searchState, int pageSize, int maxResults) throws IOException {
        long totalResults = 0;

        //perform initial fetch
        EFetchState fetchState = client.fetch(searchState);
        totalResults = totalResults + pageSize;

        EntrezXMLHandler handler = new EntrezXMLHandler(searchState.query, queryField, docIdPath, docRootPath, this.messagePublisher);
        Response response = null;

        try {
            response = fetchState.response;

            if (response.isSuccessful()) {
                //parse the stream
                EntrezXMLUtils.processResponseStream(response.body().byteStream(), handler);
                response.close();

                long timeOfLastRequest = System.currentTimeMillis();
                //Throttle
                while (totalResults < maxResults && totalResults < searchState.count) {
                    timeOfLastRequest = throttle(timeOfLastRequest);

                    //subsequent fetch
                    fetchState = client.fetch(fetchState);
                    try {
                        response = fetchState.response;

                        if (response.isSuccessful()) {
                            EntrezXMLUtils.processResponseStream(response.body().byteStream(), handler);
                            totalResults = totalResults + pageSize;

                        } else {
                            log.info(String.format("Fetch failed: %s", fetchState));
                            break;
                        }
                    } finally {
                        response.close();
                    }
                }
            } else {
                log.info(String.format("Fetch failed: %s", fetchState));
            }
        }finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public void setMessagePublisher(MessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @ConfigurationOption(displayName = "Max Results per Query", description = "Max number of results to fetch in total from the Entrez database.")
    public int getMaxResults() {
        return maxResults;
    }


    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @ConfigurationOption(displayName = "Queries", description = "List of pubmed query terms for selecting pubmed abstracts", formEntryClass = ConfigurationOption.STRING_LIST)
    public List<String> getQueries() {
        return queries;
    }


    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

    @ConfigurationOption(displayName = "Field Name for Storing Query Value", description = "field to store the Entrez query used to select the result from the db")
    public String getQueryField() {
        return queryField;
    }

    public void setQueryField(String queryField) {
        this.queryField = queryField;
    }

    @ConfigurationOption(displayName = "Fetch Page Size", description = "Max number of results to fetch per query to Entrez database. Max is 50.")
    public int getResultsPerQuery() {
        return resultsPerQuery;
    }

    public void setResultsPerQuery(int resultsPerQuery) {
        if (resultsPerQuery > 50) {
            resultsPerQuery = 50;
        }
        this.resultsPerQuery = resultsPerQuery;
    }

    @ConfigurationOption(displayName = "XML Doc ID XPath", description = "XPath expression for identifying individual doc id's in the resulting XML docs.")
    public String getDocIdPath() {
        return docIdPath;
    }

    public void setDocIdPath(String docIdPath) {
        this.docIdPath = docIdPath;
    }

    @ConfigurationOption(displayName = "XML Doc Root XPath", description = "XPath expression for identifying how to split resulting XML docs before feeding.")
    public String getDocRootPath() {
        return docRootPath;
    }

    public void setDocRootPath(String docRootPath) {
        this.docRootPath = docRootPath;
    }

    @ConfigurationOption(displayName = "Entrez Database Name", description = "Name of the Entrez database (e.g. \"pubmed\") to query.")
    public String getEntrezDatabase() {
        return entrezDatabase;
    }

    public void setEntrezDatabase(String entrezDatabase) {
        this.entrezDatabase = entrezDatabase;
    }

  /* *********************************************************************** */


    /**
     * Splits PubmedArticleSets coming back from the Entrez database.
     *
     * @author dvoutila
     */
    protected class EntrezXMLHandler extends StreamSplitter {

        private final String query;
        private final String queryField;

        private final Date mtime;

        private final MessagePublisher publisher;

        public static final String PUBMED_QUERY_FIELD = "pubmed_query_s";


        /**
         * Creates a new instance of an EntrezXMLHandler based on Attivio StreamSplitter
         *
         * @param query       String - original Entrez query string
         * @param queryField  String - field to store the query on the resulting AttivioDocument
         * @param idPath      String - xpath to extract a unique AttivioDocument id from the XML
         * @param docRootPath String - xpath to use for splitting the XML into original AttivioDocument's
         * @param publisher   MessagePublisher - reference to a MessagePublisher (usually from the Scanner)
         *                    to use when creating ContentPointers
         */
        public EntrezXMLHandler(String query, String queryField, String idPath, String docRootPath, MessagePublisher publisher) {
            super(query, docRootPath, idPath);

            // Get properties of file
            this.query = query;
            this.queryField = queryField;
            this.mtime = new Date();
            this.publisher = publisher;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void warn(Exception e, String message, Object... args) {
            log.warn(message, e, args);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        protected void handleDocument(int index, String id, NonCopyingByteArrayOutputStream data) throws AttivioException {
            // Feed document
            String docId = getDocumentId(index, id);
            if (StringUtils.isBlank(id) && StringUtils.isBlank(this.getDocumentIdPath())) {
                docId = query + "_" + getDocumentId(index, id);
            }

            if (StringUtils.isBlank(id) && (isIdPathSet()) && !StringUtils.isBlank(this.getDocumentIdPath())) {
                final String warning =
                        String.format("%s Failed to find document id at '%s' for document #%d for query: \"%s\"",
                                ConnectorError.NO_DOCUMENT_ID_FOUND, getDocumentIdPath(), index, query);
                log.warn(warning);

                return;
            }

            final IngestDocument doc = new IngestDocument(docId);

            // truncate the id just in case, the ID and the doc are the same size
            if (docId.length() > 1024) {
                docId = docId.substring(0, 1021) + "...";
            }

            doc.setField(FieldNames.MIME_TYPE, "text/xml");
            doc.setField(FieldNames.DATE, mtime);
            doc.setField(FieldNames.CONTENT_POINTER, publisher.put(doc.getId(), data.getInputStreamBuilder()));
            doc.setField(FieldNames.SIZE, data.size());
            doc.setField(queryField, query);

            log.debug(String.format("query \"%s\": extracted %d documents", query, getDocumentCount()));
            publisher.feed(doc);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endDocument() {
            if (getDocumentCount() == 0) {
                final String warning =
                        String.format("Document root '%s' not found in query: \"%s\"",
                                ConnectorError.NO_DOCUMENT_ROOT_FOUND, this.getDocumentRoot(), this.query);
                log.warn(warning);

            } else {
                log.debug(String.format("query \"%s\": extracted %d documents", query, getDocumentCount()));
            }
        }
    }

}
