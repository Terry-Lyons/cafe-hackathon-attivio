/**
 * Copyright 2015 Attivio Inc., All rights reserved.
 */
package com.acme;

import java.util.Map;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestFieldValue;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.component.ingest.FieldValueCreatingTransformer;

/**
 * Sample document transformer that will convert the contents of the
 * input field to lower case during document ingestion.
 */
@ConfigurationOptionInfo(
    displayName = "Sample Advanced Ingest Transformer",
    description = "Advanced transformer sample code provided by the SDK",
    groups = {
        @ConfigurationOptionInfo.Group(path = ConfigurationOptionInfo.PLATFORM_COMPONENT,
            propertyNames = { "fieldMapping" }
        )
    })
public class SampleAdvancedIngestTransformer implements FieldValueCreatingTransformer<String> {

  private Map<String, String> fieldMapping = null;

  @Override
  public IngestFieldValue createMappedValue(String inputFieldName, IngestFieldValue fv) throws AttivioException {
    if (fv.getValue() instanceof String) {
      String tmp = fv.stringValue();
      tmp = tmp.toLowerCase();
      return new IngestFieldValue(tmp);
    } else {
      System.err.printf("Not a string field: %s", inputFieldName);
      return null;
    }
  }

  @Override
  public Map<String, String> getFieldMapping() {
    return fieldMapping;
  }

  @Override
  public void setFieldMapping(Map<String, String> value) {
    fieldMapping = value;
  }
}