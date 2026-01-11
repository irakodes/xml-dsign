package online.erakodes.xmldsig.exception;

import java.io.Serial;

public class SignatureBuilderException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SignatureBuilderException(String message) {
        super(message);
    }

    public SignatureBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureBuilderException(Throwable cause) {
        super(cause);
    }
}