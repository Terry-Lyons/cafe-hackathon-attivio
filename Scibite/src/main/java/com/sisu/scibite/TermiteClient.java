package com.sisu.scibite;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class TermiteClient {

    private static final Logger log = LoggerFactory.getLogger(TermiteClient.class.getName());
    private static final OkHttpClient client = new OkHttpClient();


    /**
     * Builds a proper POST request to termite, spoofing a web form (*eyeroll*)
     *
     * @param baseUrl        base url to the Termite service
     * @param termiteRequest input termite request for building the proper POST
     * @return
     */
    private static Request buildPost(HttpUrl baseUrl, TermiteRequest termiteRequest) {

        RequestBody formBody = new FormBody.Builder()
                .add("text", termiteRequest.getText())
                .add("output", termiteRequest.getOutput())
                .add("maxDocs", String.valueOf(termiteRequest.getMaxDocs()))
                .build();

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(formBody)
                .build();

        return request;
    }

    /**
     * Build a proper GET request to Termite, sending the text as part of the query params (ick!)
     *
     * @param baseUrl        base url to the Termite service
     * @param termiteRequest input termite request for building the proper GET
     * @return Request ready to send!
     */
    private static Request buildGet(HttpUrl baseUrl, TermiteRequest termiteRequest) {

        HttpUrl.Builder urlBuilder = baseUrl.newBuilder(termiteRequest.getTermiteUrl());
        urlBuilder.addQueryParameter("text", termiteRequest.getText());
        urlBuilder.addQueryParameter("output", termiteRequest.getOutput());
        urlBuilder.addQueryParameter("maxDocs", String.valueOf(termiteRequest.getMaxDocs()));
        HttpUrl url = urlBuilder.build();

        log.debug(String.format("Built url: %s", url));

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return request;
    }

    public static TermiteResponse send(TermiteRequest termiteRequest) throws IOException {
        return send(termiteRequest, false);
    }

    public static TermiteResponse send(TermiteRequest termiteRequest, boolean doPostInteadOfGet) throws IOException {
        HttpUrl baseUrl = HttpUrl.parse(termiteRequest.getTermiteUrl());

        Request request;
        if (doPostInteadOfGet) {
            request = buildPost(baseUrl, termiteRequest);
        } else {
            request = buildGet(baseUrl, termiteRequest);
        }
        log.debug(String.format("Prepared request: %s", request));

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            log.debug(String.format("Success: %s", response.message()));

            return TermiteResponse.newInstanceFromJson(response.body().byteStream());
        }

        return TermiteResponse.newFailureInstance();
    }

}
