package online.erakodes.xmldsig.service;

import com.prowidesoftware.swift.model.mx.AbstractMX;
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

            var finalMessage = SignedMxMessage.builder()
                    .messageId(messageId)
                    .creationTime(Instant.now())
                    .sender(extractSender(parsedMx))
                    .receiver(extractReceiver(parsedMx))
                    .messageType(extractMessageType(parsedMx))
                    .signedContent(signedMxMessage)
                    .signatureInfo(signatureInfo)
                    .status(SignedMxMessage.MessageStatus.SIGNED)
                    .build();

            fileLogger.logSignedXml(messageId, signedMxMessage); //TODO: Move to a separate service (logger

            return finalMessage;
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
        return mx.getMxId().id();
    }

    private String extractSender(AbstractMX mx) {
        return mx.getAppHdr().from();
    }

    private String extractReceiver(AbstractMX mx) {
        return mx.getAppHdr().to();
    }

    private String extractMessageType(AbstractMX mx) {
        return mx.getMessageStandardType().toString();
    }
}