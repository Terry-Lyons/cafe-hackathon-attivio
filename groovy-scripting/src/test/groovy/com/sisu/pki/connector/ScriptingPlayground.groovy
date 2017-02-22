package com.sisu.pki.connector

import com.attivio.sdk.ingest.IngestDocument
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*;

/**
 * Created by dave on 10/6/16.
 */
class ScriptingPlayground {

    IngestDocument doc = new IngestDocument("testDoc")

    @Before
    void setUp() {
        doc.addValue("field1", "NaN")
        doc.addValue("field2", 1234.3)
        doc.addValue("field3", Double.NaN)
    }

    @Test
    void replaceNotANumbers() {


        doc.fieldNames.each { name ->
            if(doc.getFirstValue(name).stringValue() == 'NaN') {
                doc.getField(name).removeValue('NaN')
                doc.setField(name, Double.NaN)
            }
        }

        assertThat doc.getFieldNames().size() isEqualTo 3
        assertThat doc.getField('field1').size() isEqualTo(1)
        assertThat doc.getFirstValue('field1').doubleValue() isEqualTo(Double.NaN)
        assertThat doc.getField('field2').size() isEqualTo(1)
        assertThat doc.getField('field3').size() isEqualTo(1)
        assertThat doc.getFirstValue('field3').doubleValue() isEqualTo(Double.NaN)


    }
}
