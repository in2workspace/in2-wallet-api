package es.in2.wallet.configuration.adapter.azure.config.properties;


import es.in2.wallet.configuration.model.ConfigProviderName;
import es.in2.wallet.configuration.util.ConfigSourceNameAnnotation;

@ConfigSourceNameAnnotation(name = ConfigProviderName.AZURE)
public record AzurePropertiesLabel(String global) {
}
