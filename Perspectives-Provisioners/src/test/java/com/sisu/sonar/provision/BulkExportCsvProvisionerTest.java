package com.sisu.sonar.provision;

import com.attivio.TestUtils;
import com.attivio.sdk.server.sonar.provision.ProvisionModel;
import com.attivio.sdk.server.sonar.provision.ProvisionedColumn;
import com.attivio.sdk.server.sonar.provision.ProvisionedTable;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by dave on 10/19/16.
 */
public class BulkExportCsvProvisionerTest {

    BulkExportCsvProvisioner provisioner;
    ProvisionedTable itemTable;
    ProvisionedTable personTable;
    ProvisionedTable purchaseTable;
    ProvisionModel model;

    static Connection hsqlConnection;
    static Driver driver;

    static ResultSet mockRs = mock(ResultSet.class);

    static {
        TestUtils.initializeEnvironment();
    }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUpDb() throws Exception {
        when(mockRs.getString(0)).thenReturn("1");
        when(mockRs.getString(1)).thenReturn("Macbook Pro");
        when(mockRs.getFloat(2)).thenReturn(1500.0f);

        driver = (Driver) Class.forName("org.hsqldb.jdbcDriver").newInstance();
        String connectionString = "jdbc:hsqldb:file:" + folder.getRoot().getAbsolutePath() + "/testdb;shutdown=true";

        Files.copy(Paths.get(this.getClass().getResource("/fixtures/item.csv").toURI()), Paths.get(folder.getRoot().getAbsolutePath(), "item.csv"));
        Files.copy(Paths.get(this.getClass().getResource("/fixtures/person.csv").toURI()), Paths.get(folder.getRoot().getAbsolutePath(), "person.csv"));
        Files.copy(Paths.get(this.getClass().getResource("/fixtures/purchase.csv").toURI()), Paths.get(folder.getRoot().getAbsolutePath(), "purchase.csv"));


        hsqlConnection = DriverManager.getConnection(connectionString, "SA", "");
        String createItemTable = "CREATE TEXT TABLE item (id INT PRIMARY KEY, item VARCHAR(30), price FLOAT)";
        String setItemTable = "SET TABLE item SOURCE \"./item.csv;ignore_first=true\"";

        String createPersonTable = "CREATE TEXT TABLE person (id INT PRIMARY KEY, name VARCHAR(30), age FLOAT)";
        String setPersonTable = "SET TABLE person SOURCE \"./person.csv;ignore_first=true\"";

        String createPurchaseTable = "CREATE TEXT TABLE purchase (id INT PRIMARY KEY, person_id INT, item_id INT)";
        String setPurchaseTable = "SET TABLE purchase SOURCE \"./purchase.csv;ignore_first=true\"";


        Statement stmt = hsqlConnection.createStatement();
        try {
            stmt.execute(createItemTable);
            stmt.execute(setItemTable);
            stmt.execute(createPersonTable);
            stmt.execute(setPersonTable);
            stmt.execute(createPurchaseTable);
            stmt.execute(setPurchaseTable);

        }catch(SQLException e) {
            System.err.println("sql errors on setup: " + e.getCause());
        }


        stmt.close();

    }

    @After
    public void tearDown() throws Exception {
        try {
            Statement stmt = hsqlConnection.createStatement();
            stmt.execute("TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK");
            hsqlConnection.commit();
            stmt.execute("SHUTDOWN");
            stmt.close();
            hsqlConnection.close();
        }catch(SQLException e) {
            System.err.println("sql errors on teardown");
        }
    }

    @Before
    public void setUp() {
        provisioner = new BulkExportCsvProvisioner();

        itemTable = new ProvisionedTable();
        itemTable.setName("item");
        ArrayList<ProvisionedColumn> columns = new ArrayList<>();
        ProvisionedColumn idCol = new ProvisionedColumn();
        idCol.setName("id");
        idCol.setSqlTypeNum(Types.VARCHAR);

        ProvisionedColumn itemCol = new ProvisionedColumn();
        itemCol.setName("item");
        itemCol.setSqlTypeNum(Types.VARCHAR);

        ProvisionedColumn priceCol = new ProvisionedColumn();
        priceCol.setName("price");
        priceCol.setSqlTypeNum(Types.FLOAT);

        itemTable.setColumns(Arrays.asList(new ProvisionedColumn[] {idCol, itemCol, priceCol }));

        personTable = new ProvisionedTable();
        personTable.setName("person");
        ArrayList<ProvisionedColumn> personColumns = new ArrayList<>();
        personColumns.add(idCol);

        ProvisionedColumn nameColumn = new ProvisionedColumn();
        nameColumn.setName("name");
        nameColumn.setSqlTypeNum(Types.VARCHAR);
        personColumns.add(nameColumn);

        ProvisionedColumn ageColumn = new ProvisionedColumn();
        ageColumn.setName("age");
        ageColumn.setSqlTypeNum(Types.DOUBLE);
        personColumns.add(ageColumn);

        personTable.setColumns(personColumns);

        purchaseTable = new ProvisionedTable();
        purchaseTable.setName("purchase");
        ArrayList<ProvisionedColumn> purchaseColumns = new ArrayList<>();
        purchaseColumns.add(idCol);

        ProvisionedColumn personIdCol = new ProvisionedColumn();
        personIdCol.setSqlTypeNum(Types.INTEGER);
        personIdCol.setName("person_id");
        purchaseColumns.add(personIdCol);

        ProvisionedColumn itemIdCol = new ProvisionedColumn();
        itemIdCol.setSqlTypeNum(Types.INTEGER);
        itemIdCol.setName("item_id");

        model = new ProvisionModel();
        model.setTables(Arrays.asList(new ProvisionedTable[] { personTable, itemTable, purchaseTable }));
        model.setDisplayName("myDataMart");

    }

    @Test
    public void testProvisionerGeneratesZipFileOfMultipleCsv() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 1024 * 5);

        HashMap<String, String> params = new HashMap<>();
        params.put(provisioner.MAX_ROWS_PARAM, "100");

        BulkExportCsvProvisioner provisioner = new BulkExportCsvProvisioner();
        provisioner.provision(null, () -> hsqlConnection, model, params, bos);

        assertThat(bos.size()).isGreaterThan(0);

        byte[] bytes = bos.toByteArray();
        Path tempFile = Files.createTempFile("attivip-provision", "temp.zip");
        Files.write(tempFile, bytes, StandardOpenOption.CREATE);

        ZipFile zip = new ZipFile(tempFile.toFile());

        assertThat(zip.getEntry("person.csv")).isNotNull();
        assertThat(zip.getEntry("purchase.csv")).isNotNull();
        assertThat(zip.getEntry("item.csv")).isNotNull();

    }
}
