package online.erakodes.xml_dsign.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(XmlConverter.class);

    /**
     * Converts an XML Document to its string representation.
     * This method transforms the given XML document into a string,
     * ensuring the document's structure and content are preserved.
     * <p>
     * If the conversion process encounters a TransformerException,
     * an error is logged, and a RuntimeException is thrown.
     *
     * @param document the XML Document to be converted; must not be null.
     * @return the string representation of the provided XML Document.
     * @throws RuntimeException if any error occurs during the transformation process.
     */
    public static String convert(Document document) {
        log.debug("Converting XML Document [[{}] bytes]", document.toString()
                .getBytes().length);

        var tf = TransformerFactory.newInstance();

        try {
            var transformer = tf.newTransformer();

            var source = new DOMSource(document);
            var writer = new StringWriter();
            var result = new StreamResult(writer);

            transformer.transform(source, result);

            return writer.toString();
        } catch (TransformerException e) {
            log.error("Error converting XML Document: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static Document convert(String xmlString) {
        log.debug("Converting XML String [[{}] bytes]", xmlString.getBytes().length);
        var factory = DocumentBuilderFactory.newInstance();

        try {
            var builder = factory.newDocumentBuilder();
            var doc = builder.parse(new InputSource(new StringReader(xmlString)));

            if (doc != null) {
                log.info("Document: {}", doc.getDocumentElement().getTextContent());
                log.info("Node list: {}", doc.getElementsByTagName("AppHdr").getLength());
            }

            return doc;
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.error("An error occurred while converting the document: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}