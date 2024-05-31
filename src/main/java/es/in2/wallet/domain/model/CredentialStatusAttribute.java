package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialStatusAttribute (
        @JsonProperty("type") String type,
        @JsonProperty("value") CredentialStatus credentialStatus

){
}
