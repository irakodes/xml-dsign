package online.erakodes.xml_dsign;

import lombok.RequiredArgsConstructor;
import online.erakodes.xml_dsign.model.AccountLookupDto;
import online.erakodes.xml_dsign.model.AccountLookupResponse;
import online.erakodes.xml_dsign.service.IMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
@RequiredArgsConstructor
public class XMLDigitalSignatureApplication {

	private final static Logger log = LoggerFactory
	.getLogger(XMLDigitalSignatureApplication.class);

	private final IMessageHandler<AccountLookupDto, AccountLookupResponse> handler;

	public static void main(String[] args) {
		SpringApplication.run(XMLDigitalSignatureApplication.class, args);
	}

	@GetMapping("/accounts/{id}")
	public ResponseEntity<?> lookupAccount(@PathVariable String id) {
		log.info("Handling account lookup with ID {}", id);
		var response = handler
		.handle(new AccountLookupDto(id, "", ""));

		return ResponseEntity.ok(response);
	}

}
