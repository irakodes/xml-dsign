package online.erakodes.xmldsig.helper;

import online.erakodes.xmldsig.web.SignedMxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to extract digest information from signed XML documents.
 */
public class DigestExtractor {

    private static final Logger log = LoggerFactory.getLogger(DigestExtractor.class);
    private static final String DIGEST_VALUE_TAG = "DigestValue";
    private static final String REFERENCE_TAG = "Reference";
    private static final String DIGEST_METHOD_TAG = "DigestMethod";


    /**
     * Extracts digest information from a signed XML document.
     * The method parses the XML Signature element and retrieves all Reference elements
     * along with their digest values and metadata.
     *
     * @param signedXml the signed XML document as a string
     * @return a list of DigestInfo objects containing entity names and digest values
     */
    public static List<SignedMxMessage.DigestInfo> extractDigests(String signedXml) {
        var digests = new ArrayList<SignedMxMessage.DigestInfo>();

        try {
            var doc = XmlHelper.parseXml(signedXml);
            var referenceNodes = doc
                    .getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#",
                            REFERENCE_TAG);

            if (referenceNodes.getLength() == 0) {
                log.warn("No Reference elements found in signed XML");
                return digests;
            }

            log.debug("Found {} Reference elements in signed XML",
                    referenceNodes.getLength());

            for (var i = 0; i < referenceNodes.getLength(); i++) {
                var element = (Element) referenceNodes.item(i);
                var digestInfo = extractDigestFromReference(element, i);

                if (digestInfo != null) digests.add(digestInfo);
            }

            log.info("Successfully extracted {} digests from signed XML", digests.size());
        } catch (Exception e) {
            log.error("Failed to extract digests from signed XML due to an error: {}",
                    e.getMessage(), e);
        }

        return digests;
    }

    /**
     * Extracts digest information from a single Reference element.
     *
     * @param element the Reference DOM element
     * @param i       the index of this reference (for naming purposes)
     * @return DigestInfo object or null if extraction fails
     */
    private static SignedMxMessage.DigestInfo extractDigestFromReference(Element element, int i) {
        try {
            var uri = element.getAttribute("URI");

            var entityName = determineEntityName(uri, i);

            var digestValueNodes = element
                    .getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#",
                            DIGEST_VALUE_TAG);

            if (digestValueNodes.getLength() == 0) {
                log.warn("No DigestValue found for reference with URI: {}",
                        uri);
                return null;
            }

            var digestValue = digestValueNodes.item(0).getTextContent().trim();

            var digestMethodNodes = element
                    .getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#",
                            DIGEST_METHOD_TAG);

            // var algorithm = digestMethodNodes.item(0).getAttribute("Algorithm");
            var algorithm = "SHA-256";

            if (digestMethodNodes.getLength() > 0) {
                var digestMethodElement = (Element) digestMethodNodes.item(0);
                var algorithmUri = digestMethodElement.getAttribute("Algorithm");
                algorithm = extractAlgorithName(algorithmUri);
            }

            log.debug("Extracted digest for {}: {} (algorithm: {})", entityName, digestValue, algorithm);

            return SignedMxMessage.DigestInfo.builder()
                    .entityName(entityName)
                    .digestValue(digestValue)
                    .algorithm(algorithm)
                    .referenceUri(uri.isEmpty() ? null : uri)
                    .build();
        } catch (Exception e) {
            log.error("Failed to extract digest from reference element", e);
            return null;
        }
    }

    /**
     * Extracts a human-readable algorithm name from the URI.
     *
     * @param algorithmUri the algorithm URI
     * @return simplified algorithm name
     */
    private static String extractAlgorithName(String algorithmUri) {
        if (algorithmUri.contains("sha256") || algorithmUri.contains("SHA256")) return "SHA-256";
        if (algorithmUri.contains("sha512") || algorithmUri.contains("SHA512")) return "SHA-512";
        if (algorithmUri.contains("sha1") || algorithmUri.contains("SHA1")) return "SHA-1";

        return algorithmUri;
    }

    /**
     * Determines the entity name based on the Reference URI.
     * Based on the XmlSigner implementation:
     * - URI starting with "#" and containing UUID -> KeyInfo
     * - Empty URI -> Document/Envelope
     * - null URI -> AppHdr or similar
     *
     * @param uri the URI attribute from the Reference element
     * @param i   the index for fallback naming
     * @return a descriptive entity name
     */
    private static String determineEntityName(String uri, int i) {
        log.info("URI: {} | Index: {}", uri, i);
        if (uri == null || uri.isEmpty()) {
            if (i == 1) return "Document";
            else if (i == 2) return "AppHdr";
            else return "Reference_" + i;
        }

        if (uri.startsWith("#")) {
            var id = uri.substring(1);
            if (id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"))
                return "KeyInfo";

            return "Reference_" + id;
        }
        // if (uri.startsWith("cid:")) return "Attachment_" + uri.substring(4);
        return "Reference_" + i + "_URI";
    }
}