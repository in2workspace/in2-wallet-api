package es.in2.wallet.infrastructure.appconfiguration.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConfigSourceNameConditionTest {

    private ConfigSourceNameCondition condition;

    @Mock
    private ConditionContext context;

    @Mock
    private Environment environment;

    @Mock
    private AnnotatedTypeMetadata metadata;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        condition = new ConfigSourceNameCondition();
        when(context.getEnvironment()).thenReturn(environment);
    }

    @Test
    void shouldReturnFalseWhenPropertyIsNotSet() {
        when(environment.getProperty("app.config-source.name")).thenReturn(null);

        boolean result = condition.matches(context, metadata);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenAnnotationAttributesAreNull() {
        when(environment.getProperty("app.config-source.name")).thenReturn("expectedValue");
        when(metadata.getAnnotationAttributes(ConfigSourceNameAnnotation.class.getName())).thenReturn(null);

        boolean result = condition.matches(context, metadata);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenAnnotationNameAttributeIsMissing() {
        when(environment.getProperty("app.config-source.name")).thenReturn("expectedValue");
        when(metadata.getAnnotationAttributes(ConfigSourceNameAnnotation.class.getName()))
                .thenReturn(Collections.emptyMap());

        boolean result = condition.matches(context, metadata);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenValuesDoNotMatch() {
        when(environment.getProperty("app.config-source.name")).thenReturn("expectedValue");
        when(metadata.getAnnotationAttributes(ConfigSourceNameAnnotation.class.getName()))
                .thenReturn(Map.of("name", "differentValue"));

        boolean result = condition.matches(context, metadata);

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenValuesMatch() {
        when(environment.getProperty("app.config-source.name")).thenReturn("expectedValue");
        when(metadata.getAnnotationAttributes(ConfigSourceNameAnnotation.class.getName()))
                .thenReturn(Map.of("name", "expectedValue"));

        boolean result = condition.matches(context, metadata);

        assertTrue(result);
    }
}
