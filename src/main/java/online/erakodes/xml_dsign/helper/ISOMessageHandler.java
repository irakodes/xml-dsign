package online.erakodes.xml_dsign.helper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.prowidesoftware.swift.model.mx.*;
import com.prowidesoftware.swift.model.mx.dic.*;
import online.erakodes.xml_dsign.model.ContactDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ISOMessageHandler {
    private final static Logger log = LoggerFactory.getLogger(ISOMessageHandler.class);
    private final static String AttchdDocNm = "307";
    private final static String AcctOpnCcy = "RWF";
    private final static String OPCO = "RW";
    private final static String OPCO_CITY = "KGL";
    private final static String DOI /* Date Of Incorporation */ = "2013-01-01";
    private final static String BICFI = "GTBIRWRKXXX";
    private final static String FI_NAME = "Guaranty Trust Bank (Rwanda) Ltd";
    private final static String FI_CODE = "070";

    /**
     * Formats a CAMT.003.001.07 message for account lookup.
     *
     * @param messageId The message identifier (can be null to auto-generate)
     * @param creDtTm   The creation date/time (can be null to use current time)
     * @param accountId The account ID to search for
     * @param mobile    The mobile number for contact details (optional; must match ISO PhoneNumber pattern if provided)
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
        conf.envelopeType = EnvelopeType.CUSTOM;

        // Produces <BusinessMessage><AppHdr>...</AppHdr><Document>...</Document></BusinessMessage>
        conf.rootElement = "BusinessMessage";

        conf.documentPrefix = null;
        conf.headerPrefix = null;
        conf.includeXMLDeclaration = true;
        conf.useCategoryAsDocumentPrefix = false;

        return mx.message(conf);
    }

    public static String formatACMT00700102(String messageId, String accountId, String accountName, String fullName,
                                            OffsetDateTime registrationDate, OffsetDateTime dateOfBirth, ContactDetails contactDetails) {
        log.debug("Handling CAMT.007.001.02 Message Generation (Account Creation)");
        messageId = messageId != null ? messageId : generateUniqueMessageId();
        var finalCreDtTm = OffsetDateTime.now();

        //var mxMessage = new MxCamt00700102();
        var mxMessage = new MxAcmt00700102();

        var appHdr = new BusinessAppHdrV04();
        appHdr.setBizMsgIdr(messageId);
        appHdr.setCreDt(finalCreDtTm);
        mxMessage.setAppHdr(appHdr);

        var msgId = new MessageIdentification1()
                .setId(messageId)
                .setCreDtTm(finalCreDtTm);

        //var prcId = new MessageIdentification1()
        //.setId(messageId);

        var refs = new References4()
                .setMsgId(msgId)
                //.setPrcId(prcId);
                .setPrcId(msgId);
        refs.addAttchdDocNm(AttchdDocNm);

        var customerAccount = new CustomerAccount4()
                .setId(new AccountIdentification4Choice()
                        .setOthr(new GenericAccountIdentification1()
                                .setId(accountId)))
                .setNm(contactDetails.name())
                .setSts(AccountStatus3Code.ENAB)
                .setCcy(AcctOpnCcy);

        var financialInstitutionId = new BranchAndFinancialInstitutionIdentification5()
                .setFinInstnId(new FinancialInstitutionIdentification8()
                        .setBICFI(BICFI)
                        .setNm(FI_NAME)
                        .setOthr(new GenericFinancialIdentification1()
                                .setId(FI_CODE)));

        var organisationId = new Organisation12()
                .setFullLglNm(FI_NAME)
                .setCtryOfOpr(OPCO)
                .setRegnDt(LocalDate.of(2013, 1, 1))
                .setLglAdr(new PostalAddress6()
                        .setAdrTp(AddressType2Code.BIZZ))
                .setOrgId(new OrganisationIdentification8()
                        .setAnyBIC(BICFI)
                        .addOthr(new GenericOrganisationIdentification1()
                                .setId(AttchdDocNm)
                                .setSchmeNm(new OrganisationIdentificationSchemeName1Choice()
                                        .setCd(AttchdDocNm))
                                .setIssr(AttchdDocNm)))
                .addSndr(new PartyIdentification40()
                        .setPstlAdr(new PostalAddress6().setAdrTp(AddressType2Code.HOME))
                        .setId(new PersonIdentification5()
                                .setDtAndPlcOfBirth(new DateAndPlaceOfBirth()
                                        .setBirthDt(dateOfBirth.toLocalDate())
                                        .setCityOfBirth(OPCO_CITY)
                                        .setCityOfBirth(OPCO))
                                .addOthr(new GenericPersonIdentification1()
                                        .setId(messageId)))
                        .setCtctDtls(new ContactDetails2()
                                .setNm(contactDetails.name())
                                .setEmailAdr(contactDetails.email())
                                .setMobNb(contactDetails.mobileNumber())));


        // var supplementaryData = new SupplementaryData1().setPlcAndNm("INSE|nickname")
        //         .setEnvlp(new SupplementaryDataEnvelope1().setAny(
        //                 new Object() {
        //                     final Object text = new Object() {
        //                         final String value = "";
        //                     };
        //                 }
        //         ));

        var acctOpeningRequest = new AccountOpeningRequestV02()
                .setRefs(refs)
                .setAcct(customerAccount)
                .setAcctSvcrId(financialInstitutionId)
                .setOrg(organisationId)
                .addSplmtryData(null);

        var msgHdr = new MessageHeader9();
        msgHdr.setMsgId(messageId);
        msgHdr.setCreDtTm(finalCreDtTm);

        return "";
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
        return System.currentTimeMillis() + UUID.randomUUID().toString()
                .replaceAll("-", "")
                .replaceAll("[a-zA-Z]", "").substring(0, 2);
    }
}