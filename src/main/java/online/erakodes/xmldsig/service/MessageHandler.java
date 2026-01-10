package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.model.AccountLookupDto;
import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.web.SignedMxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class MessageHandler implements IMessageHandler<Object, SignedMxMessage> {

    private final static Logger log = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    public Result<SignedMxMessage> handle(Object request) {
        if (request instanceof AccountLookupDto lookupDto) {
            return handleCAMT00300107(lookupDto);
        }
        return null;
    }

    private SignedMxMessage handleCAMT00300107(AccountLookupDto lookupRequest) {
        return null;
    }
}