package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.helper.ISOMessageHandler;
import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.model.TransactionStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class TransactionStatusHandler implements IMessageHandler<String, TransactionStatusResponse> {

    private final static Logger log = LoggerFactory.getLogger(TransactionStatusHandler.class);

    @Override
    public Result<TransactionStatusResponse> handle(String txId) {
        var message = ISOMessageHandler.formatPACS00200110(txId, txId, txId);

        log.info("[PACS.002.001.10] Message: {}", message);

        return null;
    }
}