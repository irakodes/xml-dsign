package online.erakodes.xml_dsign.service;

import online.erakodes.xml_dsign.model.PaymentDto;
import online.erakodes.xml_dsign.model.Result;
import online.erakodes.xml_dsign.model.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PaymentMessageHandler implements IMessageHandler<PaymentDto, TransactionResponse> {

    private final static Logger log = LoggerFactory.getLogger(PaymentMessageHandler.class);

    @Override
    public CompletableFuture<Result<TransactionResponse>> handle(PaymentDto request) {
        return null;
    }
}