package com.sisu.ncbi;

import okhttp3.Response;

public class EFetchState extends EBaseState {

    public Response response;
    public int retStart;
    public int nextRetStart;

    EFetchState(EBaseState prevState, Response resp) {
        this(prevState, resp, 0);
    }

    EFetchState(EBaseState prevState, Response resp, int retStart) {
        super(prevState);

        this.nextRetStart = retStart + prevState.retMax;
        this.retStart = retStart;
        this.response = resp;
    }

    @Override
    public String toString() {
        return String.format("Fetch State: {retStart: %d, nextRetStart: %d, Response: %s, base: %s}",
                retStart, nextRetStart, response, super.toString());

    }

}
