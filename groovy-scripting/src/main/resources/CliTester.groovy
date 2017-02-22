import com.attivio.sdk.AttivioException
import com.sisu.pki.connector.GroovyScriptedScanner
@Grab('org.apache.commons:commons-csv:1.2')
import org.apache.commons.csv.CSVParser

import java.nio.file.Path
import java.nio.file.Paths
import com.attivio.sdk.ingest.IngestDocument
import com.attivio.sdk.ingest.IngestField

import static org.apache.commons.csv.CSVFormat.*

def cli = new CliBuilder(usage: 'groovy CliTester [options]')

cli.d(longOpt: 'data', args: 1, 'path to csv test data')
cli.s(longOpt: 'script', args: 1, 'path to gobot script')
cli.h(longOpt: 'help', 'display usage help')

// parse and process parameters
def options = cli.parse(args)
if (options.h || !(options.d && options.s)) {
    cli.usage()
    return
}

Path dataPath = Paths.get(options.d)
Path scriptPath = Paths.get(options.s)

class UtilityScanner extends GroovyScriptedScanner {

    URI uri

    @Override
    void start() throws AttivioException {
        //nop
    }

    UtilityScanner(URI uri) {
        this.uri = uri
    }

    @Override
    URI getURIToScript() {
        return uri
    }
}
GroovyScriptedScanner scanner = new UtilityScanner(scriptPath.toUri())

dataPath.toFile().withReader { reader ->
    CSVParser csv = new CSVParser(reader, DEFAULT.withHeader())

    csv.eachWithIndex { row, index ->
        IngestDocument doc = new IngestDocument("row-${index}")
        csv.headerMap.keySet().each { fieldName ->
            IngestField field = new IngestField(fieldName)
            field.addValue(row.get(fieldName))
            doc.setField(field)
        }
        Optional result = scanner.runScriptOnIngestDocument(doc)
        if(result.isPresent()) {
            def value = result.get()
            if (value instanceof List) {
                value.each { println it }
            } else {
                println value
            }
        }
    }
}