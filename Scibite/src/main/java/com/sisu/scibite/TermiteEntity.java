package com.sisu.scibite;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TermiteEntity {

    public final String name;
    /**
     * map of entity values ('name's) and their synonyms
     **/
    public final Map<String, List<String>> valueMap = new HashMap<>();

    public TermiteEntity(String name, JSONArray array) {
        this.name = name;

        for (int n = 0; n < array.length(); n++) {
            //each element SHOULD be a JSONObject
            JSONObject obj = array.getJSONObject(n);
            String value = obj.getString(TermiteResponseKeys.EntityKeys.VALUE);

            JSONArray synonyms = obj.getJSONArray(TermiteResponseKeys.EntityKeys.SYNONYMS);
            ArrayList<String> synonymList = new ArrayList<String>();

            if (synonyms != null) {
                for (int m = 0; m < synonyms.length(); m++) {
                    String synonym = synonyms.getString(m);
                    if (!synonym.equalsIgnoreCase(value)) {
                        synonymList.add(synonym);
                    }
                }
            }

            this.valueMap.put(value, synonymList);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Entity: {type: %s", name));
        sb.append(", values:[");

        boolean firstValue = true;
        for (String key : valueMap.keySet()) {

            if (firstValue) {
                sb.append(String.format("{\"%s\":[", key));
                firstValue = false;
            } else {
                sb.append(String.format(", {\"%s\":[", key));
            }

            boolean firstSynonym = true;
            for (String syn : valueMap.get(key)) {
                if (firstSynonym) {
                    sb.append(String.format("\"%s\"", syn));
                    firstSynonym = false;
                } else {
                    sb.append(String.format(", \"%s\"", syn));
                }
            }
            sb.append("]} ");
        }
        sb.append("]} ");
        return sb.toString();
    }
}
