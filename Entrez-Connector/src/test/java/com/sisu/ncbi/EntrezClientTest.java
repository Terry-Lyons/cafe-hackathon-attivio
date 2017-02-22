package com.sisu.ncbi;

import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Random;


public class EntrezClientTest {

    private Logger log = LoggerFactory.getLogger(EntrezClientTest.class.getName());
    private Random rand = new Random((new Date()).getTime());

    private void throttle() {
        try {
            Thread.sleep(1000 * (1 + rand.nextInt(4)));
        } catch (InterruptedException e) {
            log.error("Interrupted during throttling.", e);
        }
    }

    @Test
    public void testSimplestConstructorSetsDefaults() {
        EntrezClient client = new EntrezClient();
        Assert.assertEquals(5, client.maxResults);
        Assert.assertEquals("pubmed", client.targetDb);
    }

    @Test
    public void complexQueryTest() {
        searchAndFetch("diabetes[MeSH Major Topic]) AND (\"2015\"[Date - Publication] : \"3000\"[Date - Publication])", 2);
    }

    @Test
    public void testSingleTermQuery() {
        searchAndFetch("asthma[mesh]", 2);
    }

    private void searchAndFetch(String query, int pageSize) {
        EntrezClient client = new EntrezClient(pageSize);

        ESearchState searchState = doSearch(client, query);
        EFetchState fetchState = doFetch(client, searchState);

        String firstBody = validateResponse(fetchState);

        String[] articles;
        try {
            articles = firstBody.split("<PubmedArticle>");
        }catch(NullPointerException e) {
            Assert.fail("Couldn't split PubmedArticle elements");
            return;
        }

        int firstPageCount = articles.length - 1;
        Assert.assertTrue(String.format("page size matches requested (%d == %d)", pageSize, firstPageCount),
                firstPageCount == pageSize);

        log.info(String.format("First page count: %d", firstPageCount));

    }

    private ESearchState doSearch(EntrezClient client, String testQuery) {
        throttle();

        try {
            ESearchState state = client.search(testQuery);
            assertSearchState(state);

            log.info(String.format("Search State: %s", state));

            return state;

        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException: " + e.getMessage());
        }

        return null;
    }

    private EFetchState doFetch(EntrezClient client, ESearchState searchState) {
        throttle();

        try {
            EFetchState state = client.fetch(searchState);
            assertFetchState(state);
            return state;

        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException: " + e.getMessage());
        }

        return null;
    }

    private String validateResponse(EFetchState state) {
        Response resp = state.response;

        Assert.assertNotNull("response shouldn't be null", resp);

        try {
            return resp.body().string();

        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("IOException getting response body");
        }

        return null;
    }

    private void assertFetchState(EFetchState state) {

        Assert.assertNotNull("search state should not be null", state);

        log.info(String.format("EFetchState: %s", state));
        Assert.assertNotNull("webenv should not be null", state.webEnv);
        Assert.assertNotNull("queryKey should not be null", state.queryKey);
        Assert.assertTrue("RetStart should be 0 or higher", state.retStart >= 0);
        Assert.assertTrue("Next ret start should be correct", state.nextRetStart == state.retStart + state.retMax);
    }

    private void assertSearchState(ESearchState state) {
        Assert.assertNotNull("search state should not be null", state);

        log.info(String.format("ESearchState: %s", state));

        Assert.assertNotNull("webenv should not be null", state.webEnv);
        Assert.assertNotNull("queryKey should not be null", state.queryKey);
    }
}
