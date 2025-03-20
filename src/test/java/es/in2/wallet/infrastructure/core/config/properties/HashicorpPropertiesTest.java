package es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.properties;

import es.in2.wallet.infrastructure.vault.adapter.hashicorp.config.properties.HashicorpProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashicorpPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidationWithValidValues() {
        HashicorpProperties hashicorpProperties = new HashicorpProperties("vault.example.com", "8200", "https", "my-secret-token");

        Set<ConstraintViolation<HashicorpProperties>> violations = validator.validate(hashicorpProperties);

        assertTrue(violations.isEmpty(), "No validation errors expected");
    }

    @Test
    void shouldFailValidationWhenHostIsNull() {
        HashicorpProperties hashicorpProperties = new HashicorpProperties(null, "8200", "https", "my-secret-token");

        Set<ConstraintViolation<HashicorpProperties>> violations = validator.validate(hashicorpProperties);

        assertEquals(1, violations.size(), "Expected one validation error");
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("host"));
    }

    @Test
    void shouldFailValidationWhenPortIsNull() {
        HashicorpProperties hashicorpProperties = new HashicorpProperties("vault.example.com", null, "https", "my-secret-token");

        Set<ConstraintViolation<HashicorpProperties>> violations = validator.validate(hashicorpProperties);

        assertEquals(1, violations.size(), "Expected one validation error");
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("port"));
    }

    @Test
    void shouldFailValidationWhenSchemeIsNull() {
        HashicorpProperties hashicorpProperties = new HashicorpProperties("vault.example.com", "8200", null, "my-secret-token");

        Set<ConstraintViolation<HashicorpProperties>> violations = validator.validate(hashicorpProperties);

        assertEquals(1, violations.size(), "Expected one validation error");
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("scheme"));
    }

    @Test
    void shouldFailValidationWhenTokenIsNull() {
        HashicorpProperties hashicorpProperties = new HashicorpProperties("vault.example.com", "8200", "https", null);

        Set<ConstraintViolation<HashicorpProperties>> violations = validator.validate(hashicorpProperties);

        assertEquals(1, violations.size(), "Expected one validation error");
        assertTrue(violations.iterator().next().getPropertyPath().toString().contains("token"));
    }
}

