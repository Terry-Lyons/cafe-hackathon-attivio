/*
 * Created by AIE Designer, Tuesday, December 15, 2015
 */
package com.sisu.attivio.transformer.ingest;



import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.component.ingest.DocumentModifyingTransformer;

//TODO: This needs a rewrite/refactor as it uses a lot of hardcoded pubmed/entrez field names.
/**
 * Class ConcatenatePubmedAbstract
 * 
 * Collapses different sections of pubmed abstracts into one indexed text
 * field@author root
 */
@ConfigurationOptionInfo(componentGroups = ConfigurationOptionInfo.COMPONENT_GROUP_MISCELLANEOUS, displayName = "Concatenate Pubmed Abstract", groups = {}, description = "Collapses different sections of pubmed abstracts into one indexed text field")
public class ConcatenatePubmedAbstract implements DocumentModifyingTransformer {
	// Methods from interface "DocumentModifyingTransformer"
	@Override
	public boolean processDocument(IngestDocument doc) throws AttivioException {

		boolean hasSections = false;
		for(IngestField field : doc) {
			if(field.getName().contains("abstract_")) {
				doc.setField("abstract", "");
				hasSections = true;
				break;
			}
		}
		if(hasSections == true) {
			for(IngestField field : doc) {
				if(field.getName().equals("abstract_background")) {
					doc.setField("abstract", field.getFirstValue().stringValue());
				}
				else if(field.getName().equals("abstract_objective")) {
					for(int i = 0; i < field.size(); i++) {
						doc.setField("abstract", doc.getField("abstract").getFirstValue() + " " +
								field.getValue(i).stringValue());
					}
				}
				else if(field.getName().equals("abstract_unassigned")) {
					for(int i = 0; i < field.size(); i++) {
						doc.setField("abstract", doc.getField("abstract").getFirstValue() + " " + 
								field.getValue(i).stringValue());
					}
				}

				else if(field.getName().equals("abstract_methods")) {
					for(int i = 0; i < field.size(); i++) {
						doc.setField("abstract", doc.getField("abstract").getFirstValue() + " " +
								field.getValue(i).stringValue());
					}
				}
				else if(field.getName().equals("abstract_results")) {
					for(int i = 0; i < field.size(); i++) {
						doc.setField("abstract", doc.getField("abstract").getFirstValue() + " " +
								field.getValue(i).stringValue());
					}
				}
				else if(field.getName().equals("abstract_conclusions")) {
					for(int i = 0; i < field.size(); i++) {
						doc.setField("abstract", doc.getField("abstract").getFirstValue() + " " +
								field.getValue(i).stringValue());
					}
				}
			} 
		}
		return true;
	}
}