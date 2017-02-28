package com.sisu.pki.connector;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.test.MockMessagePublisher;
import com.attivio.util.AttivioLogger;

/**
 * Created by dave on 10/5/16.
 */
class LoggingMockMessagePublisher extends MockMessagePublisher {

    final AttivioLogger log = AttivioLogger.getLogger(LoggingMockMessagePublisher.class);
    long frequency;

    public LoggingMockMessagePublisher(long frequency) {
        this.frequency = frequency;
    }

    @Override
    public void feed(IngestDocument... docs) throws AttivioException {
        super.feed(docs);
        if (super.getDocumentsPublished() % frequency == 0) {
            log.info("...fed %s docs.", super.getDocumentsPublished());
        }
    }
}
