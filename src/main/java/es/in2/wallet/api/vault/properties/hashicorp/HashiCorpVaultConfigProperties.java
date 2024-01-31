package es.in2.wallet.api.vault.properties.hashicorp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.vault")
public record HashiCorpVaultConfigProperties(String authentication, String token, String scheme,
                                       String host, int port) {
}
