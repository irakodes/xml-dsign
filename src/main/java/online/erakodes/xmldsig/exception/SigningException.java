package online.erakodes.xmldsig.exception;

import java.io.Serial;

public class SigningException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SigningException(String message) {
        super(message);
    }

    public SigningException(Throwable cause) {
        super(cause);
    }

    public SigningException(String message, Throwable cause) {
        super(message, cause);
    }

}
