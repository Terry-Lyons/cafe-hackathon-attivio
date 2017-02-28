import com.attivio.sdk.ingest.IngestField

IngestField field = new IngestField('junkField')
field.addValue('junk')
doc.setField field

return doc