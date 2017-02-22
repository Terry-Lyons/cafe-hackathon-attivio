package com.sisu.pki.connector

import org.junit.Before
import org.junit.Test

import java.nio.file.Path
import java.nio.file.Paths

import static org.assertj.core.api.Assertions.*;


/**
 * Created by dave on 10/11/16.
 */
class GroovyCommandLineTest {

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Path pathToLabData = getResource("/data/lab.csv")
    Path pathToDemoData = getResource("/data/dm.csv")
    File scriptFile = getResource("/CliTester.groovy").toFile()
    GroovyShell shell = new GroovyShell()
    def oldSystemOut = System.out

    def getResource(String name) {
        return Paths.get(this.class.getResource(name).toURI())
    }

    @Before
    void setUp() {
        // Hijack stdout
        System.out = new PrintStream(bos, true)
    }

    String run(def args) {
        String[] array = new String[0]

        if(args) {
            if(args instanceof List) {
                array = args.toArray(array)
            }else {
                array = [args].toArray(array)
            }
        }
        shell.run(scriptFile, array)
        return bos.toString("UTF-8")
    }

    @Test
    void canDisplayHelp() {
        String out = run('-h')
        assertThat(out).contains("usage: groovy CliTester [options]", "-d,--data", "-s,--script")

        out = run()
        assertThat(out).contains("usage: groovy CliTester [options]", "-d,--data", "-s,--script")

    }

    @Test
    void canDisplayDataUsingStubbedScript() {
        Path pathToGobot = getResource("/passthrough.groovy")
        def args = ['-s', pathToGobot.toAbsolutePath().toString(), '-d', pathToLabData.toAbsolutePath().toString()]
        String out = run(args)

        oldSystemOut.println(out)
        assertThat(out).doesNotContain("usage:")
        assertThat(out).contains("VISITID", "RS_RETIC", "PL_RETU_DEC", "SP_RETUO", "DT_PREG")
    }

    @Test
    void canTransformFakeDemographicsData() {
        Path pathToGobot = getResource("/dm.groovy")
        def args = ['-s', pathToGobot.toAbsolutePath().toString(), '-d', pathToDemoData.toAbsolutePath().toString()]
        String out = run(args)

        oldSystemOut.println(out)
        assertThat(out).doesNotContain("usage:")
        assertThat(out).contains("SITEID", "RACE", "Sex", "Country", "AGE", "USUBJID", "DOMAIN")
    }

    @Test
    void canTransformLabData() {
        Path pathToGobot = getResource("/lab.groovy")
        def args = ['-s', pathToGobot.toAbsolutePath().toString(), '-d', pathToLabData.toAbsolutePath().toString()]
        String out = run(args)

        oldSystemOut.println(out)
        assertThat(out).doesNotContain("usage:")
        List rows = Arrays.asList(out.split("\n"))
        assertThat(rows).hasSize(4)
        rows.each { String line ->
            assertThat(line).contains('"LBCAT":"Hematology"', '"DOMAIN":"LBH"', 'LBTEST', 'LBTESTCD')
            assertThat(line).doesNotContain('10³/µL')
            assertThat(line).doesNotContain('K/UL')
            assertThat(line).doesNotContain('Other')
        }
    }
}
