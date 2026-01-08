package online.erakodes.xml_dsign.model.pacs;

public record Party(
        String name,
        String accountId,
        String agentId   // Bank / FI code
) {
}