package com.sisu.scibite;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class TermiteJsonParsingTest {

    private final static String TESTFILE = "/scibite/sample.json";

    @Test
    public void testJsonParsing() throws IOException, URISyntaxException {

        InputStream is = Files.newInputStream(
                Paths.get(getClass().getResource(TESTFILE).toURI()),
                StandardOpenOption.READ);

        TermiteResponse response = TermiteResponse.newInstanceFromJson(is);

        System.out.println("version: " + response.getMetadata().version);
        for (TermiteEntity entity : response.getEntityList()) {
            System.out.println(entity);
        }
    }

}
