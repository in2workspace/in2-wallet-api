package es.in2.wallet.api.util;
public class MessageUtils {

    private MessageUtils() {
        throw new IllegalStateException("Utility class");
    }
    public static final String RESOURCE_UPDATED_MESSAGE = "ProcessId: {}, Resource updated successfully.";
    public static final String ERROR_UPDATING_RESOURCE_MESSAGE = "Error while updating resource: {}";
    public static final String ENTITY_PREFIX = "/urn:entities:userId:";
    public static final String ATTRIBUTES = "/attrs:";
    public static final String PROCESS_ID = "ProcessId";
    public static final long MSB = 0x80L;
    public static final long LSB = 0x7FL;
    public static final long MSBALL = 0xFFFFFF80L;

}
