/**
 * Copyright 2015 Attivio Inc., All rights reserved.
 */
package com.acme;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.attivio.TestUtils;
import com.attivio.app.Attivio;
import com.attivio.app.config.Configuration;
import com.attivio.bus.PsbProperties;
import com.attivio.client.ContentFeeder;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.client.AieClientFactory;
import com.attivio.sdk.client.DefaultAieClientFactory;
import com.attivio.sdk.client.SearchClient;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.search.QueryRequest;
import com.attivio.sdk.search.QueryResponse;

/**
 * Sample integration test.
 *
 * This integration test can be used for basic integration testing
 * as well as debugging/tracing throughout the entire system.
 *
 */
public class SampleIntegrationTest {

  private final AieClientFactory clientFactory = new DefaultAieClientFactory();

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
  public void simpleTest() throws Exception {
    try (ContentFeeder feeder = new ContentFeeder()) {
      Configuration cfg = TestUtils.getConfiguration("attivio.xml");
      //'attivio.xml' should be replaced with your project's main configuration file
      //ImportConfiguration ic = new ImportConfiguration(new File("projectDirPath\\conf"));
      //Configuration cfg = TestUtils.getRuntimeConfigurationFromImportedConfiguration((ic.get()));
      
      
      Attivio.getInstance().start(cfg, true);

      // feed some content
      feeder.setIngestWorkflowName("ingest");
      IngestDocument doc = new IngestDocument("1234");
      doc.setField(FieldNames.TITLE, "test 123");
      feeder.feed(doc);
      feeder.commit();
      feeder.waitForCompletion();

      // now search for documents
      SearchClient searcher = clientFactory.createSearchClient();
      QueryRequest req = new QueryRequest("*:*");
      QueryResponse resp = searcher.search(req);
      Assert.assertEquals(1, resp.getTotalRows());

    } finally {
      Attivio.getInstance().shutdown();
    }

  }

}