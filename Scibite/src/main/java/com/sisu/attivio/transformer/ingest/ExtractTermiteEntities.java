package com.sisu.attivio.transformer.ingest;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.error.IndexWorkflowError;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestFieldValue;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.component.ingest.DocumentModifyingTransformer;
import com.sisu.scibite.TermiteClient;
import com.sisu.scibite.TermiteEntity;
import com.sisu.scibite.TermiteRequest;
import com.sisu.scibite.TermiteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


@ConfigurationOptionInfo(
        displayName = "Termite Entity Extraction",
        description = "Sends document text to Termite for Entity enrichment.",
        groups = {
                @ConfigurationOptionInfo.Group(path =
                        {ConfigurationOptionInfo.PLATFORM_COMPONENT},
                        propertyNames = {"termiteUrl", "inputFields"}),
                @ConfigurationOptionInfo.Group(path =
                        {ConfigurationOptionInfo.PLATFORM_COMPONENT, "Advanced Settings"},
                        propertyNames = {"suffixFieldNamesWith_mvs"})
        }
)
public class ExtractTermiteEntities implements DocumentModifyingTransformer {

    private boolean suffixFieldNamesWith_mvs = true;

    private List<String> inputFields;

    private String termiteUrl;

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());


    @Override
    public boolean processDocument(IngestDocument doc) throws AttivioException {

        //We're going to be super simple for now and send a whole blob of all fields to Termite
        StringBuilder sb = new StringBuilder();

        inputFields.stream().filter(doc::containsField).forEach(field -> {
            for (IngestFieldValue fv : doc.getField(field)) {
                String s = fv.stringValue();
                sb.append(s);
                sb.append("\n");
            }
        });

        log.debug(String.format("Sending payload of size %d to Termite...", sb.length()));

        try {
            TermiteRequest request = new TermiteRequest(termiteUrl, sb.toString());
            TermiteResponse response = TermiteClient.send(request, true);

            if (response.isSuccess()) {
                for (TermiteEntity entity : response.getEntityList()) {
                    String fieldName = entity.name.toLowerCase();
                    if (suffixFieldNamesWith_mvs) {
                        fieldName = fieldName + "_mvs";
                    }

                    //TODO: right now we ignore synonyms, so figure this out later.
                    for (String value : entity.valueMap.keySet()) {
                        doc.addValue(fieldName, value);
                    }
                }

                log.debug("Termite successful for doc: " + doc.getId());
                return true;
            }

        } catch (IOException e) {
            throw new AttivioException(IndexWorkflowError.DOCUMENT_WARNING, e.getMessage());
        }

        log.info(String.format("termite did something bad for doc: %s", doc.getId()));
        return false;
    }

    @ConfigurationOption(displayName = "Use dynamic (*_mvs) field names?", description = "If True, Termite entity fields will be given the _mvs field name suffix.")
    public boolean isSuffixFieldNamesWith_mvs() {
        return suffixFieldNamesWith_mvs;
    }


    public void setSuffixFieldNamesWith_mvs(boolean suffixFieldNamesWith_mvs) {
        this.suffixFieldNamesWith_mvs = suffixFieldNamesWith_mvs;
    }

    @ConfigurationOption(displayName = "Input Fields",
            description = "List of input fields to convert to text, concatenate, and send to Termite for enrichment",
            formEntryClass = ConfigurationOption.STRING_LIST)
    public List<String> getInputFields() {
        return inputFields;
    }


    public void setInputFields(List<String> inputFields) {
        this.inputFields = inputFields;
    }


    @ConfigurationOption(displayName = "Termite Url:", description = "Enter value string.")
    public String getTermiteUrl() {
        return termiteUrl;
    }


    public void setTermiteUrl(String termiteUrl) {
        this.termiteUrl = termiteUrl;
    }


}
