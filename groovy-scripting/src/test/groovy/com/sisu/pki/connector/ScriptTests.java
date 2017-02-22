package com.sisu.pki.connector;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;


/**
 * Test capabilities of a compiled Script instance
 *
 * Created by dave on 10/6/16.
 */
public class ScriptTests {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGroovyScriptWorksWhenScriptIsDestroyed() throws Exception {
        File file = tempFolder.newFile();
        Path pathToFile = Paths.get(file.toURI());
        BufferedWriter writer = Files.newBufferedWriter(pathToFile, Charset.defaultCharset(), StandardOpenOption.WRITE);
        writer.write("return doc.getId()");
        writer.close();

        GroovyScriptedScanner scanner = new GroovyScriptedScanner() {
            @Override
            public void start() throws AttivioException {

            }

            @Override
            public URI getURIToScript() {
                return pathToFile.toUri();
            }

        };

        IngestDocument doc1 = new IngestDocument("doc1");
        IngestDocument doc2 = new IngestDocument("doc2");

        // run the first doc so the script gets loaded.
        Optional<Object> result1 = scanner.runScriptOnIngestDocument(doc1);

        // delete our file
        Files.deleteIfExists(pathToFile);

        // run the second doc
        Optional<Object> result2 = scanner.runScriptOnIngestDocument(doc2);

        assertThat(result1.isPresent()).isTrue();
        assertThat(result2.isPresent()).isTrue();

        assertThat(result1.get()).isInstanceOf(String.class).asString().matches("doc1");
        assertThat(result2.get()).isInstanceOf(String.class).asString().matches("doc2");

    }
}
