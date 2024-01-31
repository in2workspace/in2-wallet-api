package es.in2.wallet.api.vault.properties.azure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.azure.appconfiguration")
public record AzureVaultConfigProperties(String endpoint) {
}
