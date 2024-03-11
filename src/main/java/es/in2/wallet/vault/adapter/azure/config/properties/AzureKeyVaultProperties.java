package es.in2.wallet.vault.adapter.azure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("azure.keyvault")
public record AzureKeyVaultProperties(String endpoint) {


}