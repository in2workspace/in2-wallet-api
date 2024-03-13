package es.in2.wallet.infrastructure.vault.util;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.Objects;

public class VaultProviderCondition implements Condition {
    public static final String PROVIDER = "provider";

    @Override
    public boolean matches(ConditionContext context, @Nullable AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String expectedImplementation = env.getProperty("vault.provider.name");

        if (expectedImplementation == null) {
            return false;
        }

        Map<String, Object> annotationAttributes = Objects.requireNonNull(metadata).getAnnotationAttributes(VaultProviderAnnotation.class.getName());
        if (annotationAttributes == null || annotationAttributes.get(PROVIDER) == null) {
            return false;
        }

        String actualImplementation = annotationAttributes.get(PROVIDER).toString();

        return expectedImplementation.equalsIgnoreCase(actualImplementation);


    }
}
