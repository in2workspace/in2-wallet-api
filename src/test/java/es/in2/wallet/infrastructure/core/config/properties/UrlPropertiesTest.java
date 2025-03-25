package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidationWithValidValues() {
        UrlProperties urlProperties = new UrlProperties( "example.com", "/api");

        Set<ConstraintViolation<UrlProperties>> violations = validator.validate(urlProperties);

        assertTrue(violations.isEmpty(), "No validation errors expected");
    }


    @Test
    void shouldFailValidationWhenDomainIsNull() {
        UrlProperties urlProperties = new UrlProperties(null, "/api");

        Set<ConstraintViolation<UrlProperties>> violations = validator.validate(urlProperties);

        assertEquals(1, violations.size(), "Expected one validation error");
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("domain"));
    }


    @Test
    void shouldFailValidationWhenPathIsNull() {
        UrlProperties urlProperties = new UrlProperties("example.com", null);

        Set<ConstraintViolation<UrlProperties>> violations = validator.validate(urlProperties);

        assertEquals(1, violations.size(), "Expected one validation error");
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("path"));
    }
}
