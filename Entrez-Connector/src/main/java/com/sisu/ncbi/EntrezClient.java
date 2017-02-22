package com.sisu.ncbi;

import okhttp3.*;
import okio.ByteString;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Client implementation of basic Entrez APIs (search, fetch). Supports targeting different databases,
 * but defaults to pubmed (only tested with pubmed!).
 *
 * @author dave@sisu.io
 */
public class EntrezClient {

    private static final String BASE_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
    private static final String SEARCH_SERVICE = "esearch.fcgi";
    private static final String ABSTRACT_SERVICE = "efetch.fcgi";
    //private static final String SUMMARY_SERVER = "esummary.fcgi";

    private static final String DEFAULT_ENTREZ_DB = "pubmed";
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.parse("application/xml; charset=utf-8");
    private static final int DEFAULT_RETMAX = 5;
    private static final String DEFAULT_RETMODE = "xml";
    private static final String DEFAULT_RETTYPE = "abstract";

    final int maxResults;
    final String targetDb;

    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    // setting to a singleton per issue #3
    private static final OkHttpClient client = new OkHttpClient();

    //TODO: Where the heck do these go?! See http://www.ncbi.nlm.nih.gov/books/NBK25497/
    //private final String TOOL = "AttivioEntrezClient";
    //private final String EMAIL = "dave@sisu.io";

    public EntrezClient() {
        this(DEFAULT_RETMAX);
    }

    public EntrezClient(int maxResultsPerCall) {
        this(DEFAULT_ENTREZ_DB, maxResultsPerCall);
    }

    public EntrezClient(String targetDb, int maxResultsPerCall) {
        this.maxResults = maxResultsPerCall;
        this.targetDb = targetDb;
    }


    /**
     * Implements the ESearch API from the NCBI E-Utilities
     *
     * @param query NCBI valid query string (see their docs for details on syntax)
     * @return new ESearchState for fetching results
     * @throws IOException if network or HTTP issues occur
     */
    public ESearchState search(String query, int retMax) throws IOException {

        HashMap<String, String> params = new HashMap<>();
        params.put("db", targetDb);
        params.put("term", query);
        params.put("useHistory", "y");
        params.put("retmax", String.valueOf(retMax));

        log.trace("Calling search service...");
        Response response = callPubmed(SEARCH_SERVICE, params);
        ESearchState state = null;

        try {
            Document doc = EntrezXMLUtils.parse(response.body().string());
            Node webEnvNode = doc.selectSingleNode("/eSearchResult/WebEnv");
            Node queryKeyNode = doc.selectSingleNode("/eSearchResult/QueryKey");
            Node countNode = doc.selectSingleNode("/eSearchResult/Count");

            String webEnv = (webEnvNode != null) ? webEnvNode.getText() : "";
            String queryKey = (queryKeyNode != null) ? queryKeyNode.getText() : "";
            long count = (countNode != null) ? Long.parseLong(countNode.getText()) : -1;

            state = new ESearchState(count, query, webEnv, queryKey, retMax);

        } catch (DocumentException d) {
            //TODO: Exception!
            log.error("DocumentException encountered", d);
            d.printStackTrace();

        } catch (NullPointerException npe) {
            //TODO: Exception!
            log.error("NPE encountered", npe);
            npe.printStackTrace();
        } finally {
            try {
                response.close();
            }catch(Exception e) {
                log.error("error closing HTTP response...socket issue?: " + e.getMessage(), e.getCause());
            }
        }
        return state;
    }

    public ESearchState search(String query) throws IOException {
        return search(query, maxResults);
    }


    /**
     * Perform an initial Fetch given a new ESearchState
     * @param state current ESearchState to start a fetch session from
     * @return EFetchState describing the state after the fetch request
     * @throws IOException if network or HTTP issues occur
     */
    public EFetchState fetch(ESearchState state) throws IOException {
        return fetch(state, 0);
    }

    /**
     * Continue fetching with a given, current EFetchState
     * @param state currently active EFetchState
     * @return new EFetchState
     * @throws IOException if network or HTTP issues occur
     */
    public EFetchState fetch(EFetchState state) throws IOException {
        return fetch(state, state.nextRetStart);
    }

    public EFetchState fetch(EBaseState state, int retStart) throws IOException {
        HashMap<String, String> params = new HashMap<>();
        params.put("db", targetDb);
        params.put("query_key", state.queryKey);
        params.put("WebEnv", state.webEnv);
        params.put("rettype", DEFAULT_RETTYPE);
        params.put("retmode", DEFAULT_RETMODE);
        params.put("retstart", Integer.toString(retStart));
        params.put("retmax", Integer.toString(maxResults));

        log.trace("Calling fetch service...");
        Response resp = callPubmed(ABSTRACT_SERVICE, params);

        return new EFetchState(state, resp, retStart);
    }

    /**
     * Uses the OkHttp library to perform a GET to Pubmed.
     * Formerly used a POST, but it seems they changed their web server!
     *
     * @param path   Service path (e.g. esearch.fcgi)
     * @param params list of query params to build into the URL
     * @return Response
     * @throws IOException if network of HTTP issues occur during POST
     */
    private Response callPubmed(String path, Map<String, String> params)
            throws IOException {
        HttpUrl base = HttpUrl.parse(BASE_URL);

        HttpUrl.Builder urlBuilder = base.newBuilder();
        urlBuilder.addPathSegment(path);

        for (String key : params.keySet()) {
            urlBuilder.addQueryParameter(key, params.get(key));
        }

        HttpUrl url = urlBuilder.build();
        log.debug(String.format("[http post] built url: %s", url.toString()));

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        log.debug(String.format("[response] %s", response.toString()));

        return response;
    }
}
