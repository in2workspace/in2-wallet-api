package es.in2.wallet.infrastructure.appconfiguration.exception;

public class ConfigAdapterFactoryException extends RuntimeException {

    private static final String MESSAGE = "Error creating ConfigAdapterFactory. There should be only one ConfigAdapter. Found: ";

    public ConfigAdapterFactoryException(int size) {
        super(MESSAGE + size);
    }

}
