package es.in2.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import es.in2.wallet.domain.enums.CredentialStatus;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialStatusAttribute (
        @JsonProperty("type") String type,
        @JsonProperty("value") CredentialStatus credentialStatus

){
}
