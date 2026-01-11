package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.AccountLookupDto;
import online.erakodes.xmldsig.model.AccountLookupResponse;
import online.erakodes.xmldsig.model.Error;
import online.erakodes.xmldsig.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class AccountLookupMessageHandler implements IMessageHandler<AccountLookupDto, AccountLookupResponse> {

    private final static Logger log = LoggerFactory.getLogger(AccountLookupMessageHandler.class);

    @Override
    public Result<AccountLookupResponse> handle(AccountLookupDto request) {
        try {
            var message = ISOMessageHandler.formatCAMT00300107(new String[]{
                    request.accountId(), request.accountName()
            });

            log.info("[CAMT.003.001.07] {}", message);

            // TODO: Perform actual account lookup
            var response = new AccountLookupResponse();
            response.setExists(true);

            return new Result.Ok<>(response, null);
        } catch (Exception e) {
            log.error("Failed to lookup account: {}", request.accountId(), e);
            return new Result.Fail<>(Error.builder()
                    .errorCode("ACCOUNT_LOOKUP_ERROR")
                    .errorType("Business Logic Error")
                    .detail("Failed to lookup account: " + e.getMessage())
                    .build());
        }
    }
}