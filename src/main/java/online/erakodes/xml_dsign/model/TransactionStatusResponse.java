package online.erakodes.xml_dsign.model;

import online.erakodes.xml_dsign.model.pacs.StatusReason;

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