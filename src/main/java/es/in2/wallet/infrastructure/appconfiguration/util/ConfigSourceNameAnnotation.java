package es.in2.wallet.infrastructure.appconfiguration.util;

import es.in2.wallet.infrastructure.appconfiguration.model.ConfigProviderName;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConfigSourceNameCondition.class)
public @interface ConfigSourceNameAnnotation {
    ConfigProviderName name();
}
