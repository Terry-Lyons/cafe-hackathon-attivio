/**
 * Copyright 2015 Attivio Inc., All rights reserved.
 */
package com.acme;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.attivio.DocumentAssert;
import com.attivio.TestUtils;
import com.attivio.bus.PsbProperties;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.ingest.IngestFieldValue;

public class SampleAdvancedIngestTransformerTest {

  private String inputField = "testin";
  private String outputField = "testout";

  @BeforeClass
  public static void initializeTestEnvironment() throws AttivioException, IOException {
    PsbProperties.setProperty("log.printStackTraces", true);
    PsbProperties.setProperty("log.level", "INFO");
    PsbProperties.setProperty("attivio.project", System.getProperty("user.dir"));
    PsbProperties.setProperty("log.directory", System.getProperty("user.dir") + "/build/logs");
    PsbProperties.setProperty("data.directory", System.getProperty("user.dir") + "/build/data");
    TestUtils.initializeEnvironment();
  }

  @Test
  public void testTransformer() throws AttivioException {
    // Initialize a transformer object
    SampleAdvancedIngestTransformer xformer = new SampleAdvancedIngestTransformer();
    Map<String, String> tmpMap = new HashMap<String, String>();
    tmpMap.put(inputField, outputField);
    xformer.setFieldMapping(tmpMap);
    IngestDocument doc = new IngestDocument("doc0001");
    doc.setField(inputField, "THIS IS A SAMPLE UPPER CASE DOCUMENT.");
    IngestField f = doc.getField(inputField);

    for (IngestFieldValue fv : f) {
      IngestFieldValue tmp = xformer.createMappedValue(inputField, fv);
      String outF = xformer.getFieldMapping().get(inputField);
      if (outF != null) {
        doc.addValue(outF, tmp);
      }
    }

    // print the document to see what it looks like
    System.err.println(doc.toString());

    // Assert that the document's text field is now all in lower case
    DocumentAssert.assertFieldValue(doc, outputField, "this is a sample upper case document.");
  }

}
