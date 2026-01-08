package online.erakodes.xmldsig.service;

import com.prowidesoftware.swift.model.mx.AbstractMX;
import online.erakodes.xmldsig.helper.XmlSigner;
import online.erakodes.xmldsig.web.SignedMxMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BodySigningService {

    @Value("${application.properties.security.keys.pass}")
    private String KEY_PASS;

    private final static Logger log = LoggerFactory.getLogger(BodySigningService.class);

    public SignedMxMessage wrapAndSign(SignedMxMessage message) {
        if (message == null) throw new IllegalArgumentException("Message cannot be null");

        var unsignedMxMessage = StringUtils.normalizeSpace(message.getSignedContent());
        var signedMxMessage = "";

        try {
            signedMxMessage = XmlSigner.sign(unsignedMxMessage, KEY_PASS);
            log.info("MX Message Signed Successfully");

            return SignedMxMessage.builder()
                    .creationTime(Instant.now())
                    .signedContent(signedMxMessage)
                    .messageType(getMxMessageType(unsignedMxMessage))
                    .messageId(getMxMessageId(unsignedMxMessage))
                    .sender(getMxMessageSender(unsignedMxMessage))
                    .receiver(getMxMessageReceiver(unsignedMxMessage))
                    .build();
        } catch (Exception e) {
            log.error("An error occurred while signing the MX Message: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String getMxMessageType(String mxMessage) {
        try {
            var mx = parse(mxMessage);
            var messageName = mx.getAppHdr().messageName();

            return mx.getMessageStandardType().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getMxMessageId(String mxMessage) {
        var mx = parse(mxMessage);
        return mx.getMxId().id();
    }

    private String getMxMessageSender(String mxMessage) {
        return parse(mxMessage).getAppHdr().from();
    }

    private String getMxMessageReceiver(String mxMessage) {
        return parse(mxMessage).getAppHdr().to();
    }

    private AbstractMX parse(String mxMessage) {
        return AbstractMX.parse(mxMessage);
    }

}