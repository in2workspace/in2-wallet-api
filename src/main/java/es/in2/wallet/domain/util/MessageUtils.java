package es.in2.wallet.domain.util;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class MessageUtils {

    private MessageUtils() {
        throw new IllegalStateException("Utility class");
    }
    public static final String RESOURCE_UPDATED_MESSAGE = "ProcessId: {}, Resource updated successfully.";
    public static final String ERROR_UPDATING_RESOURCE_MESSAGE = "Error while updating resource: {}";
}
