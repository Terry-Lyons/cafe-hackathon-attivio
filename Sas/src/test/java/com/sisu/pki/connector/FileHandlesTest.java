package com.sisu.pki.connector;

import com.attivio.TestUtils;
import com.sisu.sas7bdat.SassyFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by dave on 10/21/16.
 */
public class FileHandlesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    static {
        TestUtils.initializeLogging();
    }

    private Path pathToSample;
    private SasDataFileScanner scanner;

    @Before
    public void setUp() throws Exception {
        pathToSample = Paths.get(folder.getRoot().getAbsolutePath(), "ae.sas7bdat");

        Files.copy(
                Paths.get(getClass().getResource("/ae.sas7bdat").toURI()),
                pathToSample);

        scanner = new SasDataFileScanner();
        scanner.initializePython();
    }

    @Test
    public void testCanDeleteUnderlyingSasFile() throws IOException {
        SassyFile file = scanner.parseDataFile(pathToSample);
        Assert.assertNotNull(file);

        Assert.assertEquals("AE", file.getDataSetName());
        Assert.assertEquals(1191, file.getRows().size());

        Assert.assertTrue(Files.deleteIfExists(pathToSample));

        Assert.assertEquals("AE", file.getDataSetName());
        Assert.assertEquals(1191, file.getRows().size());

    }
}
