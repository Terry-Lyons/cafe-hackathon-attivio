package com.sisu.pki.connector;

import com.attivio.TestUtils;
import com.attivio.test.MockMessagePublisher;
import com.attivio.test.ScannerTestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by dave on 10/5/16.
 */
public class GroovyScannerTest {

    static {
        TestUtils.initializeLogging();
    }

    private Path pathToSample;
    private Path pathToScript;
    private SasDataFileScanner scanner;

    @Before
    public void setUp() throws Exception {
        pathToSample = Paths.get(getClass().getResource("/ae.sas7bdat").toURI());
        pathToScript = Paths.get(getClass().getResource("/pivot.groovy").toURI());

        scanner = new SasDataFileScanner();
        scanner.initializePython();
        scanner.setScriptFile(pathToScript.toAbsolutePath().toString());
    }

    @Test
    public void canUseGroovyScriptsOnSasData() throws Exception {
        Path pathToTestFiles = Paths.get(getClass().getResource("/ae.sas7bdat").toURI()).toAbsolutePath();
        MockMessagePublisher mp = new LoggingMockMessagePublisher(100);
        mp.waitForCompletion(10000);

        scanner.setMessagePublisher(mp);
        scanner.setStartDirectory(pathToTestFiles.toString());
        scanner.setWildcardFilter(Arrays.asList("ae.sas7bdat"));

        scanner.start();
        mp.getSavedDocs().get(0).stream()
                .forEach(doc -> {
                    Assert.assertEquals(doc.getFirstValue("junkField").toString(), "junk");
                });
    }
}
