/**
 * Copyright 2015 Attivio Inc., All rights reserved.
 */
package com.acme;

import java.util.Date;
import java.util.List;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.search.SearchDocument;
import com.attivio.sdk.search.SearchDocumentList;
import com.attivio.sdk.search.QueryFeedback;
import com.attivio.sdk.search.QueryResponse;
import com.attivio.sdk.server.component.query.ResponseTransformer;

/** Sample response transformer that adds a new document to the results. */
public class SampleResponseTransformer implements ResponseTransformer {

  @Override
  public void processResponseInfo(QueryResponse info) throws AttivioException {
    // you can modify the QueryResponse here if necessary
    List<QueryFeedback> qfList = info.getFeedbackByMessageName("sampleProcessedResponse");
    QueryFeedback qf = new QueryFeedback(this.getClass().getSimpleName(), "sampleProcessedResponseInfo",
        "also added an additional feedback message to the query's QueryResponse");
    qfList.add(qf);
    info.setFeedback(qfList);
  }

  @Override
  public void processResponseDocuments(QueryResponse info, SearchDocumentList documents) throws AttivioException {
    // here we'll add an additional response document
    SearchDocument doc4 = new SearchDocument("4");
    doc4.addValue("title", "document 4");
    doc4.addValue("cat", "cat1");
    doc4.addValue("date", new Date());
    documents.add(doc4);
    // adding feedback is optional but is useful for letting end users know what happened.
    info.addFeedback(this.getClass().getSimpleName(), "sampleProcessedResponse", "added a new response document");
  }

}
