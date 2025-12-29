package online.erakodes.xml_dsign.model;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Map;

public record RequestMeta(String requestId, Instant timestamp, @Nullable String principal,
                          Map<String, String> headers) {

}