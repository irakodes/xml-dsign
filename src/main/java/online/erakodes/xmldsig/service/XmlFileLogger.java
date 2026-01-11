package online.erakodes.xmldsig.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Service for logging signed XML messages to the filesystem.
 * Files are organized by date in the format: logs/files/dd-MMM-yy/messageId.xml
 */
@Service
public class XmlFileLogger {

    private static final Logger log = LoggerFactory.getLogger(XmlFileLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yy");

    @Value("${xmldsig.log.base-path}")
    private String basePath;

    /**
     * Logs a signed XML message to a file organized by date.
     *
     * @param messageId the unique message identifier (used as filename)
     * @param signedXml the complete signed XML content to write
     * @throws IOException if file writing fails
     */
    public Path logSignedXml(String messageId, String signedXml) throws IOException {
        try {
            var filePath = buildFilePath(messageId);
            writeXmlToFile(filePath, signedXml);
            log.info("[{}] XML successfully logged to {}.", messageId, filePath);

            return filePath;
        } catch (IOException e) {
            log.error("Failed to log signed XML for messageId: {}", messageId, e);
            throw e;
        }
    }

    /**
     * Writes the XML content to the specified file.
     *
     * @param filePath  the target file path
     * @param signedXml the XML content to write
     * @throws IOException if writing fails
     */
    private void writeXmlToFile(Path filePath, String signedXml) throws IOException {
        Files.writeString(
                filePath,
                signedXml,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Builds the file path based on current date and message ID.
     * Format: logs/files/dd-MMM-yy/messageId.xml
     *
     * @param messageId the message identifier
     * @return the complete file path
     * @throws IOException if directory creation fails
     */
    private Path buildFilePath(String messageId) throws IOException {
        var dateFolder = LocalDate.now().format(DATE_FORMATTER);
        var directoryPath = Paths.get(basePath, dateFolder);

        // Create directories if they don't exist
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
            log.debug("Created directory: {}", directoryPath);
        }

        // Sanitize messageId to ensure it's a valid file name
        var sanitizedMesageId = sanitizeFileName(messageId);
        return directoryPath.resolve(sanitizedMesageId + ".xml");
    }

    /**
     * Sanitizes a message ID to create a valid filename.
     * Removes or replaces characters that are invalid in filenames.
     *
     * @param messageId the original message ID
     * @return sanitized filename-safe string
     */
    private String sanitizeFileName(String messageId) {
        if (messageId == null || messageId.isBlank()) return "unknown_" + System.currentTimeMillis();
        return messageId.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}