package com.sisu.pki.connector;

import com.attivio.connector.AbstractFileScanner;
import com.attivio.connector.MessagePublisher;
import com.attivio.sdk.AttivioException;

import java.net.URI;

/**
 * Wrapper around the AbstractFileScanner, but hijacking the message publisher with a GroovyScriptedScanner.
 *
 * This is, quite frankly, a little nasty..mostly because of the dueling paths of Attivio's SDK providing
 * the Scanner interface along with abstract classes like AbstractFileScanner.
 *
 * Created by dave on 11/10/16.
 */
public abstract class GroovyScriptedAbstractFileScanner extends AbstractFileScanner {

    private GroovyScriptedScanner groovyScanner = new GroovyScriptedScanner() {

        private GroovyScriptedAbstractFileScanner parent;

        @Override
        public void start() throws AttivioException {
            parent.start();
        }

        @Override
        public URI getURIToScript() {
            return parent.getURIToScript();
        }

        private GroovyScriptedScanner init(GroovyScriptedAbstractFileScanner parent) {
            this.parent = parent;
            return this;
        }
    }.init(this);

    public abstract URI getURIToScript();

    @Override
    public void setMessagePublisher(MessagePublisher mp) {
        // Here we're trying to bridge between our embedded GroovyScriptedScanner and the AbstractFileScanner.
        groovyScanner.setMessagePublisher(mp);
        super.setMessagePublisher(groovyScanner.getMessagePublisher());
    }

    @Override
    public MessagePublisher getMessagePublisher() {
        return groovyScanner.getMessagePublisher();
    }

}
