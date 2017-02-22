package com.sisu.attivio.transformer.ingest;

import com.attivio.platform.transformer.ingest.xml.ExtractXPaths;
import com.attivio.platform.transformer.ingest.xml.ParseXml;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.schema.FieldNames;
import com.attivio.sdk.schema.Schema;
import com.attivio.sdk.server.util.SchemaUtil;
import com.attivio.util.stream.FileInputStreamBuilder;
import com.sisu.attivio.MockContentStoreClient;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;


public class PubmedXPathTest {


    private static final String LASTNAME_XPATH = "/PubmedArticle/MedlineCitation/Article/AuthorList/Author/LastName";
    private static final String FIRSTNAME_XPATH = "/PubmedArticle/MedlineCitation/Article/AuthorList/Author/ForeName";
    private static final String INITIALS_XPATH = "/PubmedArticle/MedlineCitation/Article/AuthorList/Author/Initials";
    private static final String FULL_XPATH = "concat(/" + LASTNAME_XPATH + ", ', ', /" + FIRSTNAME_XPATH + ")";

    private final Logger log = LoggerFactory.getLogger(getClass().getName());

    @Test
    public void testXPaths() throws AttivioException, IOException, URISyntaxException {

        ExtractXPaths transformer = new ExtractXPaths();
        HashMap<String, String> xpaths = new HashMap();
        xpaths.put("author_lastname", LASTNAME_XPATH);
        xpaths.put("author_firstname", FIRSTNAME_XPATH);
        xpaths.put("author_initials", INITIALS_XPATH);
        xpaths.put("full", FULL_XPATH);

        // For some reason, ExtractXPaths needs these set due to relying on
        // a Schema instance for some processing.
        transformer.setSchemaName("default");
        transformer.updateSchemaUtil(new SchemaUtil() {
            @Override
            public Schema getDefaultSchema() {
                return new Schema("default");
            }

            @Override
            public Schema getSchema(String s) {
                return new Schema(s);
            }
        });

        transformer.setXpaths(xpaths);
        transformer.startComponent();

        Path pathToTestFiles = Paths.get(getClass().getResource("/pubmed").toURI());
        Files.list(pathToTestFiles)
                .filter(file -> file.getFileName().toString().endsWith("xml"))
                .forEach(file -> {
                    try {
                        log.info(String.format("Testing with [%s]", file.getFileName()));
                        testFile(transformer, file.toFile());

                    }catch(Exception e) {
                        e.printStackTrace();
                        Assert.fail(String.format("Failed on test file %s", file.getFileName()));
                    }
                });

    }

    private void testFile(ExtractXPaths transformer, File file) throws AttivioException, IOException {
        FileInputStreamBuilder streamBuilder = new FileInputStreamBuilder(file);
        IngestDocument doc = new IngestDocument("test");
        MockContentStoreClient csClient = new MockContentStoreClient();
        doc.setField(FieldNames.CONTENT_POINTER, csClient.store(file.getCanonicalPath(), streamBuilder));

        ParseXml parser = new ParseXml();
        parser.processDocument(doc);

        transformer.processDocument(doc);
        csClient.deleteAll();
        doc.removeField(FieldNames.CONTENT_POINTER);
        doc.removeField(FieldNames.XML_DOM);

        log.info(String.format("%s", doc));
    }
}
