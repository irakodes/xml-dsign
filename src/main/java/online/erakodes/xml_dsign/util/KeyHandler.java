package online.erakodes.xml_dsign.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class KeyHandler {
    private static final Logger log = LoggerFactory.getLogger(KeyHandler.class);

    // TODO: 2021/03/22
    // Key store in classpath /keys
    private static final String KEY_STORE_PATH = "keys/keystore.jks";

    private static final String KEY_STORE_TYPE = "JKS";

    private static final char[] KEYSTORE_PASSWORD =
            System.getenv().getOrDefault("KEYSTORE_PASSWORD", "<PASSWORD>").toCharArray();

    public static final KeyStore KEY_STORE;

    static {
        try {
            KEY_STORE = KeyStore.getInstance(KEY_STORE_TYPE);
            try (var is = KeyHandler.class
                    .getClassLoader()
                    .getResourceAsStream(KEY_STORE_PATH)) {
                if (is == null) {
                    throw new IllegalStateException("Keystore not found");
                }

                KEY_STORE.load(is, KEYSTORE_PASSWORD);
            } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private KeyHandler() {
        // Private constructor
        // Prevent instantiation of this class
        // Use static methods

        // TODO: 2021/03/22
        // Load keystore from classpath
    }

    public static KeyStore getKeyStore() {
        return KEY_STORE;
    }

    public static PrivateKey getPrivateKey(String keyPass) {
        try {
            var ks = KeyStore.getInstance(KEY_STORE_TYPE);
            ks.load(new FileInputStream(KEY_STORE_PATH), keyPass.toCharArray());

            // Log all Key Aliases
            ks.aliases().asIterator().forEachRemaining(System.out::println);

            var key = ks.getKey("ekash prod", keyPass.toCharArray());
            log.info("KeyStore Key Algorithm fetched: {}", key.getAlgorithm());

            return (PrivateKey) key;
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | IOException | CertificateException |
                 KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Certificate getCertificate(String keyPass) {
        try {
            var ks = KeyStore.getInstance("JKS");
            ks.load(new java.io.FileInputStream(KEY_STORE_PATH), keyPass.toCharArray());

            var certificate = ks.getCertificate("ekash prod");
            log.info("KeyStore certificate fetched: {} ", certificate.getType());

            return (X509Certificate) certificate;
        } catch (NoSuchAlgorithmException | IOException | CertificateException |
                 KeyStoreException e) {
            log.error("Error while fetching certificate from keystore: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}