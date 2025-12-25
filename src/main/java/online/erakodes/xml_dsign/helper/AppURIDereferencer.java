package online.erakodes.xml_dsign.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class AppURIDereferencer implements URIDereferencer {
    private final Document doc;
    InputStream refInputStream;
    private final Logger log = LoggerFactory.getLogger(AppURIDereferencer.class);

    public AppURIDereferencer(Document doc) {
        this.doc = doc;
    }

    @Override
    public Data dereference(URIReference uriReference, XMLCryptoContext context) {
        if (uriReference.getURI() != null && uriReference.getURI().isEmpty()) {
            try {
                final var apNodes = doc.getElementsByTagName("AppHdr");
                final var apNode = apNodes.item(0);
                var parentWithoutChildren = apNode.cloneNode(false);
                refOutputStream(parentWithoutChildren);
            } catch (TransformerException ex) {
                log.error("Error while dereferencing URI: {}", uriReference.getURI(), ex);
            }

            return new OctetStreamData(refInputStream);
        }
        if (uriReference.getURI() != null) {

            var snodeList = doc.getElementsByTagName("ds:Modulus");
            setRightContent(snodeList);
            var scnodeList = doc.getElementsByTagName("ds:X509Certificate");
            setRightContent(scnodeList);

            try {
                final var keyNodes = doc.getElementsByTagName("ds:KeyInfo");
                final var kNode = keyNodes.item(0);

                refOutputStream(kNode);
            } catch (TransformerException ex) {
                log.error("Error while dereferencing URI: {}", uriReference.getURI(), ex);
            }

            return new OctetStreamData(refInputStream);
        }

        try {
            final var docNodes = doc.getElementsByTagName("Document");
            final var docNode = docNodes.item(0);
            refOutputStream(docNode);
        } catch (TransformerException ex) {
            log.error("Error while dereferencing URI: {}", uriReference.getURI(), ex);
        }

        return new OctetStreamData(refInputStream);
    }

    private void refOutputStream(Node parentWithoutChildren) throws TransformerException {
        final var refOutputStream = new ByteArrayOutputStream();
        final Transformer xform;
        xform = TransformerFactory.newInstance().newTransformer();
        xform.setOutputProperty("omit-xml-declaration", "yes");

        xform.transform(new DOMSource(parentWithoutChildren), new StreamResult(refOutputStream));
        refInputStream = new ByteArrayInputStream(refOutputStream.toByteArray());
    }

    public static void setRightContent(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            var node = nodeList.item(i);
            node.normalize();
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                var element = (Element) node;
                var nodeContent = element.getTextContent();
                nodeContent = nodeContent.replaceAll("\r", "");
                node.setTextContent(nodeContent.trim());
            }
        }
    }
}