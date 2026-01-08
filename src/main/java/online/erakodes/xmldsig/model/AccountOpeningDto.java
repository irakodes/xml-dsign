package online.erakodes.xmldsig.model;

import java.time.OffsetDateTime;

public record AccountOpeningDto(
        String accountId,
        String proxy,
        String accountName,
        String fullName,
        boolean withProxy,
        OffsetDateTime registrationDate,
        OffsetDateTime dateOfBirth,
        ContactDetails contactDetails
) {
}
