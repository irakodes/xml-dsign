package online.erakodes.xml_dsign.helper;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.prowidesoftware.swift.model.mx.*;
import com.prowidesoftware.swift.model.mx.dic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ISOMessageHandler {
    private final static Logger log = LoggerFactory.getLogger(ISOMessageHandler.class);

    /**
     * Formats a CAMT.003.001.07 message for account lookup.
     *
     * @param messageId The message identifier (can be null to auto-generate)
     * @param creDtTm The creation date/time (can be null to use current time)
     * @param accountId The account ID to search for
     * @param mobile The mobile number for contact details (optional; must match ISO PhoneNumber pattern if provided)
     * @return The formatted XML message as a string
     */
    public static String formatCAMT00300107(String messageId, OffsetDateTime creDtTm, String accountId, String mobile) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }

        log.debug("Handling CAMT.003.001.07 Message Generation (Get Account)");

        // Generate message ID if not provided
        var finalMessageId = messageId != null ? messageId : generateUniqueMessageId();
        log.info("Message ID: {}", finalMessageId);

        // Use current time if not provided
        var finalCreDtTm = creDtTm != null ? creDtTm : OffsetDateTime.now();

        // Create the main message object
        var mx = new MxCamt00300107();

        // Set Application Header
        var appHdr = new BusinessAppHdrV04();
        appHdr.setBizMsgIdr(finalMessageId);
        appHdr.setCreDt(finalCreDtTm);
        mx.setAppHdr(appHdr);

        // Create GetAccount message
        var getAcct = new GetAccountV07();

        // Set Message Header
        var msgHdr = new MessageHeader9();
        msgHdr.setMsgId(finalMessageId);
        msgHdr.setCreDtTm(finalCreDtTm);
        getAcct.setMsgHdr(msgHdr);

        // Build Account Query Definition structure:
        // AcctQryDef -> AcctCrit -> NewCrit -> SchCrit -> AcctId -> EQ -> Othr -> Id
        var acctQryDef = new AccountQuery3();
        var acctCritChoice = new AccountCriteria3Choice();
        var newCrit = new AccountCriteria7();

        // Build Search Criteria (SchCrit) - using CashAccountSearchCriteria7
        var schCrit = new CashAccountSearchCriteria7();

        // Build Account Identification Search Criteria:
        // AcctId -> EQ -> Othr -> Id
        var acctIdSearch = new AccountIdentificationSearchCriteria2Choice();
        var acctIdChoice = new AccountIdentification4Choice();
        var othr = new GenericAccountIdentification1();
        othr.setId(accountId);
        acctIdChoice.setOthr(othr);
        acctIdSearch.setEQ(acctIdChoice);
        schCrit.addAcctId(acctIdSearch);

        // Build Account Owner (AcctOwnr) only with contact details if mobile is provided
        var acctOwnr = new PartyIdentification135();

        if (mobile != null && !mobile.isEmpty()) {
            var ctctDtls = new Contact4();
            ctctDtls.setMobNb(mobile);
            acctOwnr.setCtctDtls(ctctDtls);
        }

        // Set Account Owner in Search Criteria (omit Id since no value provided)
        schCrit.setAcctOwnr(acctOwnr);

        // Complete the structure: NewCrit -> SchCrit
        newCrit.addSchCrit(schCrit);
        acctCritChoice.setNewCrit(newCrit);
        acctQryDef.setAcctCrit(acctCritChoice);
        getAcct.setAcctQryDef(acctQryDef);
        mx.setGetAcct(getAcct);

        // Configure XML output with BusinessMessage envelope
        var conf = new MxWriteConfiguration();
        conf.envelopeType = EnvelopeType.SWIFT;  // Produces <BusinessMessage><AppHdr>...</AppHdr><Document>...</Document></BusinessMessage>
        conf.documentPrefix = null;
        conf.includeXMLDeclaration = true;

        return mx.message(conf);
    }

    /**
     * Legacy method for backward compatibility.
     * Formats a CAMT.003.001.07 message using array arguments.
     *
     * @param args Array with [accountId, mobileNumber] (mobileNumber optional)
     * @return The formatted XML message as a string
     */
    public static String formatCAMT00300107(String[] args) {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("At least account ID is required");
        }
        var accountId = args[0];
        var mobile = args.length > 1 ? args[1] : null;
        return formatCAMT00300107(null, null, accountId, mobile);
    }

    private static String generateUniqueMessageId() {
        return UUID.randomUUID().toString()
        .replaceAll("^[0-9]", "");
    }
}