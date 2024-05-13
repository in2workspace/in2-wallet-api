package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.*;


@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialsBasicInfo(
        @JsonProperty("id") String id,
        @JsonProperty("type") List<String> vcType,
        @JsonProperty("status") CredentialStatus credentialStatus,
        @JsonProperty(AVAILABLE_FORMATS) List<String> availableFormats,
        @JsonProperty(CREDENTIAL_SUBJECT) JsonNode credentialSubject,
        @JsonProperty(EXPIRATION_DATE)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_DATE_PATTERN)
        ZonedDateTime expirationDate
) {
}
