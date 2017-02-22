package com.sisu.ncbi;

abstract class EBaseState {

    String webEnv;
    String queryKey;
    int retMax;

    EBaseState(String webEnv, String queryKey, int retMax) {
        this.webEnv = webEnv;
        this.queryKey = queryKey;
        this.retMax = retMax;
    }

    EBaseState(EBaseState state) {
        this(state.webEnv, state.queryKey, state.retMax);
    }

    @Override
    public String toString() {
        return String.format("{webEnv: %s, queryKey: %s, retMax: %d}", webEnv, queryKey, retMax);
    }
}
