package online.erakodes.xmldsig.model;

import online.erakodes.xmldsig.model.pacs.StatusReason;

import java.time.OffsetDateTime;
import java.util.List;

public class TransactionStatusResponse {
    private String messageId;
    private OffsetDateTime creationDateTime;

    private String originalInstructionId;
    private String originalEndToEndId;
    private String originalTransactionId;

    private TransactionStatus status;

    private List<StatusReason> reasons;
}