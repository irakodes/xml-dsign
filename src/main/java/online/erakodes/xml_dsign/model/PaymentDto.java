package online.erakodes.xml_dsign.model;

public record PaymentDto(
        String transactionId, String initiatorId, String recipientId,
        String narration, String reference, double amount) { }