package online.erakodes.xmldsig.model;

public record PaymentDto(
        String transactionId, String initiatorId, String recipientId,
        String narration, String reference, double amount) { }