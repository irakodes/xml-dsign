package online.erakodes.xmldsig.model.pacs;

public record Party(
        String name,
        String accountId,
        String agentId   // Bank / FI code
) {
}