package com.sisu.pki.connector;

import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.search.SearchDocument;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by dave on 11/11/16.
 */
public class AieIndexScannerTest {

    @Test
    public void testRemovesSystemFields() {
        AieIndexScanner scanner = new AieIndexScanner();
        SearchDocument sdoc = new SearchDocument("id");
        sdoc.addValue(FieldNames.ZONE, "zone");
        sdoc.addValue(FieldNames.SCORE, "score");
        sdoc.addValue(FieldNames.SCORE_EXPLAIN, "explain");
        sdoc.addValue("GoodField", "Hey man");


        IngestDocument doc = scanner.convertSearchDocToIngestDoc(sdoc);
        Assert.assertTrue(doc.containsField("GoodField"));
        Assert.assertFalse(doc.containsField(FieldNames.ID));
        Assert.assertFalse(doc.containsField(FieldNames.ZONE));
        Assert.assertFalse(doc.containsField(FieldNames.SCORE));
        Assert.assertFalse(doc.containsField(FieldNames.SCORE_EXPLAIN));

    }
}
