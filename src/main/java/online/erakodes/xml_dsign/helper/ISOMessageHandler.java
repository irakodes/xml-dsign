package online.erakodes.xml_dsign.helper;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prowidesoftware.swift.model.mx.BusinessAppHdrV01;
import com.prowidesoftware.swift.model.mx.EnvelopeType;
import com.prowidesoftware.swift.model.mx.MxCamt00300107;
import com.prowidesoftware.swift.model.mx.MxWriteConfiguration;
import com.prowidesoftware.swift.model.mx.dic.AccountCriteria3Choice;
import com.prowidesoftware.swift.model.mx.dic.AccountCriteria7;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentification4Choice;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentificationSearchCriteria2Choice;
import com.prowidesoftware.swift.model.mx.dic.AccountQuery3;
import com.prowidesoftware.swift.model.mx.dic.CashAccountSearchCriteria7;
import com.prowidesoftware.swift.model.mx.dic.Contact4;
import com.prowidesoftware.swift.model.mx.dic.GenericAccountIdentification1;
import com.prowidesoftware.swift.model.mx.dic.GenericPersonIdentification1;
import com.prowidesoftware.swift.model.mx.dic.GetAccountV07;
import com.prowidesoftware.swift.model.mx.dic.MessageHeader9;
import com.prowidesoftware.swift.model.mx.dic.Party38Choice;
import com.prowidesoftware.swift.model.mx.dic.PartyIdentification135;
import com.prowidesoftware.swift.model.mx.dic.PersonIdentification13;
import com.prowidesoftware.swift.model.mx.dic.PersonIdentificationSchemeName1Choice;


public class ISOMessageHandler {
    private final static Logger log = LoggerFactory.getLogger(ISOMessageHandler.class);

    /**
     * Formats a CAMT.003.001.07 message for account lookup.
     *
     * @param messageId The message identifier (can be null to auto-generate)
     * @param creDtTm The creation date/time (can be null to use current time)
     * @param accountId The account ID to search for
     * @return The formatted XML message as a string
     */
    public static String formatCAMT00300107(String messageId, OffsetDateTime creDtTm, String accountId) {
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
        var appHdr = new BusinessAppHdrV01();
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

        // Build Account Owner (AcctOwnr):
        // AcctOwnr -> CtctDtls -> MobNb
        // AcctOwnr -> Id -> PrvtId -> Othr -> SchmeNm -> Prtry, Id
        var acctOwnr = new PartyIdentification135();

        // Set Contact Details with Mobile Number
        var ctctDtls = new Contact4();
        ctctDtls.setMobNb("999999999");
        acctOwnr.setCtctDtls(ctctDtls);

        // Set Private ID structure
        var id = new Party38Choice();
        var prvtId = new PersonIdentification13();
        var othrId = new GenericPersonIdentification1();

        // Set Scheme Name with Prtry (empty)
        var schmeNm = new PersonIdentificationSchemeName1Choice();
        schmeNm.setPrtry("");
        othrId.setSchmeNm(schmeNm);
        othrId.setId("");

        prvtId.addOthr(othrId);
        id.setPrvtId(prvtId);
        acctOwnr.setId(id);

        // Set Account Owner in Search Criteria
        schCrit.setAcctOwnr(acctOwnr);

        // Complete the structure: NewCrit -> SchCrit
        newCrit.addSchCrit(schCrit);
        acctCritChoice.setNewCrit(newCrit);
        acctQryDef.setAcctCrit(acctCritChoice);
        getAcct.setAcctQryDef(acctQryDef);
        mx.setGetAcct(getAcct);

        // Configure XML output with BusinessMessage envelope
        var conf = new MxWriteConfiguration();
        conf.envelopeType = EnvelopeType.BME_V1;  // Produces <BusinessMessage><AppHdr>...</AppHdr><Document>...</Document></BusinessMessage>

        return mx.message(conf);
    }

    /**
     * Legacy method for backward compatibility.
     * Formats a CAMT.003.001.07 message using array arguments.
     *
     * @param args Array with [accountId, mobileNumber]
     * @return The formatted XML message as a string
     */
    public static String formatCAMT00300107(String[] args) {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("At least account ID is required");
        }
        return formatCAMT00300107(null, null, args[0]);
    }

    private static String generateUniqueMessageId() {
        return UUID.randomUUID().toString()
        .replaceAll("^[0-9]", "");
    }
}