package online.erakodes.xml_dsign.model;

import lombok.Data;

@Data
public class AccountLookupDto {
    String accountId;
    String accountName;
    String institution;
}