package com.sisu.attivio.transformer.ingest;

import com.attivio.sdk.ingest.IngestDocument;
import org.junit.Assert;
import org.junit.Test;

/**
 * Quick unit test for the transformer written by unknown ("root"?!?) author.
 *
 * Created by dave on 9/19/16.
 *
 * @author dave@sisu.io
 *
 */
public class ConcatenatePubmedAbstractTest {

    @Test
    public void testTransformer() {
        final String abstractField = "abstract";
        final String background = "This is my background.";
        final String objective = "This was our objective.";

        IngestDocument doc = new IngestDocument("pubmedDoc");
        doc.setField("abstract_background", background);
        doc.setField("abstract_objective", objective);

        Assert.assertFalse(doc.containsField(abstractField));

        ConcatenatePubmedAbstract transformer = new ConcatenatePubmedAbstract();

        try {
            transformer.processDocument(doc);
        }catch(Exception e) {
            Assert.fail(String.format("Failed due to exception: %s", e.getMessage()));
            e.printStackTrace();
        }

        Assert.assertTrue(doc.containsField(abstractField));
        String abstractString = doc.getFirstValue(abstractField).stringValue();
        Assert.assertTrue(abstractString.contains(background));
        Assert.assertTrue(abstractString.contains(objective));
        Assert.assertEquals(
                "Should have a new abstract that's the length of all components plus a space inserted before each, " +
                "with the exception of abstract_background (the presumed start?)",
                abstractString.length(),
                background.length() + objective.length() + 1
        );

    }
}
