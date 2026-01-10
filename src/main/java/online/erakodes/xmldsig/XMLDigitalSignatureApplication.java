package online.erakodes.xmldsig;

import lombok.RequiredArgsConstructor;
import online.erakodes.xmldsig.model.*;
import online.erakodes.xmldsig.service.IMessageHandler;
import online.erakodes.xmldsig.web.SignedMxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@SpringBootApplication
@RequiredArgsConstructor
public class XMLDigitalSignatureApplication {

	private final static Logger log = LoggerFactory
			.getLogger(XMLDigitalSignatureApplication.class);

	private final IMessageHandler<AccountLookupDto, AccountLookupResponse> accountLookupHandler;
	private final IMessageHandler<AccountOpeningDto, AccountOpeningResponse> accountOpeningHandler;
	private final IMessageHandler<PaymentDto, TransactionResponse> paymentHandler;
	private final IMessageHandler<String, TransactionStatusResponse> txStatusHandler;

	private final IMessageHandler<Object, SignedMxMessage> mxHandler; //<-- This is the one that actually signs the message

	public static void main(String[] args) {
		SpringApplication.run(XMLDigitalSignatureApplication.class, args);
	}

	@GetMapping("/accounts/{id}")
	public ResponseEntity<Result<SignedMxMessage>>
	lookupAccount(@PathVariable String id) {
		log.info("Handling account lookup with ID {}", id);
		var response = mxHandler
				.handle(new AccountLookupDto(id, "", "SOMBKBIC"));

		return ResponseEntity.ok(response);
	}

	@PostMapping("/accounts")
	public ResponseEntity<?>
	createAccount(@RequestBody AccountOpeningDto request) {
		log.info("Handling account creation for account ID: {}", request.accountId());

		if (request.withProxy() && request.proxy() == null) throw
				new IllegalArgumentException("An account proxy is required for this call");

		var response = accountOpeningHandler.handle(request);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/transfers")
	public ResponseEntity<?> transferFunds(@RequestBody PaymentDto request) {
		log.info("Handling transfer funds request for account ID: {}", request.initiatorId());
		var response = paymentHandler.handle(request);

		return ResponseEntity.ok(response);
	}

	/**
	 * Only for the purpose of testing the status endpoint
	 ***/
	@GetMapping("/transfers/{transactionId}/status")
	public ResponseEntity<?> transferFunds(@PathVariable String transactionId) {
		log.info("Handling PACS.002.001.10 Transaction Status Call for {}", transactionId);
		var response = txStatusHandler.handle(transactionId);

		return ResponseEntity.ok(response);
	}

}
