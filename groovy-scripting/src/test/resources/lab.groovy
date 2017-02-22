// Field name translations
def fieldMap = [
    //Define as 'SAS FIELD': 'SDTM FIELD'
    'SUBID': 'USUBJID',
    'VISITID': 'VISITNUM',
    'DT_PREG': 'LBDTC',
    'sas.dataset': 'DOMAIN',
]
def pivotMap = [
    // Lab Code : [Result Field, Unit Field, Other Unit Field]
    'RETIC': ['RS_RETIC', 'PL_RETU_DEC', 'SP_RETUO'],
    'PLT': ['RS_PLT', 'PL_PLTU_DEC', 'SP_PLTUO'],
    'HGB': ['RS_HGB', 'PL_HBGU_DEC', 'SP_HGBUO'],
    'WBC': ['RS_WBC', 'PL_WBCU_DEC', 'SP_WBCUO']
]
def labNameMap = [
    // Lab code: Lab Name
    'RETIC': 'Reticulocyte Count',
    'PLT': 'Platelets',
    'HGB': 'Hemoglobin',
    'WBC': 'White Blood Cell Count'
]

// Rename SAS Fields
fieldMap.each { sasFieldName, stdmFieldName -> rename from:sasFieldName, to:stdmFieldName }

// Set static values
set field:'LBCAT', to:'Hematology'


def docs = []
// since incoming data is flattened, we need to iterate once for each type of lab result embedded
// in the row of data
pivotMap.each { labCode, pivotFields ->

    // generate our new docs/rows by pivoting/depivoting the data, keeping only key STDM fields
    def pivotDocs = pivot on:pivotFields, keeping:['LBCAT', 'DOMAIN', 'LBDTC', 'VISITNUM', 'USUBJID']

    // perform SDTM conversion for the new records (pivotDocs)
    pivotDocs.each { pivotDoc ->

        // copy over the "other" unit if needed
        copy from:pivotFields[2], to:pivotFields[1], whenTo:'Other', onDoc:pivotDoc

        // standardize to SI units
        def newResult = standardizeResult \
                            result: get(field:pivotFields[0], fromDoc:pivotDoc), \
                            unit: get(field:pivotFields[1], fromDoc:pivotDoc)

        // set the new units and scaled result
        set field:pivotFields[0], to:newResult['result'], onDoc:pivotDoc
        set field:pivotFields[1], to:newResult['unit'], onDoc:pivotDoc

        // rename unit/result fields
        rename from:[pivotFields[0], pivotFields[1]], to:['LBSTRESN', 'LBSTRESU'], onDoc:pivotDoc
        removeField field:pivotFields[2], onDoc: pivotDoc

        // set our lab test and code names
        set field:'LBTESTCD', to:labCode, onDoc:pivotDoc
        set field:'LBTEST', to:labNameMap.get(labCode), onDoc: pivotDoc
    }

    // collect the new results
    docs.addAll(pivotDocs)
}
return docs