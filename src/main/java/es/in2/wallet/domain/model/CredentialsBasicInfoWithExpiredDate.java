package es.in2.wallet.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

import static es.in2.wallet.domain.util.MessageUtils.*;

@Builder
public record CredentialsBasicInfoWithExpiredDate (
        @JsonProperty("id") String id,
        @JsonProperty("vcType") List<String> vcType,
        @JsonProperty(CREDENTIAL_SUBJECT) JsonNode credentialSubject,
        @JsonProperty(EXPIRATION_DATE)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_DATE_PATTERN)
        ZonedDateTime expirationDate
) {
}
