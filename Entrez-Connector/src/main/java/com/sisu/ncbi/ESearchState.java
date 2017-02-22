package com.sisu.ncbi;

public class ESearchState extends EBaseState {

    public String query;

    public long count;

    ESearchState(long count, String query, String webEnv, String queryKey, int retMax) {
        this(query, webEnv, queryKey, retMax);
        this.count = count;
    }

    ESearchState(String query, String webEnv, String queryKey, int retMax) {
        super(webEnv, queryKey, retMax);
        this.query = query;
    }

    @Override
    public String toString() {
        return String.format("Search State: {count: %d, query: %s, base: %s}", count, query, super.toString());
    }
}
