package online.erakodes.xml_dsign.model;

public sealed interface Result<T> permits Result.Ok, Result.Fail {
    record Ok<T>(T data, Object details) implements Result<T> {
    }

    record Fail<T>(Error error) implements Result<T> {
    }
}