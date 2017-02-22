import com.attivio.sdk.ingest.IngestDocument
import com.attivio.sdk.ingest.IngestField

/*
    Groovy code for pivoting data from an IngestDocument
 */

log.info "Hey guys!, I see: ${doc}"

def docs = []
def copyFields = ["name", "age", "height"]
def pivotFields = [ ["hobby", "hobby_experience"], ["language", "language_exp"] ]
def pivotOutputNames = ["skill", "experience"]

pivotFields.eachWithIndex { name_list, i ->
    def child = new IngestDocument("${doc.getId()}-${i}")

    copyFields.each { copyField -> child.setField(doc.getField(copyField)) }

    for (int j in 0..pivotOutputNames.size()-1) {
        def ingestField = new IngestField(pivotOutputNames[j])
        ingestField.addValue doc.getFirstValue(name_list[j])

        child.setField ingestField
    }

    docs.add child
}

log.info "done here! docs: ${docs}"
return docs