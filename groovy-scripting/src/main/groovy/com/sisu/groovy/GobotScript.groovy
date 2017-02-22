package com.sisu.groovy

import com.attivio.sdk.ingest.IngestDocument
import com.attivio.sdk.ingest.IngestField
import com.attivio.sdk.search.SearchDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.*
import java.time.format.DateTimeFormatter

/**
 * Created by dave on 10/6/16.
 */
abstract class GobotScript extends Script {

    Logger log = LoggerFactory.getLogger(this.class.name)

    IngestDocument doc

    Map properties = [:]

    /**
     * <p>
     *     Helper function for running logic against each Field
     * </p>
     * <p>
     *     Examples:
     *     <pre>
     *         eachField { field -> println field }
     *     </pre>
     *
     * @param c Closure to call
     */
    void eachField(Closure c) {
        this.doc.fieldNames.each { String name ->
            IngestField field = doc.getField(name)
            c.call(field)
        }
    }

    /**
     * Rename a field
     *
     * @param renameMap
     */
    void rename(Map renameMap) {
        def from = renameMap.get('from')
        def to = renameMap.get('to')
        def onDoc = renameMap.get('onDoc', doc)

        if (from instanceof String) {
            onDoc.renameField(from, to)

        }else if(from instanceof List) {
            from.eachWithIndex { name, index ->
                if (onDoc.containsField(name)) {
                    onDoc.renameField(name, to[index])
                }
            }
        }
    }

    /**
     * <p>
     *     Pivot a row of data into one or many rows.
     * <p>
     *     Parameters:
     *     <ul>
     *         <li><b>on:</b> field or field list to pivot around, only pivots if
     *         all fields are present/li>
     *         <li><b>keeping:</b> field or field list to copy to the new rows</li>
     *     </ul>
     * @param pivotMap
     * @return
     */
    List pivot(Map pivotMap) {
        List on = pivotMap.get('on')
        def keeping = pivotMap.get('keeping')

        def docs = []

        if (on.get(0) instanceof List) {
            on.each { pivotPoint -> docs.addAll(pivot(on: pivotPoint, keeping: keeping)) }
        }else {
            if(doc.fieldNames.collect().containsAll(on)) {
                // only pivot if we have all the fields requested to pivot "on"
                IngestDocument pivotDoc = new IngestDocument("${doc.id}-on-${on}")
                on.each { fieldName -> pivotDoc.setField(doc.getField(fieldName)) }
                keeping.each { fieldName -> pivotDoc.setField(doc.getField(fieldName)) }

                docs.add(pivotDoc)
            }
        }
        return docs
    }

    /**
     * <p>
     *     Copy values from one field to another under given conditions.
     * </p>
     * <p>
     *     Parameters:
     *     <ul>
     *         <li><b>from:</b> field to copy value from</li>
     *         <li><b>to:</b> field to copy the value to</li>
     *         <li><b>whenTo:</b> (optional) condition on the "to" field</li>
     *         <li><b>whenFrom:</b> (optional) condition on the "from" field</li>
     *     </ul>
     * @param swapMap
     */
    void copy(Map swapMap) {
        def from = swapMap.get('from')
        def to = swapMap.get('to')
        def whenTo = swapMap.get('whenTo', '')
        def whenFrom = swapMap.get('whenFrom', '')
        def onDoc = swapMap.get('onDoc', doc)

        if (whenTo && whenFrom) {
            throw new RuntimeException("cannot combine whenTo and whenFrom")

        } else if (whenTo) {
            def value = onDoc.getFirstValue(to).stringValue()
            if (!(value =~ whenTo)) {
                return
            }
        }else if(whenFrom) {
            def value = onDoc.getFirstValue(from).stringValue()
            if (!(value =~ whenFrom)) {
                return
            }
        }

        IngestField toField = new IngestField(to)
        toField.addValue(onDoc.getFirstValue(from))

        onDoc.setField(toField)

    }

    /**
     * Remove field values
     * @param renameMap
     */
    void remove(Map renameMap) {
        def values = renameMap.get('values')
        def from = renameMap.get('from')

        if(!(values instanceof List)) {
            values = [values]
        }
        if(!(from instanceof List)) {
            from = [from]
        }
        from.each { fieldName ->
            if(doc.containsField(fieldName)) {
                IngestField field = doc.getField(fieldName)
                for (value in values) {
                    if (field.getFirstValue().getValue() == value) {
                        doc.removeField(field.name)
                        break
                    }
                }
            }
        }

    }

    void removeField(Map removeMap) {
        def field = removeMap.get('field')
        def onDoc = removeMap.get('onDoc', doc)

        onDoc.removeField(field)
    }

    /**
     * Set a value or values on a given field or set of fields
     * @param setMap
     */
    void set(Map setMap) {
        def field = setMap.get('field')
        def fields = setMap.get('fields')
        def to = setMap.get('to')
        def onDoc = setMap.get('onDoc', doc)

        if(field) {
            if(onDoc instanceof IngestDocument) {
                onDoc.setField(field, to)
            }else if(onDoc instanceof SearchDocument) {
                onDoc.addValue(field, to)
            }
        }else if (fields instanceof List) {
            fields.each { fieldName ->
                set field:fieldName, to:to, onDoc:onDoc
            }
        }
    }

    /**
     * Get a value from the row of data
     * @param getMap
     */
    def get(getMap) {
        def field = getMap.get('field')
        def type = getMap.get('asType', '')
        def fromDoc = getMap.get('fromDoc', doc)

        if (type instanceof Class) {
            type = type.simpleName
        }

        def defaultValue = getMap.get('default')

        if(fromDoc.containsField(field)) {
            def value = fromDoc.getFirstValue(field)
            switch(type.toLowerCase()) {

                case 'string':
                    return value.stringValue()

                case 'number':
                    return value.doubleValue()

                case 'integer':
                    return value.intValue()

                case 'float':
                    return value.floatValue()

                case 'double':
                    return value.doubleValue()

                case 'decimal':
                    return value.decimalValue()

                default:
                    return value.getValue()
            }
        }

        return defaultValue

    }

    // -------------- UNIT CONVERSION --------------
    def unitMap = [
        '10³/µL': [ scale: 1.0, unit: '10^9/L' ],
        '10Â³/ÂµL': [ scale: 1.0, unit: '10^9/L'],
        '10^3/uL': [ scale: 1.0, unit: '10^9/L'],
        'TH/mm3': [ scale: 1.0, unit: '10^9/L' ],
        'thou/uL': [ scale: 1.0, unit: '10^9/L' ],
        'K/UL': [ scale: 1.0, unit: '10^9/L'],
        'K/cumm': [ scale: 1.0, unit: '10^9/L'],
        'g/L': [ scale: 0.0621, unit: 'mmol/L'],
        'g/dL': [ scale: 0.6206, unit: 'mmol/L']
    ]

    def standardizeResult(Map resultMap) {
        def unit = resultMap.get('unit')
        def result = resultMap.get('result')

        if(unitMap.containsKey(unit)) {
            result = result * unitMap.get(unit)['scale']
            unit = unitMap.get(unit)['unit']
        }
        return ['unit': unit, 'result': result]
    }

    def parseDate(Map confMap) {
        String field = confMap.field as String
        String format = confMap.get('format', '') as String
        def fromDoc = confMap.get('fromDoc', doc)
        def fromValue = confMap.get('fromValue')

        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE
        if(!format.isEmpty()) {
            dtf = DateTimeFormatter.ofPattern(format)
        }

        def val
        if(fromValue) {
            val = fromValue
        } else {
            val = get field: field, asType: String, default: null, fromDoc: fromDoc
        }
        if(val != null) {
            def ld = LocalDate.from(dtf.parse(val))
            set field: field, to: _LocalDateToDate(ld), onDoc: fromDoc
            return ld
        }

        return null
    }

    def parseTime(Map confMap) {
        String field = confMap.field as String
        String format = confMap.get('format', 'HH:mm') as String
        def fromDoc = confMap.get('fromDoc', doc)
        def fromValue = confMap.get('fromValue')

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format)

        def val
        if(fromValue) {
            val = fromValue
        } else {
            val = get field: field, asType: String, default: null, fromDoc: fromDoc
        }
        if(val != null) {
            def lt = LocalTime.from(dtf.parse(val))
            set field: field, to: _LocalTimeToDate(lt), onDoc: fromDoc
            return lt
        }

        return null
    }

    def parseDateTime(Map confMap) {
        String field = confMap.field as String
        String format = confMap.get('format', '') as String
        def fromDoc = confMap.get('fromDoc', doc)
        def fromValue = confMap.get('fromValue')

        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME
        if(!format.isEmpty()) {
            dtf = DateTimeFormatter.ofPattern(format)
        }

        def val
        if(fromValue) {
            val = fromValue
        } else {
            val = get field: field, asType: String, default: null, fromDoc: fromDoc
        }
        if(val != null) {
            def ldt = LocalDateTime.from(dtf.parse(val))
            set field: field, to: _LocalDateTimeToDate(ldt), onDoc: fromDoc
            return ldt
        }

        return null
    }

    def _LocalDateTimeToDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant())
    }

    def _LocalDateToDate(LocalDate ld) {
        Instant i = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
        return Date.from(i)
    }

    def _LocalTimeToDate(LocalTime lt) {
        Instant i = lt.atDate(LocalDate.ofEpochDay(0)).atZone(ZoneId.systemDefault()).toInstant()
        return Date.from(i)
    }
}
