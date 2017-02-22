package com.sisu.pki.connector;

import com.attivio.connector.MessagePublisher;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.client.InputStreamBuilder;
import com.attivio.sdk.ingest.BulkUpdate;
import com.attivio.sdk.ingest.ContentPointer;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.search.query.Query;
import com.attivio.sdk.security.AttivioAcl;
import com.attivio.sdk.security.AttivioPrincipal;
import com.attivio.sdk.security.AttivioPrincipalKey;

import java.io.IOException;
import java.util.*;

/**
 * Wrapper around a MessagePublisher instanced needed by a Scanner. Hijacks the feeding method to apply
 * scripted transformations to an IngestDocument before the feed method is called by the Scanner.
 *
 * Supports anything that implements the ScriptableScanner interface, so there's no Groovy specific logic
 * in here at the moment.
 *
 * Created by dave on 11/10/16.
 */
public class WrappedMessagePublisher implements MessagePublisher {

    private MessagePublisher givenMessagePublisher;
    private ScriptableScanner scriptableScanner;

    WrappedMessagePublisher(MessagePublisher messagePublisher, ScriptableScanner scriptableScanner) {
        this.givenMessagePublisher = messagePublisher;
        this.scriptableScanner = scriptableScanner;
    }


    @Override
    public UUID getClientId() {
        return null;
    }

    @Override
    public void feed(IngestDocument... ingestDocuments) throws AttivioException {
        for(IngestDocument doc : ingestDocuments) {
            for(IngestDocument result : applyScript(doc)) {
                givenMessagePublisher.feed(result);
            }
        }
    }

    private List<IngestDocument> applyScript(IngestDocument doc) throws AttivioException {
        Optional<Object> result = scriptableScanner.runScriptOnIngestDocument(doc);

        if(result.isPresent()) {
            Object resultObject = result.get();
            if(resultObject instanceof List) {
                List resultList = (List)resultObject;
                ArrayList<IngestDocument> docList = new ArrayList<>(resultList.size());

                for(Object docResult : resultList) {
                    if(docResult instanceof IngestDocument) {
                        docList.add((IngestDocument) docResult);
                    }
                }
                return docList;

            }else if(resultObject instanceof IngestDocument) {
                return Arrays.asList((IngestDocument)resultObject);
            }
        }
        return Arrays.asList(doc);
    }

    @Override
    public ContentPointer put(String s, InputStreamBuilder inputStreamBuilder) throws AttivioException {
        return givenMessagePublisher.put(s, inputStreamBuilder);
    }

    @Override
    public void delete(String... strings) throws AttivioException {
        givenMessagePublisher.delete(strings);
    }

    @Override
    public void delete(String s, Query query) throws AttivioException {
        givenMessagePublisher.delete(s, query);
    }

    @Override
    public void bulkUpdate(BulkUpdate bulkUpdate) throws AttivioException {
        givenMessagePublisher.bulkUpdate(bulkUpdate);
    }

    @Override
    public void startMessageGroup() throws AttivioException {
        givenMessagePublisher.startMessageGroup();
    }

    @Override
    public void endMessageGroup() throws AttivioException {
        givenMessagePublisher.endMessageGroup();
    }

    @Override
    public boolean isMessageGroupInProgress() {
        return givenMessagePublisher.isMessageGroupInProgress();
    }

    @Override
    public boolean isStopped() {
        return givenMessagePublisher.isStopped();
    }

    @Override
    public boolean waitForCompletion(long l) throws AttivioException {
        return givenMessagePublisher.waitForCompletion(l);
    }

    @Override
    public void feed(IngestDocument ingestDocument, AttivioAcl attivioAcl) throws AttivioException {
        for(IngestDocument result : applyScript(ingestDocument)) {
            givenMessagePublisher.feed(result, attivioAcl);
        }
    }

    @Override
    public void feed(AttivioPrincipal attivioPrincipal) throws AttivioException {

    }

    @Override
    public void deletePrincipal(AttivioPrincipalKey attivioPrincipalKey) throws AttivioException {
        givenMessagePublisher.deletePrincipal(attivioPrincipalKey);
    }

    @Override
    public void close() throws IOException {
        givenMessagePublisher.close();
    }
}
