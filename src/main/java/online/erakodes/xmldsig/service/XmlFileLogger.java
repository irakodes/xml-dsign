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


@Service
public class XmlFileLogger {

    private static final Logger log = LoggerFactory.getLogger(XmlFileLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yy");

    @Value("${xmldsig.log.base-path}")
    private String basePath;


    public void logSignedXml(String messageId, String signedXml) {
        try {
            var filePath = buildFilePath(messageId);
            writeXmlToFile(filePath, signedXml);
            log.info("[{}] XML successfully logged to {}.", messageId, filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeXmlToFile(Path filePath, String signedXml) throws IOException {
        Files.writeString(
                filePath,
                signedXml,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

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

    private String sanitizeFileName(String messageId) {
        if (messageId == null || messageId.isBlank()) return "unknown_" + System.currentTimeMillis();
        return messageId.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}