package com.sisu.attivio.transformer.ingest;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.schema.FieldNames;
import com.sisu.scibite.TestFileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ExtractTermiteEntitiesTest {

    private String URL;

    @Before
    public void setUp() {
        URL = System.getProperty("TERMITE_URL", "");
        Assume.assumeTrue(!URL.isEmpty());
    }

    @Test
    public void test() throws IOException, AttivioException, URISyntaxException {
        File file = Paths.get(getClass().getResource("/scibite/skeletal_sample.txt").toURI()).toFile();
        String sampleText = TestFileUtils.readFile(file);
        IngestDocument doc = new IngestDocument("testDoc");
        doc.setField("text", sampleText);

        ExtractTermiteEntities transformer = new ExtractTermiteEntities();

        ArrayList<String> inputFields = new ArrayList<>();
        inputFields.add("text");

        transformer.setInputFields(inputFields);
        transformer.setTermiteUrl(URL);
        transformer.setSuffixFieldNamesWith_mvs(true);

        System.out.println("Before:\n" + doc.toString());
        Assert.assertTrue(doc.containsField(FieldNames.TEXT));

        System.out.println("Processing doc. Result: " + transformer.processDocument(doc));
        Assert.assertTrue(doc.containsField(FieldNames.TEXT));
        // Should have a few key entities for our skelatal sample
        Assert.assertTrue(doc.getField("moa_mvs").getFirstValue().stringValue().equalsIgnoreCase("Inhibitor"));
        Assert.assertArrayEquals(
                new String[]{"bone element", "bone tissue"},
                new String[]{
                        doc.getField("anat_mvs").getValue(0).stringValue(),
                        doc.getField("anat_mvs").getValue(1).stringValue()
                }
        );
        System.out.println("After:\n" + doc.toString());
    }

}
