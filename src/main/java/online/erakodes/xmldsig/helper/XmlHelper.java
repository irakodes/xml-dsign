package online.erakodes.xmldsig.helper;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class XmlHelper {

    /**
     * Parses an XML string into a DOM Document.
     *
     * @param xml the XML content as a string
     * @return parsed Document object
     * @throws Exception if parsing fails
     */
    public static Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var builder = factory.newDocumentBuilder();

        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
