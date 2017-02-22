package com.sisu.pki.connector;

import com.attivio.connector.visitor.TreeNode;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by dave on 11/10/16.
 */
public class GroovyScriptedAbstractFileScannerTest {

    Path pathToScript;
    GroovyScriptedAbstractFileScanner scanner;
    QuietMockMessagePublisher mp;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        scanner = new GroovyScriptedAbstractFileScanner() {

            @Override
            protected void crawlFile(TreeNode treeNode) throws AttivioException {
                IngestDocument doc = new IngestDocument("docId");
                doc.setField("name", "dave");
                doc.setField("height", 33);
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
        scanner.setStartDirectory(Paths.get(getClass().getResource("/data").toURI()).toString());
        scanner.setWildcardFilter(Arrays.asList("dm.csv"));
    }

    @Test
    public void canTransformIngestDocsWithGroovyScript() throws AttivioException {
        scanner.start();

        assertThat(mp.docs).hasSize(2);
        mp.docs.stream().forEach(doc ->
                assertThat(doc.getFieldNames())
                        .containsOnlyElementsOf(Arrays.asList("name", "height", "skill", "experience"))
        );

    }
}
