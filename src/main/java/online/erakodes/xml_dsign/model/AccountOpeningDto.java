package online.erakodes.xml_dsign.model;

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
