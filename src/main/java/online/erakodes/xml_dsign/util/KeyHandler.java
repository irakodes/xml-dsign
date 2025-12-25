package online.erakodes.xml_dsign.util;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class KeyHandler {

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
                if (is == null) { throw new IllegalStateException("Keystore not found"); }

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

    public static KeyStore getKeyStore() { return KEY_STORE; }
}