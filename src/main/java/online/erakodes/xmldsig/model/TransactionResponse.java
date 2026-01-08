package online.erakodes.xmldsig.model;

public record TransactionResponse(String messageId, String reference, boolean success, String message) {
}