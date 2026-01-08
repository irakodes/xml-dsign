package online.erakodes.xmldsig.model.pacs;

import java.time.LocalDate;

public record Remittance(
        String unstructured,
        String invoiceNumber,
        LocalDate invoiceDate
) {
}