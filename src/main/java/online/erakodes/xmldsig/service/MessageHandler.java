package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.web.SignedMxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class MessageHandler implements IMessageHandler<Object, SignedMxMessage> {

    private final static Logger log = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    public CompletableFuture<Result<SignedMxMessage>> handle(Object request) {
        return null;
    }
}