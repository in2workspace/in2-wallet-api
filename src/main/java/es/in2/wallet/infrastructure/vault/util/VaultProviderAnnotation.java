package es.in2.wallet.infrastructure.vault.util;

import es.in2.wallet.infrastructure.vault.model.VaultProviderEnum;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Conditional(VaultProviderCondition.class)
public @interface VaultProviderAnnotation {
    VaultProviderEnum provider();
}

