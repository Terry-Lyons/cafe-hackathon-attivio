package com.sisu.pki.connector;

import com.attivio.app.config.classloader.AddOnLibraryClassFactory;
import com.attivio.connector.Scanner;
import com.attivio.platform.util.ReflectionUtils;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.error.ConnectorError;
import com.attivio.sdk.server.annotation.ConfigurationOption;
import com.attivio.sdk.server.annotation.ConfigurationOptionInfo;
import com.attivio.sdk.server.annotation.ScannerInfo;
import com.attivio.util.AttivioLogger;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Groovy Scripting wrapped ExcelScanner
 * Created by dave on 11/16/16.
 */
@ScannerInfo(requiresFieldMapping = true)
@ConfigurationOptionInfo(
        displayName = "SDTM Excel Connector",
        description = "Wraps regular Excel Connector in Scripting Framework for SDTM conversion.",
        groups = {
                @ConfigurationOptionInfo.Group(
                    path = {"Scanner"},
                    propertyNames = {
                        "startDirectory", "wildcardFilter", "wildcardExcludeFilter",
                        "firstRowAreFieldNames", "rowNumberAsId"
                    }),
                @ConfigurationOptionInfo.Group(
                        path = {"Other"},
                        propertyNames = {
                                "strictLoad",
                        }),
                @ConfigurationOptionInfo.Group(
                    path = {ConfigurationOptionInfo.SCANNER, "Data Cleansing"},
                    propertyNames = {"scriptFile"})}
)
public class GroovyScriptedExcelScanner extends GroovyScriptedScanner {

    private String scriptFile;

    // AbstractFileScanner
    private String startDirectory;
    private String[] wildcardFilter = new String[]{"*.xlsx", "*.xls"};
    private String[] excludeWildcardFilter = new String[]{".*", "*~", "~*", "$*", "*.tmp"};

    // ExcelScanner
    private boolean rowNumberAsId = true;
    private boolean firstRowAreFieldNames = true;
    private boolean strictLoad = false;

    private AttivioLogger log = AttivioLogger.getLogger(this);

    @Override
    public void start() throws AttivioException {
        try {
            AddOnLibraryClassFactory factory = new AddOnLibraryClassFactory();
            Class excelScannerClass = factory.forName("com.attivio.connector.ExcelScanner");

            Scanner scanner = (Scanner)ReflectionUtils.newInstance(excelScannerClass);
            scanner.setMessagePublisher(getMessagePublisher());
            ReflectionUtils.getSetter(excelScannerClass, "setStartDirectory", String.class).invoke(scanner, startDirectory);

            ReflectionUtils.getSetter(excelScannerClass, "setStrictLoad", boolean.class).invoke(scanner, strictLoad);
            ReflectionUtils.getSetter(excelScannerClass, "setRowNumberAsId", boolean.class).invoke(scanner, rowNumberAsId);
            ReflectionUtils.getSetter(excelScannerClass, "setFirstRowAreFieldNames", boolean.class).invoke(scanner, firstRowAreFieldNames);


            scanner.start();
        }catch(Exception c) {
            log.error(ConnectorError.CRAWL_FAILED, "exception running ExcelScanner: %s", c.getMessage());
        }
    }

    @Override
    public URI getURIToScript() {
        // TODO: for now we assume we're just loading off disk
        if (scriptFile != null) {
            try {
                return Paths.get(scriptFile).toUri();
            } catch (NullPointerException npe) {
                log.error(ConnectorError.CONFIGURATION_WARNING, "couldn't generate URI to scriptFile: " + scriptFile, npe);
            }
        }
        return null;
    }

    @ConfigurationOption(displayName="Path to a script")
    public String getScriptFile() {
        return scriptFile;
    }

    @ConfigurationOption(
            optionLevel = ConfigurationOption.OptionLevel.Required,
            shortOpt = "d",
            longOpt = "dir",
            displayName = "Start Directory",
            description = "Starting directory for crawl"
    )public String getStartDirectory() {
        return startDirectory;
    }

    public void setStartDirectory(String startDirectory) {
        this.startDirectory = startDirectory;
    }

    @ConfigurationOption(
            optionLevel = ConfigurationOption.OptionLevel.Required,
            displayName = "Path to SDTM Script"
    )
    public void setScriptFile(String scriptFile) {
        this.scriptFile = scriptFile;
    }

    @ConfigurationOption(
            optionLevel = ConfigurationOption.OptionLevel.Basic,
            messageProp = "wildcardFilter",
            displayName = "Wildcard Include Filter",
            formEntryClass = "com.attivio.gwt.model.form.client.entries.SortedStringListEntry",
            description = "Filters for files to crawl (ex *.xml, *.doc); NOTE: be sure to escape wildcard characters according to your system\'s terminal"
    )
    public List<String> getWildcardFilter() {
        return Arrays.asList(this.wildcardFilter);
    }

    public void setWildcardFilter(List<String> wildcardFilter) {
        this.wildcardFilter = (String[])wildcardFilter.toArray(new String[wildcardFilter.size()]);
    }

    @ConfigurationOption(
            displayName = "Wildcard Exclude Filter",
            formEntryClass = "com.attivio.gwt.model.form.client.entries.SortedStringListEntry",
            description = "Filters for files to exclude in crawl (ex *.tmp)"
    )
    public List<String> getWildcardExcludeFilter() {
        return Arrays.asList(this.excludeWildcardFilter);
    }

    public void setWildcardExcludeFilter(List<String> wildcardExcludeFilter) {
        this.excludeWildcardFilter = (String[])wildcardExcludeFilter.toArray(new String[wildcardExcludeFilter.size()]);
    }

    @ConfigurationOption(
            displayName = "Row Number as ID",
            optionLevel = ConfigurationOption.OptionLevel.Basic)
    public boolean isRowNumberAsId() {
        return rowNumberAsId;
    }

    public void setRowNumberAsId(boolean rowNumberAsId) {
        this.rowNumberAsId = rowNumberAsId;
    }

    @ConfigurationOption(
            displayName = "First Row are Field Names",
            optionLevel = ConfigurationOption.OptionLevel.Basic)
    public boolean isFirstRowAreFieldNames() {
        return firstRowAreFieldNames;
    }

    public void setFirstRowAreFieldNames(boolean firstRowAreFieldNames) {
        this.firstRowAreFieldNames = firstRowAreFieldNames;
    }

    @ConfigurationOption(
            displayName = "Strict Load",
            optionLevel = ConfigurationOption.OptionLevel.Advanced)
    public boolean isStrictLoad() {
        return strictLoad;
    }

    public void setStrictLoad(boolean strictLoad) {
        this.strictLoad = strictLoad;
    }
}
