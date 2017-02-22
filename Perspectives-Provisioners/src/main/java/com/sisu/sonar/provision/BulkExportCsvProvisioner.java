package com.sisu.sonar.provision;

import com.attivio.sdk.security.AttivioPrincipal;
import com.attivio.sdk.server.sonar.provision.*;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


/**
 * Exports an entire Data Mart as individual CSV files...one per table.
 * <p>
 * Created by dave on 10/19/16.
 */
public class BulkExportCsvProvisioner implements Provisioner {

    static final long MAX_ROWS = 100_000;
    static final String MAX_ROWS_PARAM = "Max Rows per Table";
    static final Logger log = LoggerFactory.getLogger(BulkExportCsvProvisioner.class);

    @Override
    public String getMenuLabel() {
        return "Download Mart as Zip of CSV";
    }

    @Override
    public boolean producesDownloadFile() {
        return true;
    }

    @Override
    public boolean allowsConcurrentExecutions() {
        return false;
    }

    @Override
    public boolean requiresDefaultTable() {
        return false;
    }

    @Override
    public String getDownloadFilename(String martDisplayName, String martCanonicalName) {
        return String.format("%s.zip", martCanonicalName);
    }

    @Override
    public List<ProvisionerParameter> getUserParameters(AttivioPrincipal attivioPrincipal) {
        ProvisionerParameter param = new ProvisionerParameter(MAX_ROWS_PARAM,
                "Limits the retrieval of data by appending a 'LIMIT <N>' clause to the SELECT used to pull table data.");
        param.setDefaultValue(String.valueOf(MAX_ROWS));

        return Arrays.asList(new ProvisionerParameter[]{param});
    }

    public ResultSet executeQuery(ProvisionedTable table, Connection connection, long maxRows) throws SQLException {
        String sql = String.format("SELECT * FROM %s LIMIT %s", table.getName(), maxRows);
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    public void writeTableToZip(ResultSet rs, ProvisionedTable table, FileSystem zipFileSystem) {

        try {
            Path tempFilePath = Files.createTempFile("attivio-provisioner", table.getName());
            CSVWriter writer = new CSVWriter(Files.newBufferedWriter(tempFilePath, Charset.forName("utf-8"), StandardOpenOption.CREATE));
            writer.writeAll(rs, true);
            writer.flush();
            writer.close();

            Files.copy(
                    tempFilePath,
                    zipFileSystem.getPath(table.getName() + ".csv"),
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException io) {
            log.error("IOException creating CSV from table " + table.getName(), io.getCause());
            io.printStackTrace();

        } catch (SQLException sql) {
            log.error("SQLException processing result set for table " + table.getName(), sql.getCause());
            sql.printStackTrace();
        }

    }

    @Override
    public String provision(AttivioPrincipal attivioPrincipal, ProvisionConnectionFactory provisionConnectionFactory, ProvisionModel provisionModel, Map<String, String> userParameters, OutputStream outputStream) throws ProvisionException {

        final long maxRows;
        if (!userParameters.getOrDefault(MAX_ROWS_PARAM, "").isEmpty()) {
            maxRows = Long.getLong(userParameters.get(MAX_ROWS_PARAM), MAX_ROWS);
        } else {
            maxRows = MAX_ROWS;
        }

        log.info(String.format("Attempting to generate Zip of mart %s", provisionModel.getDisplayName()));

        try {
            final Connection conn = provisionConnectionFactory.connect();
            final Path tempDir = Files.createTempDirectory("attivio-provision-zip");
            final Path pathToZip = Paths.get(tempDir.toString(), provisionModel.getDisplayName() + ".zip");

            final URI uriToZip = URI.create("jar:" + pathToZip.toUri().toString() + "!/");

            Map<String, String> env = new HashMap<>();
            env.put("create", "true");

            FileSystem zipFileSystem = FileSystems.newFileSystem(uriToZip, env);

            provisionModel.getTables().stream().forEach(table -> {
                try {
                    ResultSet rs = executeQuery(table, conn, maxRows);
                    writeTableToZip(rs, table, zipFileSystem);

                } catch (SQLException sql) {
                    log.error("SQLException provisioning table " + table.getName(), sql.getCause());
                }
            });
            zipFileSystem.close();

            FileInputStream in = new FileInputStream(pathToZip.toFile());
            byte[] buffer = new byte[64 * 1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            in.close();
            outputStream.flush();

        } catch (IOException io) {
            log.error("IOException provisioning model " + provisionModel.getDisplayName(), io.getCause());
            io.printStackTrace();

        } finally {
            try {
                outputStream.close();
            } catch (IOException io) {
                log.error("couldn't close outputStream", io.getCause());
                io.printStackTrace();
            }
        }

        return null;
    }
}
