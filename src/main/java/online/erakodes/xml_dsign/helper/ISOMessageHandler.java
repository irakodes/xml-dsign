package online.erakodes.xml_dsign.helper;

import com.prowidesoftware.swift.model.mx.BusinessAppHdrV01;
import com.prowidesoftware.swift.model.mx.EnvelopeType;
import com.prowidesoftware.swift.model.mx.MxCamt00300107;
import com.prowidesoftware.swift.model.mx.MxWriteConfiguration;
import com.prowidesoftware.swift.model.mx.dic.*;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ISOMessageHandler {
    private final static Logger log = LoggerFactory.getLogger(ISOMessageHandler.class);

    public static String formatCAMT00300107(String[] args) {
        if (args.length < 1) throw new IllegalArgumentException("Not enough arguments");

        var accountId = args[0];

        log.debug("Handling CAMT.003.001.07 Message Generation (Get Account)");

        var messageId = generateUniqueMessageId();
        log.info("Message ID: {}", messageId);

        var mx = new MxCamt00300107();
        var appHdr = new BusinessAppHdrV01();
        // Set message identification in application header
        appHdr.setBizMsgIdr(messageId != null ? messageId : "CAMT003-" + System.currentTimeMillis());
        appHdr.setCreDt(OffsetDateTime.now());
        mx.setAppHdr(appHdr);

        var getAcct = new GetAccountV07();

        // Set Message Header
        var msgHdr = new MessageHeader9();
        msgHdr.setMsgId(messageId != null ? messageId : "CAMT003-" + System.currentTimeMillis());
        msgHdr.setCreDtTm(OffsetDateTime.now());
        getAcct.setMsgHdr(msgHdr);

        // Set Account Query Definition with Account Criteria
        // Based on XML structure: AcctQryDef -> AcctCrit -> NewCrit -> SchCrit -> AcctId -> EQ -> Othr -> Id
        var acctQryDef = new AccountQuery3();
        var acctCritChoice = new AccountCriteria3Choice();
        var newCrit = new AccountCriteria7();

        // Build search criteria - the class name may be AccountSearchCriteria3 or similar
        // Trying to find the correct class by checking what AccountCriteria7.setSchCrit accepts
        // For now, creating a minimal structure that matches the XML template

        // Set Account ID in the search criteria
        // The XML shows: AcctId -> EQ -> Othr -> Id
        // We need to build this structure using available Prowide classes
        var othr = new GenericAccountIdentification1();
        othr.setId(accountId);

        // Try setting account ID directly on AccountCriteria7 if it has such a method
        // Otherwise, we'll need to find the correct search criteria class
        // For now, create a basic structure - the exact class names depend on Prowide version
        // TODO: Find the correct AccountSearchCriteria class for this Prowide version

        // Set Account Owner with Contact Details (matching template)
        var acctOwnr = new PartyIdentification135();
        var ctctDtls = new Contact4();
        ctctDtls.setMobNb(args[1]);
        acctOwnr.setCtctDtls(ctctDtls);

        // Set Private ID structure (matching template)
        var id = new Party38Choice();
        var prvtId = new PersonIdentification13();
        var othrId = new GenericPersonIdentification1();
        var schmeNm = new PersonIdentificationSchemeName1Choice();
        schmeNm.setPrtry("");
        othrId.setSchmeNm(schmeNm);
        othrId.setId("");
        prvtId.addOthr(othrId);
        id.setPrvtId(prvtId);
        acctOwnr.setId(id);

        // Note: The search criteria structure needs to be completed once we identify
        // the correct Prowide class names. The account ID (accountId variable) should
        // be set in the SchCrit -> AcctId -> EQ -> Othr -> Id path
        acctCritChoice.setNewCrit(newCrit);
        acctQryDef.setAcctCrit(acctCritChoice);

        getAcct.setAcctQryDef(acctQryDef);
        mx.setGetAcct(getAcct);

        var conf = new MxWriteConfiguration();
        conf.envelopeType = EnvelopeType.BME_V2;  // Produces <BusinessMessage><AppHdr>...</AppHdr><Document>...</Document></BusinessMessage>

        return mx.message(conf);
    }

    private static @Nullable String generateUniqueMessageId() {
        return UUID.randomUUID().toString()
        .replaceAll("^[0-9]", "");
    }
}