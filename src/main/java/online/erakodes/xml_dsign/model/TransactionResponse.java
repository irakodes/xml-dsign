package online.erakodes.xml_dsign.model;

public record TransactionResponse(String messageId, String reference, boolean success, String message) {
}