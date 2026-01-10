package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.AccountLookupDto;
import online.erakodes.xmldsig.model.AccountLookupResponse;
import online.erakodes.xmldsig.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class AccountLookupMessageHandler implements IMessageHandler<AccountLookupDto, AccountLookupResponse> {

    private final static Logger log = LoggerFactory.getLogger(AccountLookupMessageHandler.class);

    @Override
    public Result<AccountLookupResponse> handle(AccountLookupDto request) {
        var message = ISOMessageHandler.formatCAMT00300107(new String[] {
                request.accountId(), request.accountName()
        });

        log.info("[CAMT.003.001.07] {}", message);
        return null;
    }
}