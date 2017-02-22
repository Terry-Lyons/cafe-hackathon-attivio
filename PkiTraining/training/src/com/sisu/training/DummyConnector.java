package com.sisu.training;

import com.attivio.connector.AbstractScanner;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;


public class DummyConnector extends AbstractScanner {
	
	@Override
	public void start() throws AttivioException {
		IngestDocument doc = new IngestDocument("DUMMYCONNECTOR");
		doc.addValue("title", "This is a dummy document");

		feed(doc);
	}

}
