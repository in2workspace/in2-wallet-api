package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        @JsonIgnore @JsonProperty("issuanceDate") String issuanceDate, // Old Credential version attribute
        @JsonProperty("issued") String issued,
        @JsonProperty("validFrom") String validFrom,
        @JsonIgnore @JsonProperty("expirationDate") String expirationDate, // Old Credential version attribute
        @JsonProperty("validUntil") String validUntil, // New Credential version attribute
        @JsonProperty("credentialSubject") JsonNode credentialSubject
) {
}
