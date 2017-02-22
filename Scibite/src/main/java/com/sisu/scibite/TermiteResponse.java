package com.sisu.scibite;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TermiteResponse {

    private final boolean isSuccess;

    private TermiteMetadata metadata;

    private List<TermiteEntity> entityList = new ArrayList<>();


    public static TermiteResponse newFailureInstance() {
        return new TermiteResponse(false);
    }

    public static TermiteResponse newInstanceFromJson(InputStream is) {
        TermiteResponse response = new TermiteResponse(true);

        JSONObject json = new JSONObject(new JSONTokener(is));

        if (json != null) {
            if (json.has(TermiteResponseKeys.METADATA)) {
                JSONObject meta = json.getJSONObject(TermiteResponseKeys.METADATA);
                response.setMetadata(new TermiteMetadata(meta));
            }

            if (json.has(TermiteResponseKeys.PAYLOAD)) {
                JSONObject payload = json.getJSONObject(TermiteResponseKeys.PAYLOAD);

                for (Object key : payload.keySet()) {
                    //SHOULD be a string key. Duh.
                    String keyName = key.toString();
                    TermiteEntity entity = new TermiteEntity(keyName, payload.getJSONArray(keyName));

                    response.getEntityList().add(entity);
                }
            }
        }

        return response;
    }

    public static TermiteResponse newInstanceFromJson(String rawJson) {
        return newInstanceFromJson(rawJson, StandardCharsets.UTF_8.name());
    }

    public static TermiteResponse newInstanceFromJson(String rawJson, String encoding) {
        return newInstanceFromJson(new ByteArrayInputStream(rawJson.getBytes(Charset.forName(encoding))));
    }

    private TermiteResponse(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public TermiteMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(TermiteMetadata metadata) {
        this.metadata = metadata;
    }

    public List<TermiteEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TermiteEntity> entityList) {
        this.entityList = entityList;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

}
