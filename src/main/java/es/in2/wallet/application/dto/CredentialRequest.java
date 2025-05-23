package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record CredentialRequest(
        @JsonProperty("types") List<String> types,

        @JsonProperty("format") String format,

        @JsonProperty("proofs") Proofs proof
) {
    @Builder
    public record Proofs(
            @JsonProperty("proof_type") String proofType,

            @JsonProperty("jwt") List<String> jwt
    ) {
    }
}
