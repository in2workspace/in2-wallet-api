package es.in2.wallet.infrastructure.appconfiguration.util;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;

public class ConfigSourceNameCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, @Nullable AnnotatedTypeMetadata metadata) {

        String expectedImplementation = context.getEnvironment().getProperty("app.config-source.name");

        if (expectedImplementation == null) {
            return false;
        }

        Map<String, Object> annotationAttributes = Objects.requireNonNull(metadata).getAnnotationAttributes(ConfigSourceNameAnnotation.class.getName());

        if (annotationAttributes == null || annotationAttributes.get("name") == null) {
            return false;
        }


        String actualImplementation = annotationAttributes.get("name").toString();

        return expectedImplementation.equals(actualImplementation);
    }
}
