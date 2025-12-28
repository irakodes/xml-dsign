package online.erakodes.xml_dsign.service;

import online.erakodes.xml_dsign.helper.ISOMessageHandler;
import online.erakodes.xml_dsign.model.AccountLookupDto;
import online.erakodes.xml_dsign.model.AccountLookupResponse;
import online.erakodes.xml_dsign.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class MessageHandler implements IMessageHandler<AccountLookupDto, AccountLookupResponse> {

    private final static Logger log = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    public CompletableFuture<Result<AccountLookupResponse>> handle(AccountLookupDto request) {
        var message = ISOMessageHandler.formatCAMT00300107(new String[] {
                request.getAccountId(), request.getAccountName()
        });

        log.info("CAMT.003.001.07 formatted: {}", message);
        return null;
    }
}