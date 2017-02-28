package com.sisu.pki.connector;

import com.attivio.connector.visitor.TreeNode;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.error.ConnectorError;
import com.attivio.sdk.ingest.IngestDocument;
import com.attivio.sdk.ingest.IngestField;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.annotation.ScannerInfo;
import com.attivio.util.AttivioLogger;
import com.sisu.sas7bdat.Column;
import com.sisu.sas7bdat.Row;
import com.sisu.sas7bdat.SASFileReader;
import com.sisu.sas7bdat.SassyFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;


/**
 * SAS7BDAT File Connector
 *
 * Created by dave on 7/28/16.
 */
@ScannerInfo(suggestedWorkflow = "ingest", requiresFieldMapping = true )
@ConfigurationOptionInfo(
        displayName="SAS Data File Connector",
        description="Crawls and parses SAS sas7bdat files.",
        groups = {
                @ConfigurationOptionInfo.Group(
                        path={"Scanner"},
                        propertyNames={"fileSystem", "startDirectory", "wildcardFilter"}),
                @ConfigurationOptionInfo.Group(
                        path={"Scanner", "Data Cleansing"},
                        propertyNames={"scriptFile"}),
                @ConfigurationOptionInfo.Group(
                        path={"Scanner", "Advanced JPY Config"},
                        propertyNames={
                            "jdlPropertyName", "jpyPropertyName", "pythonLibPropertyName",
                            "sas7bdatPropertyName", "isOverridingSystemProps"
                        }),
                @ConfigurationOptionInfo.Group(
                        path={"Scanner", "Advanced"},
                        propertyNames = { "followSymbolicLinks", "maxDepth", "maxFileSize"}
                )
        })
public class SasDataFileScanner extends GroovyScriptedAbstractFileScanner {

    private AttivioLogger log = AttivioLogger.getLogger(this);

    private String scriptFile;

    private String jdlPropertyName = "attivio.jpy.jdlLib";
    private String jpyPropertyName = "attivio.jpy.jpyLib";
    private String pythonLibPropertyName = "attivio.jpy.pythonLib";
    private String sas7bdatPropertyName = "attivio.jpy.sas7bdat";
    private boolean isOverridingSystemProps = true;

    public SasDataFileScanner() {
        super.wildcardFilter = new String[] {"*.sas7bdat"};
    }

    @Override
    public void start() throws AttivioException {
        configurePython();
        initializePython();
        super.start();
    }

    /**
     * Wire up the System properties that JPY relies on, since that library doesn't let us expclitly pass
     * them during initialization.
     */
    void configurePython() {
        if(System.getProperty("jpy.jdlLib", "").isEmpty() || isOverridingSystemProps) {
            System.setProperty("jpy.jdlLib", System.getProperty(jdlPropertyName, ""));
        }

        if(System.getProperty("jpy.jpyLib", "").isEmpty() || isOverridingSystemProps) {
            System.setProperty("jpy.jpyLib", System.getProperty(jpyPropertyName, ""));
        }

        if(System.getProperty("jpy.pythonLib", "").isEmpty() || isOverridingSystemProps) {
            System.setProperty("jpy.pythonLib", System.getProperty(pythonLibPropertyName, ""));
        }

        if(System.getProperty("jpy.sas7bdat", "").isEmpty() || isOverridingSystemProps) {
            System.setProperty("jpy.sas7bdat", System.getProperty(sas7bdatPropertyName, ""));
        }
    }

    void initializePython() throws AttivioException {
        try {

            Path pathToPythonModule;

            String sas7bdatProperty = System.getProperty("jpy.sas7bdat");
            if(sas7bdatProperty != null && !sas7bdatProperty.isEmpty()) {
                log.info("Loading sas7bdat module from %s", sas7bdatProperty);
                pathToPythonModule = Paths.get(sas7bdatProperty);
            }else {
                URI uri = null;
                if(!System.getProperty("attivio.project", "").isEmpty()) {
                    log.info("Running inside Attivio, so assuming system classloader helps.");
                    uri = ClassLoader.getSystemResource("com/sisu/sas7bdat").toURI();
                }else {
                    uri = SASFileReader.class.getResource("/sas7bdat").toURI();
                }
                log.info("Trying to load sas7bdat module from jar: %s", uri);
                if(System.getProperty("os.name").startsWith("Win")) {
                    pathToPythonModule = Paths.get(
                            uri.toString()
                                    .replaceFirst("file:/", "")
                                    .replaceFirst("jar:", "")
                                    .replaceFirst("!/.*", "")
                    );
                }else {
                    pathToPythonModule = Paths.get(
                        uri.getSchemeSpecificPart().replaceFirst("!/.*", "").replaceFirst("file:", "")
                    );
                }
            }

            SASFileReader.initializePython(new Path[] { pathToPythonModule });

        }catch(URISyntaxException e) {
            throw new AttivioException(ConnectorError.CRAWL_FAILED_TO_START, "Can't find sas7bdat python module!");
        }
    }

    @Override
    protected void crawlFile(TreeNode treeNode) throws AttivioException {
        Path pathToFile = Paths.get(treeNode.getAbsolutePath());
        SassyFile file = parseDataFile(pathToFile);

        if(file != null) {
            int i = feedDataFile(file, pathToFile);
            if(i == 0) {
                log.info("failed to get rows from file: %s", pathToFile);
            }
        }
    }

    /**
     * Feed the SAS data file (SassyFile) to Attivio using the feed() method of the
     * scanner class.
     *
     * @param file SassyFile populated with metadata and data
     * @param pathToFile Path object pointing to original file location
     * @return int - number of rows fed
     * @throws AttivioException if problem encountered during feeding
     */
    private int feedDataFile(SassyFile file, Path pathToFile) throws AttivioException {

        int rowNum = 0;
        try {
            List<Row> rows = file.getRows();
            for (Row row : rows) {
                final String docId = String.format("%s:%s_%s",
                        pathToFile.toAbsolutePath().toString(),
                        file.getDataSetName(),
                        rowNum);

                IngestDocument doc = new IngestDocument(docId);
                doc.setField(SasFieldNames.DATASET_NAME,
                        file.getDataSetName());
                doc.setField(SasFieldNames.CREATED_DATE,
                        Date.from(file.getCreatedDate().toInstant(ZoneOffset.UTC)));
                doc.setField(SasFieldNames.MODIFIED_DATE,
                        Date.from(file.getModifiedDate().toInstant(ZoneOffset.UTC)));

                List<Column> columns = file.getColumns();
                for (int i = 0; i < columns.size(); i++) {
                    Column column = columns.get(i);

                    IngestField field = new IngestField(column.getName());
                    field.addValue(String.valueOf(row.get(i)));

                    doc.setField(field);
                }
                rowNum++;
                feed(doc);
            }
        }catch(AttivioException ae) {
            if(ae.getErrorCode() != ConnectorError.CRAWL_STOPPED) {
                // Issue #4 - errors when sampling appear in SDC UI
                throw ae;
            }
        }catch(RuntimeException e) {
            log.debug("runtime error processing SassyFile: %s", e.getMessage());
        }
        return rowNum;
    }

    /**
     * Convert the filesystem file to a SassyFile using the SASFileReader library
     *
     * @param filePath java Path to the file to parse
     * @return new SassyFile
     */
    SassyFile parseDataFile(Path filePath) {
        log.info(String.format("converting file: %s", filePath));

        return SASFileReader.read(filePath);
    }

    @ConfigurationOption(displayName="JDL Lib Property Name")
    public String getJdlPropertyName() {
        return jdlPropertyName;
    }

    public void setJdlPropertyName(String jdlPropertyName) {
        this.jdlPropertyName = jdlPropertyName;
    }

    @ConfigurationOption(displayName="JPY Lib Property Name")
    public String getJpyPropertyName() {
        return jpyPropertyName;
    }

    public void setJpyPropertyName(String jpyPropertyName) {
        this.jpyPropertyName = jpyPropertyName;
    }

    @ConfigurationOption(displayName="Python Lib Property Name")
    public String getPythonLibPropertyName() {
        return pythonLibPropertyName;
    }

    public void setPythonLibPropertyName(String pythonLibPropertyName) {
        this.pythonLibPropertyName = pythonLibPropertyName;
    }

    public String getSas7bdatPropertyName() {
        return sas7bdatPropertyName;
    }

    public void setSas7bdatPropertyName(String sas7bdatPropertyName) {
        this.sas7bdatPropertyName = sas7bdatPropertyName;
    }

    @ConfigurationOption(displayName="Override System Properties", switchValue={"true"})
    public boolean isOverridingSystemProps() {
        return isOverridingSystemProps;
    }

    public void setOverridingSystemProps(boolean overridingSystemProps) {
        isOverridingSystemProps = overridingSystemProps;
    }

    @ConfigurationOption(displayName="Path to a script")
    public String getScriptFile() {
        return scriptFile;
    }

    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    @Override
    public URI getURIToScript() {
        // TODO: for now we assume we're just loading off disk
        try {
            return Paths.get(scriptFile).toUri();
        } catch (NullPointerException npe) {
            //nop
        }
        return null;
    }
}
