package com.sisu.pki.connector;

import com.attivio.TestUtils;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.test.MockMessagePublisher;
import com.attivio.util.AttivioLogger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dave on 12/23/16.
 */
public class SlowTests {

    static {
        TestUtils.initializeLogging();
    }
    private Path pathToSample;
    private SasDataFileScanner scanner;


    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("SlowTests is enabled", System.getProperty("SlowTests", "false").equalsIgnoreCase("true"));
        scanner = new SasDataFileScanner();
        scanner.initializePython();
    }

    @Test
    public void canCrawl64BitBigEndianFile() throws Exception {
        Path pathToFiles = Paths.get(getClass().getResource("/64bit/natlterr1994.sas7bdat").toURI());
        MockMessagePublisher mp = new MockMessagePublisher() {

            private final AttivioLogger log = AttivioLogger.getLogger(this.getClass());
            public long count = 0;

            @Override
            public void feed(IngestDocument... ingestDocuments) throws AttivioException {
                long newCount = count + ingestDocuments.length;
                if(newCount > 1000 && (newCount / 1000 > count / 1000)) {
                    log.info("fed %d documents", newCount);
                }
                count = newCount;
            }

            @Override
            public long getDocumentsPublished() {
                return count;
            }
        };

        scanner.setStartDirectory(pathToFiles.toString());
        scanner.setMessagePublisher(mp);
        scanner.start();

        // we only test doc count, not sure if it's worth testing schema.
        Assert.assertEquals(71212, mp.getDocumentsPublished());

    }
}
