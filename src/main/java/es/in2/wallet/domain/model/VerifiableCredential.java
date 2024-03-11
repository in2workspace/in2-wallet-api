package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.util.List;

@Builder
public record VerifiableCredential(
        @JsonProperty("type")
        List<String> type,
        @JsonProperty("@context")
        List<String> context,
        @JsonProperty("id")
        String id,
        @JsonProperty("issuer") JsonNode issuer,
        @JsonProperty("issuanceDate") String issuanceDate,
        @JsonProperty("issued") String issued,
        @JsonProperty("validFrom") String validFrom,
        @JsonProperty("expirationDate") String expirationDate,
        @JsonProperty("credentialSubject") JsonNode credentialSubject
) {
}
