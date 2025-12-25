package online.erakodes.xml_dsign.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import java.security.PublicKey;
import java.util.*;

import static online.erakodes.xml_dsign.util.KeyHandler.getCertificate;
import static online.erakodes.xml_dsign.util.KeyHandler.getPrivateKey;
import static online.erakodes.xml_dsign.util.XmlConverter.convert;

public class XmlSigner {
    private static final Logger log = LoggerFactory.getLogger(XmlSigner.class);

    public static String sign(final String xmlStr, final String keyPass) throws Exception {
        final var doc = convert(xmlStr);
        if (doc == null) {
            throw new Exception("document to be signed is null");
        }
        final var privateKey = getPrivateKey(keyPass);
        final var certificate = getCertificate(keyPass);
        //String output;
        final XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        PublicKey publicKey = certificate.getPublicKey();

        Node appHdr = null;
        final NodeList signatureList = doc.getElementsByTagName("AppHdr");
        if (signatureList.getLength() != 0) {
            log.info("AppHdr found in the document to be signed");
            appHdr = signatureList.item(0);
        }
        if (appHdr == null) {
            throw new Exception("mandatory element AppHdr is missing in the document to be signed");
        }

        final DOMSignContext dsc = new DOMSignContext(privateKey, appHdr);
        dsc.putNamespacePrefix("http://www.w3.org/2000/09/xmldsig#", "ds");
        dsc.setURIDereferencer(new NoUriDereferencer(doc));

        final KeyInfoFactory kif = fac.getKeyInfoFactory();
        KeyValue keyValue = kif.newKeyValue(publicKey);

        var x509Content = new ArrayList<>();
        x509Content.add(certificate);
        X509Data x509data = kif.newX509Data(x509Content);

        // final var keyInfoId = "a7dbf03b-0c6c-44ea-8629-20a332001aa7";
        final var keyInfoId = UUID.randomUUID().toString();

        final KeyInfo ki = kif.newKeyInfo(Arrays.asList(keyValue, x509data), keyInfoId);

        final List<Reference> refs = new ArrayList<>();

        final Reference ref1 = fac
            .newReference("#" + keyInfoId, fac
                .newDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256", null),
                Collections
                .singletonList(fac
                .newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#",
                            (XMLStructure) null)), null, null);
        refs.add(ref1);
        var transformList = new ArrayList<Transform>();


        transformList.add(fac
        .newTransform(Transform.ENVELOPED,(TransformParameterSpec) null));
        transformList.add(fac
        .newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));

        final var ref2 = fac
            .newReference("", fac
                .newDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256", null),
                transformList, null, null);
        refs.add(ref2);

        final var ref3 = fac
            .newReference(null, fac
                .newDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256", null),
                Collections.singletonList(fac
                        .newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#",
                            (XMLStructure) null)), null, null);
        refs.add(ref3);
        final SignedInfo si = fac
            .newSignedInfo(fac
                .newCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#",
                    (XMLStructure) null), fac
                .newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
                refs);

        final var signature = fac.newXMLSignature(si, ki, null, null, null);

        try {
            signature.sign(dsc);
        } catch (Exception e) {
            log.error("Error while signing the document: {}", e.getMessage());
            throw new Exception("Error while handling the signing of the document: "
                    + e.getMessage(), e);
        }
        NodeList snodeList = doc.getElementsByTagName("ds:SignatureValue");
        setRightContent(snodeList);

        return convert(doc);
    }
}