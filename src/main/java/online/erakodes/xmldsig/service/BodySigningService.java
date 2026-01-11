package online.erakodes.xmldsig.service;

import com.prowidesoftware.swift.model.mx.AbstractMX;
import com.prowidesoftware.swift.model.mx.BusinessAppHdrV04;
import lombok.RequiredArgsConstructor;
import online.erakodes.xmldsig.exception.SignatureBuilderException;
import online.erakodes.xmldsig.exception.SigningException;
import online.erakodes.xmldsig.helper.DigestExtractor;
import online.erakodes.xmldsig.helper.XmlSigner;
import online.erakodes.xmldsig.util.KeyHandler;
import online.erakodes.xmldsig.web.SignedMxMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BodySigningService {

    private static final String SIGNATURE_ALGORITHM = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    @Value("${xmldsig.properties.security.keys.pass}")
    private String KEY_PASS;

    private final static Logger log = LoggerFactory.getLogger(BodySigningService.class);

    private final XmlFileLogger fileLogger;

    /**
     * Signs an MX message and returns a complete SignedMxMessage with signature metadata
     * <p>
     * If any error occurs during the signing process, a runtime exception is thrown with the
     * appropriate error details logged.
     *
     * @param message The `SignedMxMessage` to be wrapped and signed. Must not be null.
     * @return A new `SignedMxMessage` containing the signed content and associated metadata.
     * @throws IllegalArgumentException if the provided message is null.
     * @throws RuntimeException         if there is an error during the signing process.
     */
    public SignedMxMessage wrapAndSign(SignedMxMessage message) {
        if (message == null) throw new IllegalArgumentException("Message cannot be null");

        var unsignedMxMessage = StringUtils.normalizeSpace(message.getSignedContent());

        try {
            log.info("Signing MX Message");

            var parsedMx = AbstractMX.parse(unsignedMxMessage);

            var signedMxMessage = XmlSigner.sign(unsignedMxMessage, KEY_PASS);
            log.info("MX Message signed successfully: {}", parsedMx.getMxId().id());

            var digests = DigestExtractor.extractDigests(signedMxMessage);
            log.debug("Extracted {} digests from signed XML", digests.size());

            var signatureInfo = buildSignatureInfo(digests);

            var messageId = extractMessageId(parsedMx);

            var contentPath = fileLogger.logSignedXml(messageId, signedMxMessage);

            return SignedMxMessage.builder()
                    .messageId(messageId)
                    .creationTime(Instant.now())
                    .sender(extractSender(parsedMx))
                    .receiver(extractReceiver(parsedMx))
                    .messageType(extractMessageType(parsedMx))
                    .signedContent(contentPath.toString())
                    .signatureInfo(signatureInfo)
                    .status(SignedMxMessage.MessageStatus.SIGNED)
                    .build();
        } catch (Exception e) {
            log.error("An error occurred while signing the MX Message: {}", e.getMessage());
            throw new SigningException(e);
        }
    }

    /**
     * Builds and returns a {@link SignedMxMessage.SignatureInfo} object containing details about the
     * signature algorithm, signing time, certificate thumbprint, digest information, and validity status.
     *
     * @param digests the list of digest information extracted from the signed XML
     * @return a {@link SignedMxMessage.SignatureInfo} object with complete signature metadata
     * @throws RuntimeException if a {@link NoSuchAlgorithmException} or {@link CertificateEncodingException}
     *                          occurs during certificate thumbprint computation.
     */
    private SignedMxMessage.SignatureInfo buildSignatureInfo(List<SignedMxMessage.DigestInfo> digests) {
        try {
            var instant = Instant.now();
            var thumbprint = computeCertificateThumbprint();

            return SignedMxMessage.SignatureInfo.builder()
                    .algorithm(SIGNATURE_ALGORITHM)
                    .signedAt(instant)
                    .signerCertificateThumbprint(thumbprint)
                    .signatureValid(true)
                    .digests(digests)
                    .build();
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            log.error("Failed to compute certificate thumbprint", e);
            throw new SignatureBuilderException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        var hexString = new StringBuilder();

        for (byte b : bytes) {
            var hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString().toLowerCase(Locale.ROOT);
    }

    private String computeCertificateThumbprint() throws CertificateEncodingException, NoSuchAlgorithmException {
        var certificate = KeyHandler.getCertificate();
        var encodedCert = certificate.getEncoded();

        var digest = MessageDigest.getInstance("SHA-256");
        var hash = digest.digest(encodedCert);

        var hexValue = bytesToHex(hash);

        //return String.format("%064x", new java.math.BigInteger(1, hash));
        log.debug("[%064x]BigInteger(1, hash): {}", String.format("%064x", new java.math.BigInteger(1, hash)));
        log.debug("SHA256:bytesToHex(hash): {}", "SHA256:" + hexValue);
        return "SHA256:" + hexValue;
    }

    private String extractMessageId(AbstractMX mx) {
        // First try to get the BizMsgIdr from AppHdr (Business Message Identifier)
        try {
            var bizMsgId = mx.getAppHdr();
            return ((BusinessAppHdrV04) bizMsgId).getBizMsgIdr();
        } catch (Exception e) {
            log.debug("Could not extract BizMsgIdr from AppHdr", e);
        }

        // Fallback: try to extract MsgId from the message body (Group Header)
        try {
            // This works for most payment messages (pacs, pain, camt)
            var message = mx.message();
            if (message != null) {
                // Use reflection to try common message ID fields
                var msgId = extractMsgIdFromMessage(mx);
                if (msgId != null && !msgId.isBlank()) {
                    return msgId;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract MsgId from message body", e);
        }

        // Final fallback: generate a unique ID based on timestamp and type
        String fallbackId = String.format("%s_%d",
                mx.getMxId().id().replace(".", "_"),
                System.currentTimeMillis());
        log.warn("Could not extract message ID, using fallback: {}", fallbackId);
        return fallbackId;
    }

    /**
     * Attempts to extract the message ID from the message body using reflection.
     * Tries common field paths used in ISO20022 messages.
     */
    private String extractMsgIdFromMessage(Object message) {
        try {
            // Try GrpHdr.MsgId (common in pacs, pain, camt messages)
            var grpHdrMethod = message.getClass().getMethod("getGrpHdr");
            var grpHdr = grpHdrMethod.invoke(message);

            if (grpHdr != null) {
                var msgIdMethod = grpHdr.getClass().getMethod("getMsgId");
                var msgId = msgIdMethod.invoke(grpHdr);
                if (msgId != null) {
                    return msgId.toString();
                }
            }
        } catch (Exception e) {
            // Try alternative paths for other message types
            try {
                // Some messages use Hdr.MsgId instead of GrpHdr.MsgId
                var hdrMethod = message.getClass().getMethod("getAppHdr");
                var hdr = hdrMethod.invoke(message);

                if (hdr != null) {
                    // var msgIdMethod = hdr.getClass().getMethod("getMsgId");
                    var msgId = ((BusinessAppHdrV04) hdr).getBizMsgIdr();// msgIdMethod.invoke(hdr);
                    if (msgId != null) {
                        return msgId;
                    }
                }
            } catch (Exception ex) {
                log.trace("Could not extract MsgId using alternative paths", ex);
            }
        }

        return null;
    }

    private String extractSender(AbstractMX mx) {
        return mx.getAppHdr().from();
    }

    private String extractReceiver(AbstractMX mx) {
        return mx.getAppHdr().to();
    }

    private String extractMessageType(AbstractMX mx) {
        return mx.getMxId().id();
    }
}