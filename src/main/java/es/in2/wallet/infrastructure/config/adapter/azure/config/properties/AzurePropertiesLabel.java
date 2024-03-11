package es.in2.wallet.infrastructure.config.adapter.azure.config.properties;


import es.in2.wallet.infrastructure.config.model.ConfigProviderName;
import es.in2.wallet.infrastructure.config.util.ConfigSourceNameAnnotation;

@ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
public record AzurePropertiesLabel(String global) {
}
