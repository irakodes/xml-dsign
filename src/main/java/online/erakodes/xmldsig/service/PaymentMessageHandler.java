package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.Error;
import online.erakodes.xmldsig.model.PaymentDto;
import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.model.TransactionResponse;
import online.erakodes.xmldsig.model.pacs.Pacs008Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class PaymentMessageHandler implements IMessageHandler<PaymentDto, TransactionResponse> {

    private final static Logger log = LoggerFactory.getLogger(PaymentMessageHandler.class);

    @Override
    public Result<TransactionResponse> handle(PaymentDto request) {
        try {
            var pacs008 = Pacs008Transfer.fromPaymentDto(request);
            var xmlMessage = ISOMessageHandler.formatPACS00800108(pacs008);

            log.info("Generated PACS.008.001.08 Message: {}", xmlMessage);

            // TODO: Process the payment and return actual transaction response
            var response = new TransactionResponse(
                    pacs008.messageId(),
                    pacs008.transactionId(),
                    true,
                    "Payment processed successfully"
            );

            return new Result.Ok<>(response, null);
        } catch (Exception e) {
            log.error("Failed to process payment request", e);
            return new Result.Fail<>(Error.builder()
                    .errorCode("PAYMENT_PROCESSING_ERROR")
                    .errorType("Business Logic Error")
                    .detail("Failed to process payment: " + e.getMessage())
                    .build());
        }
    }
}