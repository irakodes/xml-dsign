package online.erakodes.xmldsig.service;

import online.erakodes.xmldsig.model.Result;

import java.util.concurrent.CompletableFuture;

public interface IMessageHandler<I, O> {
    CompletableFuture<Result<O>> handle(I request);
}