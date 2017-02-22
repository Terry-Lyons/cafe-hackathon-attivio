package com.sisu.attivio.scanner;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestFieldValue;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.test.MockMessagePublisher;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;


@SuppressWarnings("restriction")
public class EntrezScannerTest {

    private Logger log = LoggerFactory.getLogger(getClass().getName());

    @Test
    public void testEntrezScannerStart() throws AttivioException, IOException {

        EntrezScanner scanner = new EntrezScanner();
        ArrayList<String> queries = new ArrayList<>();
        queries.add("ams");
        queries.add("hape");

        scanner.setQueries(queries);
        scanner.setResultsPerQuery(100);
        scanner.setMaxResults(25);

        MockMessagePublisher mmp = new MockMessagePublisher();
        scanner.setMessagePublisher(mmp);
        scanner.start();

        Assert.assertTrue(mmp.getDocCount() > 0);

        if (mmp.getDocCount() > 0) {
            for (IngestDocument doc : mmp.getDocs()) {
                final String id = doc.getId();

                IngestFieldValue cp = doc.getField(FieldNames.CONTENT_POINTER).getFirstValue();
                InputStream is = cp.contentPointerValue().getStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));

                log.info(String.format("Doc:: %s - %s%s%s", id, reader.readLine(), reader.readLine(), reader.readLine()));


            }
        }
    }

    @Test
    public void testMultiPageFetching() throws AttivioException, IOException {
        EntrezScanner scanner = new EntrezScanner();
        ArrayList<String> queries = new ArrayList<>();
        queries.add("ams");
        scanner.setQueries(queries);
        MockMessagePublisher mmp = new MockMessagePublisher();
        scanner.setMessagePublisher(mmp);

        // should result in 5 fetches
        scanner.setResultsPerQuery(5);
        scanner.setMaxResults(25);

        scanner.start();

        Assert.assertEquals(25, mmp.getDocCount());
    }

    @Test
    public void testNPEReportedByMavis() throws Exception {

        EntrezScanner scanner = new EntrezScanner();
        ArrayList<String> queries = new ArrayList<>();
        //queries.add("diabetes[MeSH Major Topic]) AND (\"2015\"[Date - Publication] : \"3000\"[Date - Publication]");
        queries.add("acute mountain sickness AND iron "); //as of 15 sep 2016 should return 9 results

        scanner.setQueries(queries);
        scanner.setResultsPerQuery(50);
        scanner.setMaxResults(15500);

        MockMessagePublisher mmp = new MockMessagePublisher();
        scanner.setMessagePublisher(mmp);

        scanner.start();

        Assert.assertTrue(mmp.getDocCount() > 0);
        log.info("Number of docs created: " + mmp.getDocCount());
    }

}
