package com.sisu.pki.connector;

import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by dave on 10/5/16.
 */
public class GroovyScriptedScannerTest {

    Path pathToScript;
    Path pathToBadScript;
    GroovyScriptedScanner scanner;
    QuietMockMessagePublisher mp;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        scanner = new GroovyScriptedScanner() {

            @Override
            public void start() throws AttivioException {
                IngestDocument doc = new IngestDocument("docId");
                doc.setField("name", "dave");
                doc.setField("age", 33);
                doc.setField("language", "english");
                doc.setField("hobby", "running");
                doc.setField("language_exp", 32);
                doc.setField("hobby_experience", 12);

                feed(doc);
            }

            @Override
            public URI getURIToScript() {
                return pathToScript.toUri();
            }
        };
        pathToScript = Paths.get(getClass().getResource("/pivot.groovy").toURI());
        mp = new QuietMockMessagePublisher();
        scanner.setMessagePublisher(mp);

        // set up a temporary script file that's garbage
        pathToBadScript = Paths.get(tempFolder.newFile("bad.groovy").toURI());

        final BufferedWriter writer = Files.newBufferedWriter(pathToBadScript, Charset.defaultCharset(), StandardOpenOption.APPEND);
        writer.write("def x =\n");
        writer.close();

    }

    @Test
    public void canTransformIngestDocsWithGroovyScript() throws AttivioException {
        scanner.start();

        assertThat(mp.docs).hasSize(2);
        mp.docs.stream().forEach(doc ->
            assertThat(doc.getFieldNames())
                    .containsOnlyElementsOf(Arrays.asList("name", "age", "skill", "experience"))
        );

    }

    @Test
    public void leavesDocsAloneWhenNoScriptIsSet() throws Exception {
        pathToScript = mock(Path.class);
        when(pathToScript.toUri()).thenReturn(null);

        scanner.start();

        assertThat(mp.docs).hasSize(1);
        assertThat(mp.docs.get(0).getFieldNames())
                .containsOnlyElementsOf(
                        Arrays.asList("name", "age", "hobby", "hobby_experience", "language", "language_exp")
                );
    }

    @Test
    public void canGetGroovyErrorInLog() throws Exception {
        pathToScript = pathToBadScript;
        Optional result = scanner.runScriptOnIngestDocument(new IngestDocument("junk"));
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void scriptingIsDisabledOnFailure() throws Exception {
        // we want to simulate not having set a script, or having a bogus script
        pathToScript = mock(Path.class);
        when(pathToScript.toUri()).thenReturn(null);

        assertThat(scanner.isScriptingEnabled()).isTrue();
        scanner.start();
        assertThat(scanner.isScriptingEnabled()).isFalse();

        // now we simulate a busted script that doesn't compile
        scanner.setScriptingEnabled(true);
        pathToScript = pathToBadScript;

        assertThat(scanner.isScriptingEnabled()).isTrue();
        scanner.start();
        assertThat(scanner.isScriptingEnabled()).isFalse();

    }

}
