package com.sisu.attivio.transformer.ingest;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.ingest.IngestFieldValue;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.component.ingest.DocumentModifyingTransformer;
import com.attivio.sdk.token.TokenList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


@ConfigurationOptionInfo(
        displayName = "Transpose into Single StringField",
        description = "Transposes multiple fields and their values into a single StringField on the document.",
        groups = {
                @ConfigurationOptionInfo.Group(
                        path = {ConfigurationOptionInfo.PLATFORM_COMPONENT},
                        propertyNames = {"fieldNames", "outputFieldName", "separator", "defaultValue"}
                )
        }
)
public class TransposeIntoSingleStringField implements DocumentModifyingTransformer {

    private ArrayList<String> fieldNames;

    private String outputFieldName;

    private String separator = "";

    private String defaultValue = "";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean processDocument(IngestDocument doc) throws AttivioException {

        if (!containsRequiredFields(doc)) {
            return true;
        }

        int maxValueCount = findMaxValues(doc, fieldNames);

        final IngestField sf = new IngestField(outputFieldName);

        try {
            for (int index = 0; index < maxValueCount; index++) {

                final StringBuilder sb = new StringBuilder();
                final TokenList tokenList = new TokenList();
                final IngestFieldValue sfv = new IngestFieldValue("");

                boolean first = true;
                for (String fieldName : fieldNames) {

                    final IngestField f = getFieldFromDoc(doc, fieldName);
                    final String str = getStringValueAtIndex(f, index);
                    final TokenList tl = getTokenListAtIndex(f, index);

                    if (first) {
                        //deal with separate logic
                        first = false;

                        //make sure we at least keep some details on the language/local detection
                        if (index == 0) {
                            sf.setLocale(f.getLocale());
                        }
                    } else {
                        sb.append(separator);
                    }

                    sb.append(str);
                    tokenList.add(tl);
                }
                sfv.setTokenList(tokenList);
                sfv.setValue(sb.toString());
                sf.addValue(sfv);
            }
        } catch (IndexOutOfBoundsException e) {
            //TODO:
        }

        log.debug("Created field %s: %s", sf.getName(), sf.toString());
        doc.setField(sf);

        return true;
    }


    private boolean containsRequiredFields(IngestDocument doc) {
        //for now, let's assume we need at least 1 field to proceed
        if (fieldNames == null) {
            return false;
        }

        for (String fieldName : fieldNames) {
            if (doc.containsField(fieldName)) {
                return true;
            }
        }

        return false;
    }


    private IngestField getFieldFromDoc(IngestDocument doc, String fieldName) {
        if (doc.containsField(fieldName)) {
            return doc.getField(fieldName);
        }

        return null;
    }


    private TokenList getTokenListAtIndex(IngestField f, int index) {
        if (f != null && index < f.size()) {
            TokenList tl = f.getValue(index).getTokenList();
            if (tl != null) {
                return tl;
            }
        }

        return new TokenList();
    }


    private String getStringValueAtIndex(IngestField f, int index) {
        if (f != null && index < f.size()) {
            return f.getValue(index).stringValue();
        }

        return defaultValue;
    }


    private int findMaxValues(IngestDocument doc, List<String> fieldNames) {
        int max = 0;

        for (String fieldName : fieldNames) {
            if (doc.containsField(fieldName)) {
                IngestField f = doc.getField(fieldName);
                if (f.size() > max) {
                    max = f.size();
                }
            }
        }

        return max;
    }

    @ConfigurationOption(displayName = "Fields to Transpose", description = "List of field names of fields to transpose.", formEntryClass = ConfigurationOption.STRING_LIST)
    public ArrayList<String> getFieldNames() {
        return fieldNames;
    }


    public void setFieldNames(ArrayList<String> fieldNames) {
        this.fieldNames = fieldNames;
    }

    @ConfigurationOption(displayName = "Output Field", description = "Name of output field to populate with a new StringField")
    public String getOutputFieldName() {
        return outputFieldName;
    }


    public void setOutputFieldName(String outputFieldName) {
        this.outputFieldName = outputFieldName;
    }

    @ConfigurationOption(displayName = "Separator Characters", description = "Characeters to use as a seperator between transposed field values")
    public String getSeparator() {
        return separator;
    }


    public void setSeparator(String separator) {
        this.separator = separator;
    }


    @ConfigurationOption(displayName = "Default Placeholder Value", description = "Default string to use when encountering sparse values")
    public String getDefaultValue() {
        return defaultValue;
    }


    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }


}
