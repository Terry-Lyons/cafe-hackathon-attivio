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
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/**
 * Log4J free MockMessagePublisher.
 *
 * Created by dave on 10/5/16.
 */
public class QuietMockMessagePublisher implements MessagePublisher {

    public ArrayList<IngestDocument> docs = new ArrayList<>();

    @Override
    public UUID getClientId() {
        return null;
    }

    @Override
    public void feed(IngestDocument... ingestDocuments) throws AttivioException {
        Collections.addAll(docs, ingestDocuments);
    }

    @Override
    public ContentPointer put(String s, InputStreamBuilder inputStreamBuilder) throws AttivioException {
        return null;
    }

    @Override
    public void delete(String... strings) throws AttivioException {

    }

    @Override
    public void delete(String s, Query query) throws AttivioException {

    }

    @Override
    public void bulkUpdate(BulkUpdate bulkUpdate) throws AttivioException {

    }

    @Override
    public void startMessageGroup() throws AttivioException {

    }

    @Override
    public void endMessageGroup() throws AttivioException {

    }

    @Override
    public boolean isMessageGroupInProgress() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean waitForCompletion(long l) throws AttivioException {
        return false;
    }

    @Override
    public void feed(IngestDocument ingestDocument, AttivioAcl attivioAcl) throws AttivioException {

    }

    @Override
    public void feed(AttivioPrincipal attivioPrincipal) throws AttivioException {

    }

    @Override
    public void deletePrincipal(AttivioPrincipalKey attivioPrincipalKey) throws AttivioException {

    }

    @Override
    public void close() throws IOException {

    }
}
