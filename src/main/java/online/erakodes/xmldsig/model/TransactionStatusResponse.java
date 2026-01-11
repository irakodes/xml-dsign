package online.erakodes.xmldsig.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.erakodes.xmldsig.model.pacs.StatusReason;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record TransactionStatusResponse(
        String messageId,
        OffsetDateTime creationDateTime,

        String originalInstructionId,
        String originalEndToEndId,
        String originalTransactionId,

        TransactionStatus status,

        List<StatusReason> reasons
) {
}