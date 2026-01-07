package online.erakodes.xml_dsign.helper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.prowidesoftware.swift.model.mx.*;
import com.prowidesoftware.swift.model.mx.dic.*;
import online.erakodes.xml_dsign.model.ContactDetails;
import online.erakodes.xml_dsign.model.pacs.Pacs008Transfer;
import org.jspecify.annotations.NonNull;
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
     * @param accountId The account ID to search for
     * @param mobile    The mobile number for contact details (optional; must match ISO PhoneNumber pattern if provided)
     * @return The formatted XML message as a string
     */
    private static String formatCAMT00300107(String accountId, String mobile) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }

        log.debug("Handling CAMT.003.001.07 Message Generation (Get Account)");

        // Generate message ID if not provided
        var finalMessageId = generateUniqueMessageId();
        log.info("Message ID: {}", finalMessageId);

        // Use current time if not provided
        var finalCreDtTm = OffsetDateTime.now();

        // Create the main message object
        var mx = new MxCamt00300107();

        // Set Application Header
        var appHdr = new BusinessAppHdrV04();
        appHdr.setBizMsgIdr(finalMessageId);
        appHdr.setCreationDate(true);
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
        var acctQryDef = getAccountQuery3(accountId, mobile);
        getAcct.setAcctQryDef(acctQryDef);
        mx.setGetAcct(getAcct);

        var conf = getMxWriteConfiguration();
        return mx.message(conf);
    }

    private static String
    formatPACS00800108(String messageId, OffsetDateTime creationDateTime, String instructionId,
                       String endToEndId, String transactionId,
                       String instructedAgentId, String creditorAgentId,
                       double amount, String currency, LocalDate settlementDate,
                       String debtorName, String debtorAccountId,
                       String creditorName, String creditorAccountId, String purposeCode,
                       String remittanceInformation, String invoiceNumber,
                       LocalDate invoiceDate) {
        String[] args = new String[] {
            messageId,
            creationDateTime != null ? creationDateTime.toString() : null,
            instructionId,
            endToEndId,
            transactionId,
            instructedAgentId,
            creditorAgentId,
            String.valueOf(amount),
            currency,
            settlementDate != null ? settlementDate.toString() : null,
            debtorName,
            debtorAccountId,
            creditorName,
            creditorAccountId,
            purposeCode,
            remittanceInformation,
            invoiceNumber,
            invoiceDate != null ? invoiceDate.toString() : null
        };
        return formatPACS00800108(args);
    }

    private static String formatPACS00800108(String[] args) {
        log.debug("Handling PACS.008.001.08 Message Generation (Payment Initiation)");
        if (args == null || args.length < 17) throw new
                IllegalArgumentException("Insufficient arguments provided");

        var messageId = args[0] != null ? args[0] : generateUniqueMessageId();
        var creDtTm = args[1] != null ? OffsetDateTime.parse(args[1]) : OffsetDateTime.now();
        var instructionId = args[2];
        var endToEndId = args[3];
        var transactionId = args[4];
        var instructedAgentId = args[5];
        var creditorAgentId = args[6];
        var amount = Double.parseDouble(args[7]);
        var currency = args[8] != null ? args[8] : AcctOpnCcy;
        var settlementDate = args[9] != null ? LocalDate.parse(args[9]) : LocalDate.now();
        var debtorName = args[10];
        var debtorAccountId = args[11];
        var creditorName = args[12];
        var creditorAccountId = args[13];
        var purposeCode = args[14];
        var remittanceInformation = args[15];
        var invoiceNumber = args[16];
        var invoiceDate = args[17] != null ? LocalDate.parse(args[17]) : null;

        var groupHdr = new GroupHeader93()
                .setMsgId(messageId)
                .setCreDtTm(creDtTm)
                .setNbOfTxs("1")
                .setSttlmInf(new SettlementInstruction7()
                        .setSttlmMtd(SettlementMethod1Code.INDA))
                .setInstdAgt(new BranchAndFinancialInstitutionIdentification6()
                        .setBrnchId(new BranchData3()
                                .setId(instructedAgentId != null ? instructedAgentId : FI_CODE)))
                .setInstgAgt(new BranchAndFinancialInstitutionIdentification6()
                        .setBrnchId(new BranchData3()
                                .setId(FI_CODE)));

        var cdtTrfTxInf = new CreditTransferTransaction39()
                .setPmtId(new PaymentIdentification7()
                        .setInstrId(instructionId != null ? instructionId : messageId)
                        .setEndToEndId(endToEndId != null ? endToEndId : messageId).setUETR(transactionId))
                .setPmtTpInf(new PaymentTypeInformation28()
                        .setCtgyPurp(new CategoryPurpose1Choice()
                                .setCd(purposeCode != null ? purposeCode : "000")))
                .setIntrBkSttlmAmt(new ActiveCurrencyAndAmount()
                        .setCcy(currency)
                        .setValue(BigDecimal.valueOf(amount)))
                .setIntrBkSttlmDt(settlementDate)
                .setChrgBr(ChargeBearerType1Code.SHAR)
                .setInitgPty(new PartyIdentification135()
                        .setNm(FI_NAME)
                        .setId(new Party38Choice()
                                .setOrgId(new OrganisationIdentification29()
                                        .addOthr(new GenericOrganisationIdentification1()
                                                .setId(FI_CODE)
                                                .setSchmeNm(new OrganisationIdentificationSchemeName1Choice()
                                                        .setCd(FI_CODE))))))
                .setDbtr(new PartyIdentification135()
                        .setNm(debtorName)
                        .setId(new Party38Choice()
                                .setPrvtId(new PersonIdentification13()
                                        .addOthr(new GenericPersonIdentification1()
                                                .setId(debtorAccountId)))))
                .setDbtrAcct(new CashAccount38()
                        .setCcy(currency)
                        .setId(new AccountIdentification4Choice()
                                .setOthr(new GenericAccountIdentification1()
                                        .setId(debtorAccountId))))
                .setDbtrAgt(new BranchAndFinancialInstitutionIdentification6().setFinInstnId(new FinancialInstitutionIdentification18()
                        .setOthr(new GenericFinancialIdentification1().setId(FI_CODE))))
                .setCdtr(new PartyIdentification135()
                        .setId(new Party38Choice()
                                .setOrgId(new OrganisationIdentification29()))
                        .setNm(creditorName))
                .setCdtrAgt(new BranchAndFinancialInstitutionIdentification6()
                        .setFinInstnId(new FinancialInstitutionIdentification18()
                                .setBICFI(creditorAgentId != null ? creditorAgentId : "SOME_BANK_BIC")))
                .setCdtrAcct(new CashAccount38().setId(new AccountIdentification4Choice()
                        .setOthr(new GenericAccountIdentification1()
                                .setId(creditorAccountId))
                        .setIBAN("SOME_BANK_IBAN")))
                .setPurp(new Purpose2Choice()
                        .setCd(purposeCode));

        var rmtInf = new RemittanceInformation16();
        if (remittanceInformation != null) {
            rmtInf.addUstrd(remittanceInformation);
        }
        if (invoiceNumber != null) {
            rmtInf.addUstrd(invoiceNumber);
        }

        if (invoiceNumber != null || invoiceDate != null) {
            var rfrdDoc = new ReferredDocumentInformation7()
                    .setTp(new ReferredDocumentType4()
                            .setCdOrPrtry(new ReferredDocumentType3Choice()
                                    .setCd(DocumentType6Code.CINV)));
            if (invoiceNumber != null) {
                rfrdDoc.setNb(invoiceNumber);
            }
            if (invoiceDate != null) {
                rfrdDoc.setRltdDt(invoiceDate);
            }
            rmtInf.addStrd(new StructuredRemittanceInformation16()
                    .addRfrdDocInf(rfrdDoc));
        }
        cdtTrfTxInf.setRmtInf(rmtInf);

        var fiToFi = new FIToFICustomerCreditTransferV08()
                .setGrpHdr(groupHdr)
                .addCdtTrfTxInf(cdtTrfTxInf);

        var mxMessage = new MxPacs00800108().setFIToFICstmrCdtTrf(fiToFi);

        var appHdr = new BusinessAppHdrV04();
        appHdr.setBizMsgIdr(messageId);
        appHdr.setCreationDate(true);
        appHdr.setMsgDefIdr("pacs.008.001.08");
        mxMessage.setAppHdr(appHdr);

        return mxMessage.message(getMxWriteConfiguration());
    }

    private static @NonNull AccountQuery3 getAccountQuery3(String accountId, String mobile) {
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
        return acctQryDef;
    }

    private static String formatACMT00700102(String accountId, String accountName, String fullName,
                                             OffsetDateTime registrationDate, OffsetDateTime dateOfBirth, ContactDetails contactDetails) {
        log.debug("Handling CAMT.007.001.02 Message Generation (Account Creation)");
        var messageId = generateUniqueMessageId();
        var finalCreDtTm = OffsetDateTime.now();

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
                .setNm(contactDetails != null ? contactDetails.name() : accountName)
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
                        .setCtctDtls(contactDetails != null ? new ContactDetails2()
                                .setNm(contactDetails.name())
                                .setEmailAdr(contactDetails.email())
                                .setMobNb(contactDetails.mobileNumber()) : null));

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

        //var mxMessage = new MxCamt00700102();
        var mxMessage = new MxAcmt00700102()
                .setAcctOpngReq(acctOpeningRequest);

        var appHdr = new BusinessAppHdrV04();
        appHdr.setBizMsgIdr(messageId);
        appHdr.setCreationDate(true);
        appHdr.setMsgDefIdr("acmt.007.001.02");

        mxMessage.setAppHdr(appHdr);
        var config = getMxWriteConfiguration();

        return mxMessage.message(config);
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
        return formatCAMT00300107(accountId, mobile);
    }

    /**
     * Legacy method for backward compatibility.
     * Formats an ACMT.007.001.02 message using array arguments.
     *
     * @param args Array of input parameters with the following structure:
     *             - args[0]: The account ID (required).
     *             - args[1]: The account name (required).
     *             - args[2]: The full name of the account holder (required).
     *             - args[3 And Beyond]: Optional additional arguments that can be parsed into contact details
     *             (e.g., email, mobile number) through {@link ContactDetails#fromArray(String[])}.
     * @return The formatted XML message as a string representing ACMT.007.001.02.
     * The result encapsulates the account opening request with the provided details.
     */
    public static String formatACMT00700102(String[] args) {
        return formatACMT00700102(args[0], args[1], args[2], OffsetDateTime.now(), OffsetDateTime.now(), args.length > 3 ? ContactDetails.fromArray(args) : null);
    }

    public static String formatPACS00800108(Pacs008Transfer transfer) {
        return formatPACS00800108(
                transfer.messageId(),
                transfer.creationDateTime(),
                transfer.instructionId(),
                transfer.endToEndId(),
                transfer.transactionId(),
                transfer.instructedAgentId(),
                transfer.creditor().agentId(),
                transfer.amount(),
                transfer.currency(),
                transfer.settlementDate(),
                transfer.debtor().name(),
                transfer.debtor().accountId(),
                transfer.creditor().name(),
                transfer.creditor().accountId(),
                transfer.purposeCode(),
                transfer.remittance() != null ? transfer.remittance().unstructured() : null,
                transfer.remittance() != null ? transfer.remittance().invoiceNumber() : null,
                transfer.remittance() != null ? transfer.remittance().invoiceDate() : null
        );
    }

    public static String generateUniqueMessageId() {
        return System.currentTimeMillis() + UUID.randomUUID().toString()
                .replaceAll("-", "")
                .replaceAll("[a-zA-Z]", "").substring(0, 2);
    }

    private static MxWriteConfiguration getMxWriteConfiguration() {
        // Configure XML output with BusinessMessage envelope
        var conf = new MxWriteConfiguration();
        conf.envelopeType = EnvelopeType.CUSTOM;

        // Produces
        // <BusinessMessage>
        //      <AppHdr>...</AppHdr>
        //          <Document>...</Document>
        // </BusinessMessage>
        conf.rootElement = "BusinessMessage";

        conf.documentPrefix = null;
        conf.headerPrefix = null;
        conf.includeXMLDeclaration = true;
        conf.useCategoryAsDocumentPrefix = false;

        return conf;
    }
}