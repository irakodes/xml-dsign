package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.Error;
import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.model.TransactionStatus;
import online.erakodes.xmldsig.model.TransactionStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;


@Component
public class TransactionStatusHandler implements IMessageHandler<String, TransactionStatusResponse> {

    private final static Logger log = LoggerFactory.getLogger(TransactionStatusHandler.class);

    @Override
    public Result<TransactionStatusResponse> handle(String txId) {
        try {
            var message = ISOMessageHandler.formatPACS00200110(txId, txId, txId);

            log.info("[PACS.002.001.10] Message: {}", message);

            // TODO: Fetch actual transaction status from the system
            var response = TransactionStatusResponse.builder()
                    .messageId(UUID.randomUUID().toString())
                    .creationDateTime(OffsetDateTime.now())
                    .originalTransactionId(txId)
                    .status(TransactionStatus.PENDING)
                    .build();

            return new Result.Ok<>(response, null);
        } catch (Exception e) {
            log.error("Failed to retrieve transaction status for txId: {}", txId, e);
            return new Result.Fail<>(Error.builder()
                    .errorCode("TRANSACTION_STATUS_ERROR")
                    .errorType("Business Logic Error")
                    .detail("Failed to retrieve transaction status: " + e.getMessage())
                    .build());
        }
    }
}