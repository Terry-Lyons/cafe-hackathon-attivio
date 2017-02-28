package com.sisu.sas7bdat;

import org.jpy.PyLib;
import org.junit.Assume;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by dave on 7/28/16.
 */
public class TestPythonEnv {

    static {
        System.out.println("jpyLib: " + System.getProperty("jpy.jpyLib"));
        System.out.println("jdlLib: " + System.getProperty("jpy.jdlLib"));
        System.out.println("python: " + System.getProperty("jpy.pythonLib"));
    }

    @Test
    public void testCanInitializeJpyOnNix() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Win"));
        URI uri = SASFileReader.class.getResource("/sas7bdat").toURI();
        String uriString = uri.getSchemeSpecificPart().replaceFirst("!/sas7bdat", "").replaceFirst("file:", "");

        System.out.println("uriString: " + uriString);
        SASFileReader.initializePython(Paths.get(uriString));

        System.out.println("LOADED MODULE!");
    }

    @Test
    public void testCanInitializeOnWindowsWithEmbedded() throws Exception {
        Assume.assumeTrue(System.getProperty("os.name").startsWith("Win"));
        URI uri = SASFileReader.class.getResource("/sas7bdat").toURI();
        SASFileReader.initializePython(Paths.get(
            uri.toString().replaceFirst("file:/", "").replaceFirst("jar:", "").replaceFirst("!/sas7bdat", "")
        ));

        System.out.println("LOADED MODULE!");
    }
}
