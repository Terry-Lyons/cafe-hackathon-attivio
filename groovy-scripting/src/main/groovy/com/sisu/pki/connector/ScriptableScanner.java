package com.sisu.pki.connector;

import com.attivio.sdk.ingest.IngestDocument;

import java.net.URI;
import java.util.Optional;

/**
 * Interface for a Scriptable Scanner so we can have a basic contract since
 * it appears we need at least 2 Groovy ones for now (one based of AbstractFileScanner
 * and the other just implementing Scanner).
 *
 * Created by dave on 10/5/16.
 */
public interface ScriptableScanner {

    /**
     * We need the name of the script class to use when compiling.
     * @return name of the class (ideally canonical)
     */
    String getScriptClassname();

    /**
     * Assumption is we need a way to get a path to the script in order
     * to load and compile it at least once.
     *
     * @return Path to the script file on disk.
     */
    URI getURIToScript();

    /**
     * Execute an evaluation of a script file at the set Path
     *
     * @return result object
     */
    Optional<Object> runScriptOnIngestDocument(IngestDocument doc);

}
