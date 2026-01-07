package online.erakodes.xml_dsign.model.pacs;

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
}