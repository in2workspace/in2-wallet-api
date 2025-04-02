package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthServerPropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldUseProvidedValuesWhenValidUrls() {
        String externalUrl = "https://auth.example.com";
        String internalUrl = "http://auth.internal";

        AuthServerProperties properties = new AuthServerProperties(externalUrl, internalUrl);

        Set<ConstraintViolation<AuthServerProperties>> violations = validator.validate(properties);

        assertTrue(violations.isEmpty(), "Expected no validation errors for valid URLs");
    }

    @Test
    void shouldFailValidationWhenExternalUrlIsInvalid() {
        String externalUrl = "invalid-url";
        String internalUrl = "http://auth.internal";

        AuthServerProperties properties = new AuthServerProperties(externalUrl, internalUrl);

        Set<ConstraintViolation<AuthServerProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty(), "Expected validation error for invalid externalUrl");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("externalUrl")));
    }

    @Test
    void shouldFailValidationWhenInternalUrlIsInvalid() {
        String externalUrl = "https://auth.example.com";
        String internalUrl = "not-a-url";

        AuthServerProperties properties = new AuthServerProperties(externalUrl, internalUrl);

        Set<ConstraintViolation<AuthServerProperties>> violations = validator.validate(properties);

        assertFalse(violations.isEmpty(), "Expected validation error for invalid internalUrl");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("internalUrl")));
    }
}
