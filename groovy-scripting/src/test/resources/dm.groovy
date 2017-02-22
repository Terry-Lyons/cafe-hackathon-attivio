
// Field name translations
def fieldMap = [
        //Define as 'SAS FIELD': 'SDTM FIELD'
        'SITENUM': 'SITEID',
        'PL_RACE_DEC': 'RACE',
        'PL_SEX_DEC': 'Sex',
        'PL_CTRY_DEC': 'Country',
        'CC_AGE': 'AGE',
        'SUBID': 'USUBJID',
        'sas.dataset': 'DOMAIN'
]

fieldMap.each { sasFieldName, stdmFieldName ->
    rename from: sasFieldName, to: stdmFieldName
}

// scrub NaN
doc.fieldNames.each { name ->
    if(doc.getFirstValue(name).stringValue() == 'NaN') {
        doc.getField(name).removeValue('NaN')
        doc.setField(name, Double.NaN)
    }
}

return doc