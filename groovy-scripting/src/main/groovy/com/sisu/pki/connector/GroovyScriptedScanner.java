package com.sisu.pki.connector;

import com.attivio.connector.MessagePublisher;
import com.attivio.connector.Scanner;
import com.attivio.sdk.AttivioException;
import com.attivio.sdk.ingest.IngestDocument;
import com.sisu.groovy.GobotScript;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Groovy Wrapper of Attivio's AbstractFileScanner.
 *
 * Created by dave on 10/5/16.
 */
public abstract class GroovyScriptedScanner implements ScriptableScanner, Scanner {

    private final Logger log = LoggerFactory.getLogger(getClass().getName());
    GobotScript script;

    boolean scriptingEnabled = true;

    protected MessagePublisher wrappedMessagePublisher;

    public void feed(IngestDocument... docs) throws AttivioException {
        wrappedMessagePublisher.feed(docs);
    }

    @Override
    public void setMessagePublisher(MessagePublisher messagePublisher) {
        this.wrappedMessagePublisher = new WrappedMessagePublisher(messagePublisher, this);
    }

    public MessagePublisher getMessagePublisher() {
        return this.wrappedMessagePublisher;
    }

    @Override
    public String getScriptClassname() {
        return GobotScript.class.getCanonicalName();
    }

    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    protected void compileScript(URI uriToScript) throws IOException, CompilationFailedException {

        final CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(getScriptClassname());

        final GroovyShell shell = new GroovyShell(getClassLoader(), new Binding(), config);

        log.info(String.format("...compiling %s script from uri: %s", config.getScriptBaseClass(), uriToScript));
        script = (GobotScript) shell.parse(uriToScript);
    }

    @Override
    public Optional<Object> runScriptOnIngestDocument(IngestDocument doc) {

        URI uriToScript = getURIToScript();
        Optional<Object> result = Optional.empty();

        if (scriptingEnabled && uriToScript == null) {
            log.info("No script provided. Passing.");
            scriptingEnabled = false;
            return result;
        }

        try {
            if(scriptingEnabled) {
                if (script == null) {
                    compileScript(uriToScript);
                }
                script.setDoc(doc);
                result = Optional.of(script.run());
            }
        } catch (IOException io) {
            scriptingEnabled = false;
            log.error(String.format("IOException trying to parse Gobot script at uri: %s", uriToScript));

        } catch (CompilationFailedException e) {
            scriptingEnabled = false;
            log.error(String.format("Could not compile from uri %s: %s", uriToScript, e.getMessage()));

        } catch(Exception e) {
            scriptingEnabled = false;
            log.error(String.format("Error calling run() on script from uri %s: %s", uriToScript, e.getMessage()));
        }

        return result;
    }

    /**
     * Flag for whether we try running scripts. Used to quiet log output after
     * first failure.
     *
     * @return boolean
     */
    public boolean isScriptingEnabled() {
        return scriptingEnabled;
    }

    /**
     * Toggle the flag for if we try to execute a script or not
     * @param scriptingEnabled
     */
    public void setScriptingEnabled(boolean scriptingEnabled) {
        this.scriptingEnabled = scriptingEnabled;
    }

    @Override
    protected void finalize() throws Throwable {


        super.finalize();
    }
}
