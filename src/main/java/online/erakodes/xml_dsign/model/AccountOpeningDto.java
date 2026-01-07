package online.erakodes.xml_dsign.model;

import java.time.OffsetDateTime;

public record AccountOpeningDto(
        String accountId,
        String accountName,
        String fullName,
        OffsetDateTime registrationDate,
        OffsetDateTime dateOfBirth,
        ContactDetails contactDetails
) {
}
