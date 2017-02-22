package com.sisu.ncbi;

import com.attivio.util.XMLUtils.NonValidatingEntityResolver;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.EmptyStackException;

/**
 * XML handling routines for the Entrez system using Dom4J and SAX
 *
 * @author dvoutila
 */
@SuppressWarnings("restriction")
public class EntrezXMLUtils {

    private static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

    private final static Logger log = LoggerFactory.getLogger(EntrezXMLUtils.class.getSimpleName());

    /**
     * Uses simple SAXReader to parse the xml String and return a Document
     *
     * @param xml String containing valid XML
     * @return Documnent
     * @throws DocumentException
     */
    public static Document parse(String xml) throws DocumentException {
        SAXReader reader = new SAXReader(false);

        return reader.read(new StringReader(xml));
    }


    /**
     * Process the a stream of (ideally) XML input from Entrez, properly parsing using SAX
     *
     * @param stream  InputStream containing XML to parse
     * @param handler DefaultHandler to use for the XMLReader's Content Handler
     */
    public static void processResponseStream(InputStream stream, DefaultHandler handler) {

        try {

            final SAXParser parser = parserFactory.newSAXParser();
            final XMLReader reader = parser.getXMLReader();

            reader.setEntityResolver(NonValidatingEntityResolver.getInstance());
            reader.setContentHandler(handler);
            reader.parse(new InputSource(stream));

        } catch (SAXException | IOException | ParserConfigurationException | EmptyStackException e) {

            // TODO Why are we getting an EmptyStackException?!?!
            final String error = String.format("Exception (%s) caught while wrangling InputStream %s",
                    e.getClass().getName(), stream.toString());
            log.error(error, e);
        }

    }

}
