package online.erakodes.xmldsig.model;


public record AccountLookupDto(
        String accountId,
        String accountName,
        String institution
) {
}