package es.in2.wallet.infrastructure.vault.adapter.azure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("security.vault.azure")
public record AzureKeyVaultProperties(String endpoint) {


}
