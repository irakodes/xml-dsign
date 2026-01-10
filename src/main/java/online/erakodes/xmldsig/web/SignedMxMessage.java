package online.erakodes.xmldsig.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

/**
 * Represents a signed ISO 20022 MX message for API transmission.
 * This class is immutable and designed for JSON serialization.
 */
@Value
@Builder
@Jacksonized
public class SignedMxMessage {

    /**
     * Unique message identifier (e.g., ISO 20022 MsgId)
     */
    @NotBlank(message = "Message ID is required")
    @JsonProperty("messageId")
    String messageId;

    /**
     * Message creation timestamp in ISO 8601 format
     */
    @NotNull(message = "Creation time is required")
    @JsonProperty("creationTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant creationTime;

    /**
     * Sender BIC code (Business Identifier Code)
     */
    @NotBlank(message = "Sender is required")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "Invalid BIC format")
    @JsonProperty("senderBIC")
    String sender;

    /**
     * Receiver BIC code
     */
    @NotBlank(message = "Receiver is required")
    @Pattern(regexp = "^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$", message = "Invalid BIC format")
    @JsonProperty("receiverBIC")
    String receiver;

    /**
     * ISO 20022 message type (e.g., pacs.008.001.08)
     */
    @NotBlank(message = "Message type is required")
    @JsonProperty("messageType")
    String messageType;

    /**
     * The complete signed XML message content (base64 encoded for safe JSON transport)
     */
    @NotBlank(message = "Message content is required")
    @JsonProperty("signedContent")
    String signedContent;

    /**
     * Signature metadata
     */
    @NotNull(message = "Signature info is required")
    @JsonProperty("signatureInfo")
    SignatureInfo signatureInfo;

    /**
     * Message status (SIGNED, VERIFIED, SENT, etc.)
     */
    @JsonProperty("status")
    MessageStatus status;

    /**
     * Nested class for signature information
     */
    @Value
    @Builder
    @Jacksonized
    public static class SignatureInfo {
        @JsonProperty("algorithm")
        String algorithm;

        @JsonProperty("signedAt")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant signedAt;

        @JsonProperty("signerCertificateThumbprint")
        String signerCertificateThumbprint;

        @JsonProperty("signatureValid")
        Boolean signatureValid;

        /**
         * Represents a collection of digests associated with the signature.
         * Each digest provides specific details about a particular algorithm or data verification.
         */
        @JsonProperty("digests")
        List<DigestInfo> digests;
    }

    @Value
    @Builder
    @Jacksonized
    public static class DigestInfo {

        /**
         * Name/identifier of the signed entity (e.g., "KeyInfo", "Document", "AppHdr")
         */
        @JsonProperty("entityName")
        String entityName;

        /**
         * The digest value in hexadecimal format.
         */
        @JsonProperty("digestValue")
        String digestValue;

        @JsonProperty("algorithm")
        String algorithm;

        @JsonProperty("referenceUri")
        String referenceUri;
    }

    /**
     * Enum for message status
     */
    public enum MessageStatus {
        CREATED,
        SIGNED,
        VERIFIED,
        SENT,
        FAILED,
        REJECTED
    }
}