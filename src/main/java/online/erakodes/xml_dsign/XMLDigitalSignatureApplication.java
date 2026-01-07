package online.erakodes.xml_dsign;

import lombok.RequiredArgsConstructor;
import online.erakodes.xml_dsign.model.AccountLookupDto;
import online.erakodes.xml_dsign.model.AccountLookupResponse;
import online.erakodes.xml_dsign.model.AccountOpeningDto;
import online.erakodes.xml_dsign.model.AccountOpeningResponse;
import online.erakodes.xml_dsign.model.Result;
import online.erakodes.xml_dsign.service.IMessageHandler;
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

import java.util.concurrent.CompletableFuture;

@RestController
@SpringBootApplication
@RequiredArgsConstructor
public class XMLDigitalSignatureApplication {

	private final static Logger log = LoggerFactory
			.getLogger(XMLDigitalSignatureApplication.class);

	private final IMessageHandler<AccountLookupDto, AccountLookupResponse> accountLookupHandler;
	private final IMessageHandler<AccountOpeningDto, AccountOpeningResponse> accountOpeningHandler;

	public static void main(String[] args) {
		SpringApplication.run(XMLDigitalSignatureApplication.class, args);
	}

	@GetMapping("/accounts/{id}")
	public ResponseEntity<CompletableFuture<Result<AccountLookupResponse>>> lookupAccount(@PathVariable String id) {
		log.info("Handling account lookup with ID {}", id);
		var response = accountLookupHandler
				.handle(new AccountLookupDto(id, "", ""));

		return ResponseEntity.ok(response);
	}

	@PostMapping("/accounts")
	public ResponseEntity<CompletableFuture<Result<AccountOpeningResponse>>> createAccount(@RequestBody AccountOpeningDto request) {
		log.info("Handling account creation for account ID: {}", request.accountId());

		if (request.withProxy() && request.proxy() == null) throw
				new IllegalArgumentException("An account proxy is required for this call");

		var response = accountOpeningHandler.handle(request);

		return ResponseEntity.ok(response);
	}

}
