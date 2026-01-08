package online.erakodes.xml_dsign.service;

import online.erakodes.xml_dsign.helper.ISOMessageHandler;
import online.erakodes.xml_dsign.model.AccountOpeningDto;
import online.erakodes.xml_dsign.model.AccountOpeningResponse;
import online.erakodes.xml_dsign.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AccountOpeningMessageHandler implements IMessageHandler<AccountOpeningDto, AccountOpeningResponse> {

    private final static Logger log = LoggerFactory.getLogger(AccountOpeningMessageHandler.class);

    @Override
    public CompletableFuture<Result<AccountOpeningResponse>> handle(AccountOpeningDto request) {
        var contactDetails = request.contactDetails();
        String[] args;

        if (contactDetails != null) {
            args = new String[] {
                request.accountId(),
                request.accountName(),
                request.fullName(),
                contactDetails.name() != null ? contactDetails.name() : "",
                contactDetails.mobileNumber() != null ? contactDetails.mobileNumber() : "",
                contactDetails.email() != null ? contactDetails.email() : "",
                contactDetails.address() != null ? contactDetails.address() : ""
            };
        } else {
            args = new String[] {
                request.accountId(),
                request.accountName(),
                request.fullName()
            };
        }

        var message = ISOMessageHandler.formatACMT00700102(args);

        log.info("[ACMT.007.001.02] {}", message);

        var response = new AccountOpeningResponse();
        response.setAccountId(request.accountId());
        response.setSuccess(true);

        return CompletableFuture.completedFuture(new Result.Ok<>(response, null));
    }
}
