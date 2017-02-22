package com.sisu.pki.connector;

import com.attivio.TestUtils;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.test.MockMessagePublisher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dave on 11/16/16.
 */
public class GroovyScriptedExcelScannerTest {

    private Path pathToSampleFile;
    private Path pathToScript;
    private GroovyScriptedExcelScanner scanner;
    private MockMessagePublisher mp;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    static {
        TestUtils.initializeEnvironment();
    }

    @Before
    public void setUp() throws Exception {
        pathToSampleFile = Paths.get(this.getClass().getResource("/data/excel.xlsx").toURI());
        pathToScript = Paths.get(this.getClass().getResource("/addcolumn.groovy").toURI());

        scanner = new GroovyScriptedExcelScanner();
        scanner.setStartDirectory(pathToSampleFile.toAbsolutePath().toString());
        mp = new MockMessagePublisher();
        scanner.setMessagePublisher(mp);
    }


    @Test
    public void testLoadingExcelFileWithoutScript() throws AttivioException {
        scanner.start();

        Assert.assertEquals(3, mp.getDocumentsPublished());
        for(IngestDocument doc : mp.getDocs()) {
            log.info(doc.toString());
            Assert.assertTrue(doc.containsField("name"));
            Assert.assertTrue(doc.containsField("age"));
            Assert.assertTrue(doc.containsField("weight"));
        }
    }

    @Test
    public void testLoadingExcelFileWithScript() throws AttivioException {
        scanner.setScriptFile(pathToScript.toAbsolutePath().toString());
        scanner.start();

        Assert.assertEquals(3, mp.getDocumentsPublished());
        for(IngestDocument doc : mp.getDocs()) {
            log.info(doc.toString());
            Assert.assertTrue(doc.containsField("name"));
            Assert.assertTrue(doc.containsField("age"));
            Assert.assertTrue(doc.containsField("weight"));
            Assert.assertEquals("findme", doc.getFirstValue("newField").stringValue());
        }

    }
}