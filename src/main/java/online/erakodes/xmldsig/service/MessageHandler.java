package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.AccountLookupDto;
import online.erakodes.xmldsig.model.Error;
import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.web.SignedMxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Component
public class MessageHandler implements IMessageHandler<Object, SignedMxMessage> {

    private final static Logger log = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    public Result<SignedMxMessage> handle(Object request) {
        try {
            if (request instanceof AccountLookupDto lookupDto) {
                var mx = handleCAMT00300107(lookupDto);
                return new Result.Ok<>(mx, null);
            }

            log.warn("Unsupported request type: {}", request.getClass().getName());
            return new Result.Fail<>(Error.builder()
                    .errorCode("UNSUPPORTED_REQUEST_TYPE")
                    .errorType("Validation Error")
                    .detail("Unsupported request type: " + request.getClass().getSimpleName())
                    .build());
        } catch (Exception e) {
            log.error("Failed to handle message request", e);
            return new Result.Fail<>(Error.builder()
                    .errorCode("MESSAGE_HANDLING_ERROR")
                    .errorType("System Error")
                    .detail("Failed to handle message: " + e.getMessage())
                    .build());
        }
    }

    private SignedMxMessage handleCAMT00300107(AccountLookupDto request) {
        var message = ISOMessageHandler.formatCAMT00300107(new String[]{
                request.accountId(), request.accountName()
        });

        log.info("[CAMT.003.001.07] {}", message);

        // TODO: Handle the message and do some operations with Fineract

        return SignedMxMessage.builder()
                .messageId("")
                .creationTime(Instant.now())
                .sender("")
                .receiver("")
                .messageType("CAMT.003.001.07")
                .signedContent(message)
                .signatureInfo(SignedMxMessage.SignatureInfo.builder()
                        .build())
                .status(SignedMxMessage.MessageStatus.CREATED)
                .build();
    }
}