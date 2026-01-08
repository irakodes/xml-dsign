package online.erakodes.xmldsig.web;

import lombok.RequiredArgsConstructor;
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
public class SignedXmlResponseAdvice implements ResponseBodyAdvice<SignedMxMessage> {

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

        // Return type is ResponseEntity<SignedMxMessage>
        if (ResponseEntity.class.isAssignableFrom(bodyType)) {
            var entityType = extractGenericTypeFromResponseEntity(returnType);

            if (entityType != null && SignedMxMessage.class.isAssignableFrom(entityType)) {
                log.debug("SignedXmlResponseAdvice supports ResponseEntity<SignedMxMessage> return type [{}]",
                        entityType.getSimpleName());
                return true;
            }
        }

        // TODO: Handle error case scenarios
        log.debug("SignedXmlResponseAdvice does NOT support return type: {}", bodyType.getSimpleName());
        return false;
    }

    @Override
    public @Nullable SignedMxMessage beforeBodyWrite(
            @Nullable SignedMxMessage body, @Nullable MethodParameter returnType,
            @Nullable MediaType selectedContentType, @Nullable Class<? extends HttpMessageConverter<?>>
                    selectedConverterType,
            @Nullable ServerHttpRequest request, @Nullable ServerHttpResponse response) {
        if (body == null) return null;

        log.info("XML Signing The Response Body {} bytes",
                 body.getSignedContent().getBytes(StandardCharsets.UTF_8));

        body = signingService.wrapAndSign(body);

        return null;
    }


    /**
     * Extract the generic type parameter from ResponseEntity<T>
     * For example, extracts Message from ResponseEntity<Message>
     */
    private Class<?> extractGenericTypeFromResponseEntity(MethodParameter returnType) {
        try {
            var genericType = returnType.getGenericParameterType();

            if (genericType instanceof ParameterizedType paramType) {
                var typeArgs = paramType.getActualTypeArguments();

                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract generic type from ResponseEntity", e);
        }

        return null;
    }
}