package com.sisu.attivio.transformer.ingest;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.token.TokenList;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;


public class TransposeIntoSingleStringFieldTest {

    private Logger log = LoggerFactory.getLogger(getClass().getName());

    @Test
    public void test() throws AttivioException {

        IngestDocument doc = new IngestDocument("test");
        doc.setField("field1", "Dave", "Dan", "Jess", "Nora", "Ryan");

        IngestField sf = new IngestField("field2");
        sf.addValues("Voutila", "Voutila", "Cormier");
        sf.getValue(0).setTokenList(new TokenList("Voutila"));
        sf.getValue(0).setTokenList(new TokenList("Voutila"));
        sf.getValue(1).setTokenList(new TokenList("Voutila", "Cormier"));
        doc.setField(sf);

        IngestField intField = new IngestField("field3");
        intField.addValue(32);
        intField.addValue(26);
        intField.addValue(29);
        doc.setField(intField);

        log.info(String.format("BEFORE:\n%s", doc));

        TransposeIntoSingleStringField trans = new TransposeIntoSingleStringField();
        ArrayList<String> fields = new ArrayList<>();
        fields.add("field1");
        fields.add("field2");
        fields.add("field3");
        trans.setFieldNames(fields);
        trans.setDefaultValue("<<DEFAULT VAL!>>");
        trans.setOutputFieldName("output");
        trans.setSeparator("__");


        trans.processDocument(doc);

        log.info(String.format("AFTER:\n%s", doc));

        Assert.assertTrue("Doc contains output field", doc.containsField(trans.getOutputFieldName()));
        Assert.assertTrue("Doc's output field is a StringField", doc.getField(trans.getOutputFieldName()).getFirstValue().getValue() instanceof String);
        Assert.assertTrue("Doc contains proper number of field values", doc.getField(trans.getOutputFieldName()).size() == doc.getField("field1").size());

    }


}
