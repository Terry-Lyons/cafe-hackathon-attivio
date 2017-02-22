package com.sisu.sonar.provision;

import au.com.bytecode.opencsv.CSVReader;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by dave on 10/19/16.
 */
public class CsvFiles {

    static Path pathToItemTable;
    static Path pathToPurchaseTable;
    static Path pathToPersonTable;

    static {
        try {
            pathToItemTable = Paths.get(CsvFiles.class.getResource("/fixtures/item.csv").toURI());
            pathToPersonTable = Paths.get(CsvFiles.class.getResource("/fixtures/person.csv").toURI());
            pathToPurchaseTable = Paths.get(CsvFiles.class.getResource("/fixtures/purchase.csv").toURI());

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    static CSVReader openCsvReader(Path path) {
        try {
            return new CSVReader(Files.newBufferedReader(path));
        }catch(Exception e) {
            System.err.println("Couldn't open " + path.toString());
            return null;
        }
    }

    static CSVReader getItemTable() {
        return openCsvReader(pathToItemTable);
    }

    static CSVReader getPersonTable() {
        return openCsvReader(pathToPersonTable);
    }

    static CSVReader getPurchaseTable() {
        return openCsvReader(pathToPurchaseTable);
    }
}
