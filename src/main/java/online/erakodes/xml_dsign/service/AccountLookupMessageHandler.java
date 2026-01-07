package online.erakodes.xml_dsign.service;

import online.erakodes.xml_dsign.helper.ISOMessageHandler;
import online.erakodes.xml_dsign.model.AccountLookupDto;
import online.erakodes.xml_dsign.model.AccountLookupResponse;
import online.erakodes.xml_dsign.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AccountLookupMessageHandler implements IMessageHandler<AccountLookupDto, AccountLookupResponse> {

    private final static Logger log = LoggerFactory.getLogger(AccountLookupMessageHandler.class);

    @Override
    public CompletableFuture<Result<AccountLookupResponse>> handle(AccountLookupDto request) {
        var message = ISOMessageHandler.formatCAMT00300107(new String[] {
                request.accountId(), request.accountName()
        });

        log.info("[CAMT.003.001.07] {}", message);
        return null;
    }
}