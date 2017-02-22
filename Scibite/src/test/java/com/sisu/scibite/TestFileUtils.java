package com.sisu.scibite;

import java.io.*;

public class TestFileUtils {


    public static String readFile(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        StringBuilder builder = new StringBuilder();

        String line = reader.readLine();

        while (line != null) {
            builder.append(line);
            builder.append("\n");
            line = reader.readLine();
        }
        reader.close();

        return builder.toString();
    }

    public static final FilenameFilter XML_FILE_FILTER = new TestFileFilter(".xml");

    public static final FilenameFilter GZIP_XML_FILE_FILTER = new TestFileFilter(".xml.gz");

    public static final FilenameFilter TEXT_FILE_FILTER = new TestFileFilter(".txt");

    private static class TestFileFilter implements FilenameFilter {

        private String ext;

        public TestFileFilter(String ext) {
            this.ext = ext;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(ext);
        }

    }
}
