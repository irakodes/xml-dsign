package online.erakodes.xmldsig.web;

import lombok.RequiredArgsConstructor;
import online.erakodes.xmldsig.model.Response;
import online.erakodes.xmldsig.model.Result;
import online.erakodes.xmldsig.service.BodySigningService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;

@RestControllerAdvice
@RequiredArgsConstructor
public class SignedXmlResponseAdvice implements ResponseBodyAdvice<Object> {

    private final BodySigningService signingService;

    private final static Logger log = LoggerFactory.getLogger(SignedXmlResponseAdvice.class);

    @Override
    public boolean supports
            (MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        var bodyType = returnType.getParameterType();
        log.debug("Body type: {}", bodyType.getName());

        // Return type is SignedMxMessage
        if (SignedMxMessage.class.isAssignableFrom(bodyType)) {
            log.debug("SignedXmlResponseAdvice supports message return type [{}]",
                    bodyType.getSimpleName());
            return true;
        }

        // Return type is Result<T>
        if (Result.class.isAssignableFrom(bodyType)) {
            log.debug("SignedXmlResponseAdvice supports Result return type [{}]",
                    bodyType.getSimpleName());
            return true;
        }

        // Return type is ResponseEntity<...>
        if (ResponseEntity.class.isAssignableFrom(bodyType)) {
            var entityType = extractGenericTypeFromResponseEntity(returnType);

            if (entityType != null) {
                // ResponseEntity<SignedMxMessage>
                if (SignedMxMessage.class.isAssignableFrom(entityType)) {
                    log.debug("SignedXmlResponseAdvice supports ResponseEntity<SignedMxMessage> return type [{}]",
                            entityType.getSimpleName());
                    return true;
                }

                // ResponseEntity<Result<T>>
                if (Result.class.isAssignableFrom(entityType)) {
                    log.debug("SignedXmlResponseAdvice supports ResponseEntity<Result<T>> return type [{}]",
                            entityType.getSimpleName());
                    return true;
                }
            }
        }

        log.debug("SignedXmlResponseAdvice does NOT support return type: {}", bodyType.getSimpleName());
        return false;
    }

    @Override
    public @Nullable Object beforeBodyWrite(
            @Nullable Object body, @Nullable MethodParameter returnType,
            @Nullable MediaType selectedContentType, @Nullable Class<? extends HttpMessageConverter<?>>
                    selectedConverterType,
            @Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response) {
        if (body == null) {
            log.debug("Response body is null, skipping signing");
            return null;
        }

        log.debug("Processing response body of type: {}", body.getClass().getName());

        // Handle direct SignedMxMessage
        if (body instanceof SignedMxMessage signedMessage) {
            log.info("XML Signing The Response Body {} bytes",
                    signedMessage.getSignedContent().getBytes(StandardCharsets.UTF_8).length);
            return signingService.wrapAndSign(signedMessage);
        }

        // Handle Result<T> - convert to Response<T> using ResponseMapper
        if (body instanceof Result<?> result) {
            return handleResultType(result);
        }

        log.debug("Response body does not contain a SignedMxMessage or Result (type: {}), passing through",
                body.getClass().getName());
        return body;
    }

    /**
     * Handles Result<T> types by converting them to Response<T> using ResponseMapper.
     * For Result.Ok containing SignedMxMessage, the content is signed before mapping.
     * For Result.Fail, the error is mapped to Response directly.
     *
     * @param result The Result to process
     * @return Response<T> mapped from the Result
     */
    @SuppressWarnings("unchecked")
    private <T> Response<T> handleResultType(Result<?> result) {
        // Handle success case: Result.Ok
        if (result instanceof Result.Ok<?> ok) {
            // If data is SignedMxMessage, sign it first
            if (ok.data() instanceof SignedMxMessage signed) {
                log.info("XML Signing The Response Body from Result.Ok<SignedMxMessage> {} bytes",
                        signed.getSignedContent().getBytes(StandardCharsets.UTF_8).length);

                var signedMessage = signingService.wrapAndSign(signed);
                var signedResult = new Result.Ok<>(signedMessage, ok.details());

                log.debug("Converting signed Result.Ok to Response");
                return (Response<T>) ResponseMapper.toHttpResponse(signedResult);
            }

            // For non-SignedMxMessage data, convert directly
            log.debug("Converting Result.Ok to Response");
            return ResponseMapper.toHttpResponse((Result<T>) result);
        }

        // Handle failure case: Result.Fail
        if (result instanceof Result.Fail<?> fail) {
            log.debug("Converting Result.Fail to Response with error code: {}",
                    fail.error() != null ? fail.error().getErrorCode() : "unknown");
            return ResponseMapper.toHttpResponse((Result<T>) result);
        }

        // Fallback - should not reach here due to sealed interface
        log.warn("Unknown Result type encountered: {}", result.getClass().getName());
        return ResponseMapper.toHttpResponse((Result<T>) result);
    }


    /**
     * Extract the generic type parameter from ResponseEntity<T>
     * For example, extracts Message from ResponseEntity<Message>
     */
    private Class<?> extractGenericTypeFromResponseEntity(MethodParameter returnType) {
        try {
            var isReturnType = returnType.getParameterIndex() == -1;
            log.debug("Is return type: {}, parameter index: {}", isReturnType, returnType.getParameterIndex());

            // For return types, we need to get the generic return type differently
            // Try getNestedGenericParameterType() first
            var genericType = returnType.getNestedGenericParameterType();
            log.debug("Nested generic parameter type: {}", genericType);

            // If that doesn't work for return types, try getting it from the method's return type
            if (genericType == Object.class) {
                if (isReturnType && returnType.getMethod() != null) {
                    var methodReturnType = returnType.getMethod().getGenericReturnType();
                    log.info("Method generic return type: {}", methodReturnType);
                    genericType = methodReturnType;
                }
            }

            if (genericType instanceof ParameterizedType paramType) {
                var typeArgs = paramType.getActualTypeArguments();
                log.info("Type arguments count: {}", typeArgs.length);

                if (typeArgs.length > 0) {
                    var firstArg = typeArgs[0];
                    log.info("First type argument: {} (type: {})", firstArg, firstArg.getClass().getName());

                    // Direct case: ResponseEntity<SignedMxMessage>
                    if (firstArg instanceof Class<?> clazz) {
                        log.info("Direct class type: {}", clazz);
                        return clazz;
                    }

                    // Nested case: ResponseEntity<Result<SignedMxMessage>>
                    if (firstArg instanceof ParameterizedType nestedType) {
                        log.info("Nested ParameterizedType found: {}", nestedType);
                        var nestedArgs = nestedType.getActualTypeArguments();
                        log.info("Nested type arguments count: {}", nestedArgs.length);
                        if (nestedArgs.length > 0 && nestedArgs[0] instanceof Class<?> nestedClass) {
                            log.info("Extracted nested type: {}", nestedClass);
                            return nestedClass;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract generic type from ResponseEntity", e);
        }

        return null;
    }
}