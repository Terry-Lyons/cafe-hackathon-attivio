package com.sisu.groovy

import com.attivio.sdk.ingest.IngestDocument
import com.attivio.sdk.ingest.IngestField
import com.attivio.sdk.schema.FieldNames
import com.attivio.sdk.search.SearchDocument
import com.ibm.icu.text.SimpleDateFormat
import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Before
import org.junit.Test

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.assertj.core.api.Assertions.*;


/**
 * Test helper functions of GobotScript's
 *
 * Created by dave on 10/7/16.
 */
class GobotFeaturesTest {

    CompilerConfiguration config = new CompilerConfiguration()
    GroovyShell shell;
    IngestDocument doc;

    @Before
    void setUp() {
        config.scriptBaseClass = GobotScript.class.name
        shell = new GroovyShell(this.class.classLoader, new Binding(), config)
        doc = new IngestDocument("test")
    }

    def runScript(String src) {
        return runScript(src, null)
    }
    def runScript(String src, def otherDoc) {
        def script = shell.parse(src)
        assert script instanceof GobotScript

        if(otherDoc) {
            script.properties.put('otherDoc', otherDoc)
        }else {
            script.setDoc(doc)
        }
        return script.run()
    }

    def testRenameOutput(result) {
        assertThat(result).isNotNull()
        assertThat(result).isInstanceOf(IngestDocument.class)

        IngestDocument resultDoc = (IngestDocument)result
        assertThat(resultDoc.getFirstValue('new_name').stringValue()).asString().matches('dave')
        assertThat(resultDoc.getFirstValue('new_age').intValue()).isEqualTo(33)
    }

    @Test
    void testHelperFunctions() {
        doc.setField('name', 'dave')
        doc.setField('age', 33)
        doc.setField('height', 5.5)

        // eachField
        def result = runScript("""
            names = []
            eachField { field -> names.add(field.name) }
            return names
        """)
        assertThat(result).isInstanceOf(List.class)
        assertThat((List)result).contains('name', 'age', 'height')

    }

    @Test
    void canRenameFieldsEasily() {
        doc.setField('name', 'dave')
        doc.setField('age', 33)

        //String src = "rename ['name', 'age'] on doc to ['new_name', 'new_age']; return doc"
        def src = """
            rename from:'name', to:'new_name'
            rename from:'age', to:'new_age'
            return doc
        """
        testRenameOutput runScript(src)

        src = """
            //rename from:['name', 'age'], to:['new_name', 'new_age']
            rename from:'name', to:'new_name'
            rename from:'age', to:'new_age'
            return doc
        """
        testRenameOutput runScript(src)
    }

    @Test
    void canPivotFields() {
        doc.setField('name', 'Dave')
        doc.setField('age', 33)
        doc.setField('hgb_result', 12)
        doc.setField('hgb_unit', 'magic beans')
        doc.setField('wbc_result', 3.14)
        doc.setField('wbc_unit', 'pies')

        def result = runScript("pivot on: ['hgb_result', 'hgb_unit'], keeping: ['name', 'age']")

        assertThat(result).isInstanceOf(List.class).hasSize(1)
        assertThat(result[0]).isInstanceOf(IngestDocument.class)
        IngestDocument resultDoc = (IngestDocument)result[0]
        assertThat(resultDoc.getFirstValue('name').stringValue()).asString().matches('Dave')
        assertThat(resultDoc.getFirstValue('age').intValue()).isEqualTo(33)
        assertThat(resultDoc.fieldNames).contains('hgb_result', 'hgb_unit')

        result = runScript("pivot on: [['hgb_result', 'hgb_unit'],['wbc_result', 'wbc_unit']], keeping: ['name', 'age']")
        assertThat(result).isInstanceOf(List.class).hasSize(2)
        assertThat(result[0]).isInstanceOf(IngestDocument.class)

        IngestDocument resultDoc1 = (IngestDocument)result[0]
        IngestDocument resultDoc2 = (IngestDocument)result[1]

        assertThat(resultDoc1.getFirstValue('name').stringValue()).asString().matches('Dave')
        assertThat(resultDoc1.getFirstValue('age').intValue()).isEqualTo(33)
        assertThat(resultDoc1.fieldNames).contains('hgb_result', 'hgb_unit')
        assertThat(resultDoc1.fieldNames).doesNotContain('wbc_result', 'wbc_unit')

        assertThat(resultDoc2.getFirstValue('name').stringValue()).asString().matches('Dave')
        assertThat(resultDoc2.getFirstValue('age').intValue()).isEqualTo(33)
        assertThat(resultDoc2.fieldNames).doesNotContain('hgb_result', 'hgb_unit')
        assertThat(resultDoc2.fieldNames).contains('wbc_result', 'wbc_unit')

        // shouldn't pivot if fields don't exist
        result = runScript("pivot on: ['junk', 'morejunk'], keeping: ['name', 'age']")
        assertThat(result).isInstanceOf(List.class).hasSize(0)

    }

    def testCopyResult(result, expectedValue) {
        assertThat(result).isInstanceOf(IngestDocument.class)
        IngestDocument resultDoc = (IngestDocument) result
        assertThat(resultDoc.fieldNames).contains('name', 'unit', 'other_unit')
        assertThat(resultDoc.getFirstValue('unit').stringValue()).matches(expectedValue)
    }

    @Test
    void canCopyFieldsConditionally() {
        def testValue = 'magic beans'

        doc.setField('name', 'Dave')
        doc.setField('unit', 'Other')
        doc.setField('other_unit', testValue)

        testCopyResult(runScript("copy from: 'other_unit', to: 'unit'; return doc"), testValue)

        doc.setField('name', 'Dave')
        doc.setField('unit', 'Other')
        doc.setField('other_unit', testValue)

        testCopyResult(runScript("copy from: 'other_unit', to: 'unit', whenTo: 'Other'; return doc"), testValue)

        doc.setField('name', 'Dave')
        doc.setField('unit', 'Other')
        doc.setField('other_unit', testValue)

        // test ignoring the copy when the whenTo doesn't match
        testCopyResult(runScript("copy from: 'other_unit', to: 'unit', whenTo: 'junk'; return doc"), 'Other')

        testCopyResult(runScript("copy from: 'other_unit', to: 'unit', whenFrom: 'junk'; return doc"), 'Other')

        Throwable thrown = catchThrowable({ runScript("copy from: 'other_unit', to: 'unit', whenFrom: 'junk', whenTo: 'junk'"); })
        assertThat(thrown).isInstanceOf(RuntimeException.class).hasMessageContaining("cannot combine whenTo and whenFrom")

        // test doing it on a doc other than the Script's doc property
        IngestDocument otherDoc = new IngestDocument("otherDoc")
        otherDoc.setField('name', 'John')
        runScript("copy from: 'name', to: 'new_name', whenFrom: 'John', onDoc:properties.get('otherDoc')", otherDoc)
        assertThat(otherDoc.fieldNames).contains('name', 'new_name')
        assertThat(otherDoc.getFirstValue('new_name').stringValue()).matches('John')
    }

    @Test
    void testCanRemoveValues() {
        doc.setField('nan', Double.NaN)
        doc.setField('name', 'dave')
        doc.setField('junk', 'dropme')

        def result = runScript("remove values: [Double.NaN, 'dropme'], from: ['nan', 'junk', 'dontexist']")
        assertThat(doc.fieldNames).contains('name')
        assertThat(doc.fieldNames).doesNotContain('nan', 'junk')

    }

    @Test
    void testSettingStaticValues() {
        runScript("set field:'name', to:'Dave'")
        assertThat(doc.fieldNames).contains('name')
        assertThat(doc.getFirstValue('name').toString()).matches('Dave')

        IngestDocument newDoc = new IngestDocument("new")
        runScript("set field:'name', to:'John', onDoc:properties.get('otherDoc')", newDoc)
        assertThat(newDoc.fieldNames).contains('name')
        assertThat(newDoc.getFirstValue('name').toString()).matches('John')
    }

    @Test
    void testGettingValues() {
        doc.setField('name', 'Dave')
        doc.setField('age', 33)
        doc.setField('height', 5.55)
        doc.setField("hours", "17")

        assertThat(runScript("get field:'name'")).matches('Dave')
        assertThat(runScript("get field:'age'")).isEqualTo(33)
        assertThat(runScript("get field:'height'")).isEqualTo(5.55)
        assertThat(runScript("get field:'height', asType:'String'")).matches("5.55")
        assertThat(runScript("get field:'age', asType:String")).matches("33")
        assertThat(runScript("get field:'hours', asType:'integer'")).isEqualTo(17)

    }

    @Test
    void testStandardizingResults() {
        doc.setField('result', 10.0)
        doc.setField('unit', 'g/L')

        // should convert to mmol/L
        def result = runScript("standardizeResult unit:get(field:'unit'), result:get(field:'result')")

        assertThat(result).isInstanceOf(Map.class)
        assertThat(result as Map).containsKeys('result', 'unit')
        assertThat(result['unit']).isEqualTo('mmol/L')
        assertThat(result['result']).isEqualTo(10.0 * 0.0621)

        doc.setField('result', 10.0)
        doc.setField('unit', '10³/µL')
        result = runScript("standardizeResult unit:get(field:'unit'), result:get(field:'result')")

        assertThat(result).isInstanceOf(Map.class)
        assertThat(result as Map).containsKeys('result', 'unit')
        assertThat(result['unit']).isEqualTo('10^9/L')
        assertThat(result['result']).isEqualTo(10.00)
    }

    @Test
    void testCanRemoveFields() {
        doc.setField('name', 'Dave')
        doc.setField('age', 33)

        runScript("removeField field:'name'")
        assertThat(doc.fieldNames).doesNotContain('name')
        assertThat(doc.fieldNames).containsOnly('age')

        IngestDocument otherDoc = new IngestDocument("otherDoc")
        otherDoc.setField('name', 'John')
        otherDoc.setField('age', 33)
        runScript("removeField field:'age', onDoc:properties.get('otherDoc')", otherDoc)

        assertThat(otherDoc.fieldNames).doesNotContain('age')
        assertThat(otherDoc.fieldNames).containsOnly('name')

    }

    @Test
    void testCanParseLocalDate() {
        def fieldName = 'localdate', dateString = '11 Dec 2013', dateFormat = 'd MMM yyyy'

        LocalDate expected = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormat))
        doc.setField fieldName, dateString

        LocalDate ld = runScript "parseDate field: '${fieldName}', format: '${dateFormat}'"

        assertThat(doc.fieldNames).containsOnly(fieldName)
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasSameTimeAs("2013-12-11")
        assertThat(ld).isEqualTo("2013-12-11")
    }

    @Test
    void testCanParseLocalTime() {
        def fieldName = 'localtime', timeString = '16:20', timeFormat = "HH:mm"

        LocalTime expected = LocalTime.parse(timeString)
        doc.setField fieldName, timeString

        LocalTime lt = runScript "parseTime field: '${fieldName}', format: '${timeFormat}'"

        assertThat(doc.fieldNames).containsOnly(fieldName)
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasHourOfDay(16)
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasMinute(20)
        assertThat(lt).isEqualTo("16:20")
    }

    @Test
    void testCanParseDateTime() {
        def fieldName= 'datefield', dateString = '11 Dec 2013 16:20', dateFormat = 'd MMM yyyy HH:mm'

        LocalDateTime expected = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(dateFormat))
        doc.setField fieldName, dateString

        LocalDateTime d = runScript "parseDateTime field: '${fieldName}', format: '${dateFormat}'"

        assertThat(doc.fieldNames).containsOnly(fieldName)
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasYear(expected.getYear())
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasMonth(expected.getMonthValue())
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasDayOfMonth(expected.getDayOfMonth())
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasHourOfDay(expected.getHour())
        assertThat(doc.getFirstValue(fieldName).dateValue()).hasMinute(expected.getMinute())

        assertThat(d).isEqualTo(expected)

    }

    @Test
    void testCanParseLocalDateFromAlternativeDoc() {
        def fieldName = 'localdate', dateString = '11 Dec 2013', dateFormat = 'd MMM yyyy'

        LocalDate expected = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormat))
        IngestDocument otherDoc = new IngestDocument("other")
        otherDoc.setField fieldName, dateString

        LocalDate ld = runScript "parseDate field: '${fieldName}', format: '${dateFormat}', fromDoc: properties.get('otherDoc')", otherDoc

        assertThat(doc.fieldNames).doesNotContain(fieldName)
        assertThat(otherDoc.fieldNames).containsOnly(fieldName)
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasSameTimeAs("2013-12-11")
        assertThat(ld).isEqualTo(expected)
    }

    @Test
    void testCanParseLocalTimeFromAlternativeDoc() {
        def fieldName = 'localtime', timeString = '16:20', timeFormat = "H:mm"

        LocalTime expected = LocalTime.parse(timeString)
        IngestDocument otherDoc = new IngestDocument("other")
        otherDoc.setField fieldName, timeString

        LocalTime lt = runScript "parseTime field: '${fieldName}', format: '${timeFormat}', fromDoc: properties.get('otherDoc')", otherDoc

        assertThat(doc.fieldNames).doesNotContain(fieldName)
        assertThat(otherDoc.fieldNames).containsOnly(fieldName)
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasHourOfDay(16)
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasMinute(20)
        assertThat(lt).isEqualTo(expected)
    }

    @Test
    void testCanParseDateTimeFromAlternativeDoc() {
        def fieldName= 'datefield', dateString = '11 Dec 2013 16:20', dateFormat = 'd MMM yyyy HH:mm'

        LocalDateTime expected = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(dateFormat))
        IngestDocument otherDoc = new IngestDocument("other")
        otherDoc.setField fieldName, dateString

        LocalDateTime d = runScript "parseDateTime field: '${fieldName}', format: '${dateFormat}', fromDoc: properties.get('otherDoc')", otherDoc

        assertThat(doc.fieldNames).doesNotContain(fieldName)
        assertThat(otherDoc.fieldNames).containsOnly(fieldName)
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasYear(expected.getYear())
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasMonth(expected.getMonthValue())
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasDayOfMonth(expected.getDayOfMonth())
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasHourOfDay(expected.getHour())
        assertThat(otherDoc.getFirstValue(fieldName).dateValue()).hasMinute(expected.getMinute())
        assertThat(d).isEqualTo(expected)

    }

    @Test
    void testCanUseDateAndTimeParsingOnSearchDocument() {
        SearchDocument sdoc = new SearchDocument("ugh")
        def dateField = 'dateField', timeField = 'timeField', dateTimeField = 'dateTimeField'
        def dateString = '11 Dec 2013', timeString = '16:20', dateTimeString = "${dateString} ${timeString}"

        sdoc.addValue(dateField, dateString)
        sdoc.addValue(timeField, timeString)
        sdoc.addValue(dateTimeField, dateTimeString)

        def src = """
            parseDate field: '${dateField}', format: 'd MMM yyyy', fromDoc: properties.get('otherDoc')
            parseTime field: '${timeField}', format: 'H:mm', fromDoc: properties.get('otherDoc')
            parseDateTime field: '${dateTimeField}', format: 'd MMM yyyy HH:mm', fromDoc: properties.get('otherDoc')"""
        runScript src, sdoc

        assertThat(sdoc.fieldNames).containsOnly(dateField, timeField, dateTimeField, FieldNames.ID)
    }

    @Test
    void testCanUseDateAndTimeParsingOnStrings() {
        def dateString = '11 Dec 2013', timeString = '16:20', dateTimeString = "${dateString} ${timeString}"
        def dateFormat = 'd MMM yyyy', timeFormat = 'H:mm', dateTimeFormat = 'd MMM yyyy HH:mm'

        def src = """
            return [
                date: parseDate(fromValue: '${dateString}', format: '${dateFormat}'),
                time: parseTime(fromValue: '${timeString}', format: '${timeFormat}'),
                datetime: parseDateTime(fromValue: '${dateTimeString}', format: '${dateTimeFormat}')
            ]"""
        Map output = runScript src


        assertThat(output).hasSize(3)
        assertThat(output).containsKeys('date', 'time', 'datetime')
        assertThat(output.date).isEqualTo(LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormat)))
        assertThat(output.time).isEqualTo(LocalTime.parse(timeString, DateTimeFormatter.ofPattern(timeFormat)))
        assertThat(output.datetime).isEqualTo(LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(dateTimeFormat)))

    }
}
