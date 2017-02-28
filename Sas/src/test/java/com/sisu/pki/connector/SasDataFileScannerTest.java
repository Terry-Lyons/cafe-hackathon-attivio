package com.sisu.pki.connector;

import com.attivio.TestUtils;
import com.attivio.connector.MessagePublisher;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.client.InputStreamBuilder;
import com.attivio.sdk.ingest.BulkUpdate;
import com.attivio.sdk.ingest.ContentPointer;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.search.query.Query;
import com.attivio.sdk.security.AttivioAcl;
import com.attivio.sdk.security.AttivioPrincipal;
import com.attivio.sdk.security.AttivioPrincipalKey;
import com.attivio.test.MockMessagePublisher;
import com.attivio.util.AttivioLogger;
import com.sisu.sas7bdat.SassyFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by dave on 7/28/16.
 */
public class SasDataFileScannerTest {

    static {
        TestUtils.initializeLogging();
    }

    private Path pathToSample;
    private SasDataFileScanner scanner;

    @Before
    public void setUp() throws Exception {
        pathToSample = Paths.get(getClass().getResource("/ae.sas7bdat").toURI());

        scanner = new SasDataFileScanner();
        scanner.initializePython();
    }

    @Test
    public void canConvertSampleFile() {
        SassyFile file = scanner.parseDataFile(pathToSample);
        Assert.assertNotNull(file);

        Assert.assertEquals("AE", file.getDataSetName());

        /*
            Via Python tests:

            # we should just be able to fire away and read. it's python, remember!
            assert len(sas_file.rows) == 1191

            for index, row in enumerate(sas_file.rows):
                print('[%s]: %s' % (str(index), str(row)))
                assert row[0] == 'CDISCPILOT01'
                assert row[5] == 'AE'
         */
        Assert.assertEquals(1191, file.getRows().size());
        file.getRows().stream().forEach(row -> {
            Assert.assertEquals("CDISCPILOT01", row.get(0));
            Assert.assertEquals("AE", row.get(5));
        });
    }

    @Test
    public void canCrawlAndLoadSas7bdatFiles() throws Exception {
        Path pathToTestFiles = Paths.get(getClass().getResource("/ae.sas7bdat").toURI()).toAbsolutePath();
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);
        mp.waitForCompletion(10000);

        scanner.setMessagePublisher(mp);
        scanner.setStartDirectory(pathToTestFiles.toString());
        scanner.setWildcardFilter(Arrays.asList("ae.sas7bdat"));

        scanner.start();

        Assert.assertEquals(1191, mp.getDocumentsPublished());

        mp.getSavedDocs().get(0).stream()
                .forEach(doc -> {
                    // System.out.println("Testing doc: " + doc);

                    Assert.assertTrue(doc.getId().startsWith(pathToTestFiles.toString()));
                    Assert.assertNotNull(doc.getFirstValue(SasFieldNames.CREATED_DATE));
                    Assert.assertNotNull(doc.getFirstValue(SasFieldNames.MODIFIED_DATE));

                    Assert.assertEquals("AE", doc.getFirstValue(SasFieldNames.DATASET_NAME).stringValue());
                    Assert.assertEquals("AE", doc.getFirstValue("DOMAIN").stringValue());
                    Assert.assertEquals("CDISCPILOT01", doc.getFirstValue("STUDYID").stringValue());
                });
    }


    @Test
    public void canSkipBadFiles() throws Exception {
        Path pathToBadFile = Paths.get(getClass().getResource("/bad_files/bad.sas7bdat").toURI());
        MockMessagePublisher mp = new MockMessagePublisher();
        mp.waitForCompletion(1000);

        scanner.setStartDirectory(pathToBadFile.toString());
        scanner.setMessagePublisher(mp);

        scanner.start();

        Assert.assertEquals(0, mp.getDocumentsPublished());
    }

    // ISSUE #1
    @Test
    public void canLoadVSFileRelatedToIssue01() throws Exception {
        Path pathToBadFile = Paths.get(getClass().getResource("/issue01/vs.sas7bdat").toURI());
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);
        mp.waitForCompletion(1000);
        mp.setMaxDocs(1337);

        scanner.setStartDirectory(pathToBadFile.toString());
        scanner.setWildcardFilter(Arrays.asList("vs.sas7bdat"));
        scanner.setMessagePublisher(mp);

        try {
            scanner.start();
        }catch(AttivioException ae) {
            Assert.assertTrue(ae.getCause().getMessage().contains("max docs reached"));
        }

        Assert.assertEquals(1337, mp.getDocumentsPublished());
    }

    // RLE compressed files
    @Test
    public void canLoadRLECompressedFiles() throws Exception {
        Path pathToRLEFiles = Paths.get(getClass().getResource("/rle/ae.rle.sas7bdat").toURI());
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);
        mp.waitForCompletion(1000);
        mp.setMaxDocs(700);
        /*
        assert len(sas_file.columns) == 62
        assert len(sas_file.rows) == 638
        */

        scanner.setStartDirectory(pathToRLEFiles.toString());
        scanner.setWildcardFilter(Arrays.asList("ae.rle.sas7bdat"));
        scanner.setMessagePublisher(mp);

        try {
            scanner.start();
        }catch(AttivioException ae) {
            Assert.assertTrue(ae.getCause().getMessage().contains("max docs reached"));
        }

        Assert.assertEquals(638, mp.getDocumentsPublished());
        mp.getSavedDocs().get(0).stream()
                .forEach(doc -> {
                    // System.out.println("Testing doc: " + doc);

                    Assert.assertTrue(doc.getId().startsWith(pathToRLEFiles.toString()));
                    Assert.assertNotNull(doc.getFirstValue(SasFieldNames.CREATED_DATE));
                    Assert.assertNotNull(doc.getFirstValue(SasFieldNames.MODIFIED_DATE));

                    Assert.assertEquals("AE", doc.getFirstValue(SasFieldNames.DATASET_NAME).stringValue());
                    Assert.assertEquals("ADVERSE EVENTS", doc.getFirstValue("PAGENAME").stringValue());
                    // DELETED is a good indicator that RLE worked.
                    Assert.assertEquals("f", doc.getFirstValue("DELETED").stringValue());
                    Assert.assertEquals("240.0", doc.getFirstValue("VISITID").stringValue());
                    Assert.assertTrue(doc.getFirstValue("SUBID").floatValue() > 0);

                });
    }

    @Test
    public void canCrawlMultipleFilesWithSameSchema() throws Exception {
        Path pathToFiles = Paths.get(getClass().getResource("/issue03/same").toURI());
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);

        scanner.setStartDirectory(pathToFiles.toString());
        scanner.setWildcardFilter(Arrays.asList("*.sas7bdat"));
        scanner.setMessagePublisher(mp);

        scanner.start();
        Assert.assertEquals(2382, mp.getDocumentsPublished());

    }

    @Test
    public void canCrawlMultipleFilesWithDifferentSchema() throws Exception {
        Path pathToFiles = Paths.get(getClass().getResource("/issue03/different").toURI());
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);

        scanner.setStartDirectory(pathToFiles.toString());
        scanner.setWildcardFilter(Arrays.asList("*.sas7bdat"));
        scanner.setMessagePublisher(mp);

        scanner.start();

        // we only test doc count, not sure if it's worth testing schema.
        Assert.assertEquals(30834, mp.getDocumentsPublished());

    }


    @Test
    public void canCrawl64BitFile() throws Exception {
        Path pathToFiles = Paths.get(getClass().getResource("/64bit/ex.sas7bdat").toURI());
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);

        scanner.setStartDirectory(pathToFiles.toString());
        scanner.setMessagePublisher(mp);

        scanner.start();

        // we only test doc count, not sure if it's worth testing schema.
        Assert.assertEquals(592, mp.getDocumentsPublished());
        mp.getSavedDocs().get(0).stream()
                .forEach(doc -> System.out.println(doc));

    }

    @Test
    public void canSampleWithoutRaisingErrors() throws Exception {
        Path pathToFiles = Paths.get(getClass().getResource("/issue03/same").toURI());
        MockMessagePublisher mp = new LoggingMockMessagePublisher(5);

        scanner.setStartDirectory(pathToFiles.toString());
        mp.setMaxDocs(10);
        scanner.setMessagePublisher(mp);
        scanner.start();
    }
}
