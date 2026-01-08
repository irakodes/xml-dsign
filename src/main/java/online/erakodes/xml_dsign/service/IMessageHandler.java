package online.erakodes.xml_dsign.service;

import online.erakodes.xml_dsign.model.Result;

import java.util.concurrent.CompletableFuture;

public interface IMessageHandler<I, O> {
    CompletableFuture<Result<O>> handle(I request);
}