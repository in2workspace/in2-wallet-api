package es.in2.wallet.infrastructure.vault.adapter.azure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("azure.key-vault")
public record AzureKeyVaultProperties(String endpoint) {


}
