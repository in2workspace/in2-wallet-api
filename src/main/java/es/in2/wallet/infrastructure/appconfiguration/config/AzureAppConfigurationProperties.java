package es.in2.wallet.infrastructure.appconfiguration.config;

import es.in2.wallet.infrastructure.appconfiguration.model.ConfigProviderName;
import es.in2.wallet.infrastructure.appconfiguration.util.ConfigSourceNameAnnotation;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
@ConfigurationProperties(prefix = "azure.app")
@Validated
public record AzureAppConfigurationProperties(@NotNull(message = "Endpoint is mandatory") String endpoint,
                                              @NestedConfigurationProperty AzurePropertiesLabel label) {

    @ConstructorBinding
    public AzureAppConfigurationProperties(String endpoint, AzurePropertiesLabel label) {
        this.endpoint = endpoint;
        this.label = Optional.ofNullable(label).orElse(new AzurePropertiesLabel(null));
    }

    @ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
    public record AzurePropertiesLabel(String global) {

    }

}
