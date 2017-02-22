package com.sisu.attivio;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.client.ContentStoreClient;
import com.attivio.sdk.client.InputStreamBuilder;
import com.attivio.sdk.ingest.ContentPointer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Mock ContentStore Client Implementation for Tests using NIO.
 * <p>
 * Does not rely on Attivio guts.
 * <p>
 * Created by dave on 9/15/16.
 */
public class MockContentStoreClient implements ContentStoreClient {

    private HashMap<String, ContentPointer> map;

    public MockContentStoreClient() {
        this.map = new HashMap<>();
    }

    @Override
    public void setNamespace(String namespace) {

    }

    @Override
    public ContentPointer store(String id, InputStreamBuilder inputStreamBuilder) throws AttivioException {
        MockContentPointer mcp;
        try {
            mcp = new MockContentPointer(id, inputStreamBuilder.createInputStream());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        map.put(id, mcp);
        return mcp;
    }

    @Override
    public ContentPointer retrieve(String id) throws AttivioException {
        return map.get(id);
    }

    @Override
    public void delete(String id) throws AttivioException {
        map.remove(id);
    }

    @Override
    public void deleteAll() throws AttivioException {
        map.clear();
    }

    @Override
    public long getTotalRecords() throws AttivioException {
        return map.size();
    }

    @Override
    public boolean contains(String id) throws AttivioException {
        return map.containsKey(id);
    }

    private class MockContentPointer implements ContentPointer {

        private static final int MAX_MEGABYTES = 25;
        private String id;
        private int size;
        private byte[] bytes;

        MockContentPointer(String id, InputStream is) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(1024 * MAX_MEGABYTES);
            size = 0;

            int raw = is.read();
            while (raw > -1) {
                buffer.put((byte) raw);
                size++;
                raw = is.read();
            }

            buffer.rewind();
            bytes = new byte[size];
            buffer.get(bytes);

            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public InputStream getStream() throws AttivioException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public String getStoreName() {
            return "mockStore";
        }

        @Override
        public String getExternalUri() {
            return "mock://mockStore";
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        @Override
        public long getSize() {
            return this.size;
        }
    }
}
