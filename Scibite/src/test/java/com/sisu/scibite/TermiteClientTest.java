package com.sisu.scibite;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;


public class TermiteClientTest {

    private String URL;
    private final Logger log = LoggerFactory.getLogger(getClass().getName());

    @Before
    public void setUp() {
        URL = System.getProperty("TERMITE_URL", "");
        Assume.assumeTrue(!URL.isEmpty());
    }

    File getTestFile(String resourcePath) throws URISyntaxException {
        return Paths.get(getClass().getResource(resourcePath).toURI()).toFile();
    }

    @Test
    public void testFiles() throws IOException, URISyntaxException {

        File testDataDir = getTestFile("/scibite");
        if (testDataDir.isDirectory()) {

            File[] testFiles = testDataDir.listFiles(TestFileUtils.TEXT_FILE_FILTER);

            if (testFiles != null && testFiles.length > 0) {

                for (int n = 0; n < testFiles.length; n++) {
                    System.out.println("Testing SciBite with file: " + testFiles[n].getCanonicalPath());
                    testFile(testFiles[n]);
                }

            } else {
                Assert.fail("No test files found in path: " + testDataDir.getCanonicalPath());
            }
        } else {
            Assert.fail("Coudn't find a test directory: " + testDataDir.getCanonicalPath());
        }

    }


    public void testFile(File file) throws IOException {

        final TermiteClient client = new TermiteClient();
        log.info("Testing with file: " + file.getName());
        TermiteResponse response = client.send(new TermiteRequest(URL, TestFileUtils.readFile(file)));

        Assert.assertTrue(response.isSuccess());
        Assert.assertTrue(response.getEntityList().size() > 0);
        Assert.assertTrue(!response.getMetadata().version.isEmpty());
    }

    @Test
    public void testPost() throws IOException, URISyntaxException {
        final TermiteClient client = new TermiteClient();
        TermiteRequest request = new TermiteRequest(URL, TestFileUtils.readFile(getTestFile("/scibite/skeletal_sample.txt")));

        TermiteResponse response = client.send(request, true);

        log.info(String.format("Success? (%s)", response.isSuccess()));
    }

    @Test
    public void testClosingResponseBody() throws Exception {
        final Path pathToSampleResponse = Paths.get(getClass().getResource("/scibite/skeletal_sample.output.json").toURI());
        String sampleJson = Files.readAllLines(pathToSampleResponse, Charset.forName("utf-8"))
                .stream().collect(Collectors.joining("\n"));


        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setBody(sampleJson));

        mockWebServer.start();

        TermiteRequest request = new TermiteRequest(mockWebServer.url("/termite").toString(), "Just a test");
        TermiteResponse response = TermiteClient.send(request, true);

        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(1, mockWebServer.getRequestCount());

        mockWebServer.shutdown();

    }

}
