package online.erakodes.xml_dsign.model;


public record AccountLookupDto(
        String accountId,
        String accountName,
        String institution
) {
}