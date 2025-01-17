package es.in2.wallet.domain.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.application.dto.*;
import es.in2.wallet.domain.enums.CredentialStatus;
import es.in2.wallet.domain.exceptions.FailedDeserializingException;
import es.in2.wallet.domain.exceptions.NoSuchVerifiableCredentialException;
import es.in2.wallet.domain.exceptions.ParseErrorException;
import es.in2.wallet.domain.services.DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.in2.wallet.domain.utils.ApplicationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final ObjectMapper objectMapper;

    /**
     * Retrieves the user's Verifiable Credentials in JSON format from a list of credential JSON strings.
     * This method processes each JSON string to extract credential data and constructs a list of basic credential info.
     *
     * @param credentialsJson The JSON list of credentials as a string.
     * @return A list of basic credential info extracted from the provided JSON strings.
     */
    @Override
    public Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String credentialsJson) {
        try {
            // Deserialize the JSON into a list of CredentialEntity objects
            List<CredentialEntity> credentials = objectMapper.readValue(credentialsJson, new TypeReference<>() {});

            // Check if the list is empty and throw an exception if it is
            if (credentials.isEmpty()) {
                log.error("Credential list is empty");
                return Mono.error(new NoSuchVerifiableCredentialException("The credentials list is empty. Cannot proceed."));
            }

            List<CredentialsBasicInfo> credentialsInfo = new ArrayList<>();
            for (CredentialEntity credential : credentials) {
                List<String> availableFormats = new ArrayList<>();
                availableFormats.add(JSON_VC);
                if (credential.jwtCredentialAttribute() != null) {
                    availableFormats.add(JWT_VC);
                }
                if (credential.cwtCredentialAttribute() != null) {
                    availableFormats.add(CWT_VC);
                }

                JsonNode jsonCredential = objectMapper.convertValue(credential.jsonCredentialAttribute().value(), JsonNode.class);
                JsonNode credentialSubject = jsonCredential.get(CREDENTIAL_SUBJECT);
                ZonedDateTime validUntil = null;

                if (jsonCredential.has(VALID_UNTIL) && !jsonCredential.get(VALID_UNTIL).isNull()) {
                    validUntil = parseZonedDateTime(jsonCredential.get(VALID_UNTIL).asText());
                }

                CredentialsBasicInfo info = CredentialsBasicInfo.builder()
                        .id(credential.id())
                        .vcType(credential.credentialTypeAttribute().value())
                        .credentialStatus(credential.credentialStatusAttribute().credentialStatus())
                        .availableFormats(availableFormats)
                        .credentialSubject(credentialSubject)
                        .validUntil(validUntil)
                        .build();

                credentialsInfo.add(info);
            }

            return Mono.just(credentialsInfo);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing Credential list: ", e);
            return Mono.error(new RuntimeException("Error processing credentials JSON: " + e));
        }
    }





    /**
     * This method parses a date-time string into a ZonedDateTime object using a custom DateTimeFormatter.
     * The formatter is built with Locale.US to ensure consistency in parsing text-based elements of the date-time,
     * such as month names and AM/PM markers, according to US conventions. This choice does not imply the date-time
     * is in a US timezone; rather, it ensures that the parsing behavior is consistent and matches the expected format,
     * particularly for applications used across different locales. The input date-time format expected is 'yyyy-MM-dd HH:mm:ss'
     * followed by optional nanoseconds and a timezone offset (e.g., '+0000' or equivalent UTC notation). The method
     * handles date-time strings in an international format (ISO 8601) with high precision and includes provisions
     * for parsing the timezone correctly. The use of Locale.US here is a standard practice for avoiding locale-specific
     * variations in date-time parsing and formatting, ensuring that the application behaves consistently in diverse
     * execution environments.
     */

    private ZonedDateTime parseZonedDateTime(String dateString) {
        // Pattern for "2025-04-02 09:23:22.637345122 +0000 UTC"
        Pattern pattern1 = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}\\.\\d+) \\+0000 UTC");
        Matcher matcher1 = pattern1.matcher(dateString);
        if (matcher1.matches()) {
            String normalized = matcher1.group(1) + "T" + matcher1.group(2) + "Z"; // Convert to "2025-04-02T09:23:22.637345122Z"
            return ZonedDateTime.parse(normalized, DateTimeFormatter.ISO_DATE_TIME);
        }

        // Pattern for ISO-8601 directly parseable formats like "2024-04-21T09:29:30Z"
        try {
            return ZonedDateTime.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString, e);
        }
    }

    /**
     * Retrieves the specified format of a Verifiable Credential from a given CredentialEntity.
     * Throws an error if the requested format is not available or not supported (only jwt_vc and cwt_vc are supported).
     *
     * @param format The format of the Verifiable Credential to retrieve (e.g., "jwt_vc", "cwt_vc").
     * @return A Mono<String> containing the credential in the requested format or an error if the format is not available.
     */
    @Override
    public Mono<String> getVerifiableCredentialOnRequestedFormat(String credentialEntityJson, String format) {
        return Mono.just(credentialEntityJson)
                .flatMap(credentialJson -> {
                    // Deserializing the JSON string to a CredentialEntity object
                    try {
                        CredentialEntity credential = objectMapper.readValue(credentialJson, CredentialEntity.class);
                        // Extracting the credential attribute based on the requested format
                        CredentialAttribute credentialAttribute;
                        switch (format) {
                            case JSON_VC:
                                credentialAttribute = credential.jsonCredentialAttribute();
                                break;
                            case JWT_VC:
                                credentialAttribute = credential.jwtCredentialAttribute();
                                break;
                            case CWT_VC:
                                credentialAttribute = credential.cwtCredentialAttribute();
                                break;
                            default:
                                // If the format is not supported, return an error Mono
                                return Mono.error(new IllegalArgumentException("Unsupported credential format requested: " + format));
                        }

                        // Check if the format attribute is available
                        if (credentialAttribute == null || credentialAttribute.value() == null) {
                            return Mono.error(new NoSuchElementException("Credential format not found or is null: " + format));
                        }

                        Object value = credentialAttribute.value();
                        if (value instanceof String stringValue) {
                            return Mono.just(stringValue);
                        } else {
                            String jsonValue = objectMapper.writeValueAsString(value);
                            return Mono.just(jsonValue);
                        }
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Error deserializing CredentialEntity from JSON", e));
                    }
                });
    }


    /**
     * Extracts the DID from a Verifiable Credential based on its type.
     * If the credential type includes "LEARCredentialEmployee", the DID is extracted from a nested structure within credentialSubject.
     * Otherwise, the DID is extracted directly from the credentialSubject.
     *
     * @param credentialJson The JSON string representing a CredentialEntity.
     * @return A Mono<String> containing the DID or an error if the DID cannot be found.
     */
    @Override
    public Mono<String> extractDidFromVerifiableCredential(String credentialJson) {
        return Mono.fromCallable(() -> {
            // Deserialize the credential JSON into a CredentialEntity object
            CredentialEntity credential = objectMapper.readValue(credentialJson, CredentialEntity.class);

            // Check if any of the credential types include "LEARCredentialEmployee"
            boolean isLearCredentialEmployee = credential.credentialTypeAttribute().value().stream()
                    .anyMatch("LEARCredentialEmployee"::equals);

            JsonNode vcNode = objectMapper.convertValue(credential.jsonCredentialAttribute().value(), JsonNode.class);
            JsonNode didNode;

            if (isLearCredentialEmployee) {
                // For LEARCredentialEmployee, the DID is located under credentialSubject.mandate.mandatee.id
                didNode = vcNode.path(CREDENTIAL_SUBJECT).path("mandate").path("mandatee").path("id");
            } else {
                // For other types, the DID is directly under credentialSubject.id
                didNode = vcNode.path(CREDENTIAL_SUBJECT).path("id");
            }

            if (didNode.isMissingNode() || didNode.asText().isEmpty()) {
                throw new NoSuchVerifiableCredentialException("DID not found in VC");
            }

            return didNode.asText();
        }).onErrorMap(JsonProcessingException.class, e -> new RuntimeException("Error processing VC JSON", e));
    }
}
