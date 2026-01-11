package online.erakodes.xmldsig.model;

import java.util.Map;
import java.util.Optional;

public interface Request {
    RequestMeta meta();

    Map<String, Object> payload();

    default <T> Optional<T> get(String key, Class<T> type) {
        var value = payload().get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }
}