package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.model.Result;


public interface IMessageHandler<I, O> {
    Result<O> handle(I request);
}