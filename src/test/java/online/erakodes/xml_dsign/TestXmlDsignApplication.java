package online.erakodes.xml_dsign;

import org.springframework.boot.SpringApplication;

public class TestXmlDsignApplication {

	public static void main(String[] args) {
		SpringApplication.from(XMLDigitalSignatureApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
