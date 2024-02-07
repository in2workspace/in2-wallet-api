package es.in2.wallet.api.util;

import java.util.regex.Pattern;

public class MessageUtils {

    private MessageUtils() {
        throw new IllegalStateException("Utility class");
    }
    public static final String RESOURCE_UPDATED_MESSAGE = "ProcessId: {}, Resource updated successfully.";
    public static final String ERROR_UPDATING_RESOURCE_MESSAGE = "Error while updating resource: {}";
    public static final String ENTITY_PREFIX = "/urn:entities:userId:";
    public static final String ATTRIBUTES = "/attrs:";
    public static final String PROCESS_ID = "ProcessId";
    public static final String PRIVATE_KEY_TYPE = "privateKey";
    public static final String PUBLIC_KEY_TYPE = "publicKey";
    public static final String DID = "did";
    public static final long MSB = 0x80L;
    public static final long LSB = 0x7FL;
    public static final long MSBALL = 0xFFFFFF80L;

    public static final Pattern PROOF_DOCUMENT_PATTERN = Pattern.compile("proof");
    public static final Pattern VP_DOCUMENT_PATTERN = Pattern.compile("vp");
    public static final Pattern VC_DOCUMENT_PATTERN = Pattern.compile("vc");

}
