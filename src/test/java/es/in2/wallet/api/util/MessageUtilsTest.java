package es.in2.wallet.api.util;

import es.in2.wallet.domain.util.MessageUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.in2.wallet.domain.util.ApplicationRegexPattern.DOME_REDIRECT_URI_PATTERN;
import static org.junit.jupiter.api.Assertions.*;

class MessageUtilsTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<MessageUtils> constructor = MessageUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor is not private");
        constructor.setAccessible(true); // make the constructor accessible
        assertThrows(InvocationTargetException.class, constructor::newInstance, "Constructor invocation should throw IllegalStateException");
    }

    @Test
    void testConstantValues() {
        assertEquals("ProcessId: {}, Resource updated successfully.", MessageUtils.RESOURCE_UPDATED_MESSAGE);
        assertEquals("Error while updating resource: {}", MessageUtils.ERROR_UPDATING_RESOURCE_MESSAGE);

        // Add assertions for other constants as well
    }

    @Test
    void testDomeRedirectUriPattern() {
        // Test the DOME_REDIRECT_URI_PATTERN
        Pattern domePattern = DOME_REDIRECT_URI_PATTERN;
        Matcher matcher = domePattern.matcher("https://example.dome-marketplace.org/path");
        assertTrue(matcher.find(), "URL should match DOME_REDIRECT_URI_PATTERN");

        assertTrue(domePattern.matcher("https://another.dome-marketplace.org/other").find(),
                "Another URL should match DOME_REDIRECT_URI_PATTERN");
    }
}
