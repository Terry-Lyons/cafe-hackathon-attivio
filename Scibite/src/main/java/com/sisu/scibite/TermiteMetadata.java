package com.sisu.scibite;

import org.json.JSONObject;

public class TermiteMetadata {

    public final String version;
    public final long timing;

    public TermiteMetadata(JSONObject jsonObject) {
        this.version = jsonObject.getString(TermiteResponseKeys.MetaKeys.TERMITE_VERSION);
        this.timing = jsonObject.getLong(TermiteResponseKeys.MetaKeys.TIMING);
    }

    @Override
    public String toString() {
        return String.format("MetaData: {version: %s, timing: %d}", version, timing);
    }

}
