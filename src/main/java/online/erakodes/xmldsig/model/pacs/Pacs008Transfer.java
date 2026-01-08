package online.erakodes.xmldsig.model.pacs;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.PaymentDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Pacs008Transfer(
        String messageId,
        OffsetDateTime creationDateTime,
        String instructingAgentId,   // InstgAgt (070)
        String instructedAgentId,    // InstdAgt
        String instructionId,
        String endToEndId,
        String transactionId,
        double amount,
        String currency,
        LocalDate settlementDate,
        Party debtor,
        Party creditor,
        String purposeCode,
        Remittance remittance
) {
        private static final String DEFAULT_CURRENCY = "RWF";
        private static final String DEFAULT_FI_CODE = "070";

        public static Pacs008Transfer fromPaymentDto(PaymentDto paymentDto) {
                var messageId = paymentDto.reference() != null && !paymentDto.reference().isEmpty()
                        ? paymentDto.reference()
                        : ISOMessageHandler.generateUniqueMessageId();
                var creationDateTime = OffsetDateTime.now();
                var transactionId = paymentDto.transactionId();
            var settlementDate = LocalDate.now();

                var debtor = new Party(
                        paymentDto.initiatorId(),
                        paymentDto.initiatorId(),
                        DEFAULT_FI_CODE
                );

                var creditor = new Party(
                        paymentDto.recipientId(),
                        paymentDto.recipientId(),
                        null
                );

                Remittance remittance = null;
                if (paymentDto.narration() != null || paymentDto.reference() != null) {
                        remittance = new Remittance(
                                paymentDto.narration(),
                                paymentDto.reference(),
                                null
                        );
                }

                return new Pacs008Transfer(
                        messageId,
                        creationDateTime,
                        DEFAULT_FI_CODE,
                        DEFAULT_FI_CODE,
                    transactionId,
                    transactionId,
                        transactionId,
                        paymentDto.amount(),
                        DEFAULT_CURRENCY,
                        settlementDate,
                        debtor,
                        creditor,
                        null,
                        remittance
                );
        }
}