package com.sisu.scibite;

import java.util.ArrayList;
import java.util.List;

public class TermiteRequest {

    private String termiteUrl;
    private String text;
    private String output;
    private int maxDocs;
    private final List<String> entities;

    /**
     * Create a new TermiteRequest instance with json String body with default settings.
     *
     * @param termiteUrl url to Termite service
     * @param text json as String
     */
    public TermiteRequest(String termiteUrl, String text) {
        this(termiteUrl, text, "json", 5000);
    }

    /**
     * Create a new TermiteRequest with a lot of the parameters set
     * @param termiteUrl url to Termite service
     * @param text text to send to Termite for processing
     * @param output output format (such as "json")
     * @param maxDocs max number of documents to
     * @param entities Termite entity names to try to extract from text
     */
    public TermiteRequest(String termiteUrl, String text, String output, int maxDocs, String... entities) {

        this.termiteUrl = termiteUrl;
        this.text = text;
        this.output = output;
        this.maxDocs = maxDocs;

        this.entities = new ArrayList<String>();
        if (entities != null) {
            for (int n = 0; n < entities.length; n++) {
                this.entities.add(entities[n]);
            }
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public int getMaxDocs() {
        return maxDocs;
    }

    public void setMaxDocs(int maxDocs) {
        this.maxDocs = maxDocs;
    }

    public List<String> getEntities() {
        return entities;
    }

    public String getTermiteUrl() {
        return termiteUrl;
    }

    public void setTermiteUrl(String termiteUrl) {
        this.termiteUrl = termiteUrl;
    }
}
