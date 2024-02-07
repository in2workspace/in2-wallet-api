package es.in2.wallet.vault.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultSecretData(
        @JsonProperty("privateKey") String privateKey,
        @JsonProperty("publicKey") String publicKey
) {
}