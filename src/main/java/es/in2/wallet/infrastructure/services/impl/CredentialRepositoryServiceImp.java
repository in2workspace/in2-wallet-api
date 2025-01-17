package es.in2.wallet.infrastructure.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.domain.entities.Credential;
import es.in2.wallet.domain.enums.CredentialFormats;
import es.in2.wallet.domain.enums.CredentialStatus;
import es.in2.wallet.domain.exceptions.NoSuchVerifiableCredentialException;
import es.in2.wallet.domain.exceptions.ParseErrorException;
import es.in2.wallet.infrastructure.repositories.CredentialRepository;
import es.in2.wallet.infrastructure.services.CredentialRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.minvws.encoding.Base45;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static es.in2.wallet.domain.utils.ApplicationConstants.CWT_VC;
import static es.in2.wallet.domain.utils.ApplicationConstants.JWT_VC;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialRepositoryServiceImp implements CredentialRepositoryService {
    private final CredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<UUID> saveCredential(String processId, UUID userId, CredentialResponse credentialResponse) {
        Timestamp currentTimestamp = Timestamp.from(Instant.now());

        // Basic null checks
        if (credentialResponse == null) {
            return Mono.error(new IllegalArgumentException("CredentialResponse is null"));
        }

        // If format is null, treat it as a plain (non-signed) credential
        if (credentialResponse.format() == null) {
            return parseAsPlainJson(credentialResponse.credential())
                    .flatMap(vcJson -> Mono.zip(
                            extractCredentialTypes(vcJson),
                            extractVerifiableCredentialIdFromVcJson(vcJson),
                            (types, credId) -> buildCredentialEntity(
                                    UUID.fromString(credId),
                                    userId,
                                    types,
                                    null,   // No credentialFormat
                                    null,   // No credentialData
                                    vcJson,
                                    CredentialStatus.ISSUED.getCode(),
                                    currentTimestamp
                            )
                    ))
                    .flatMap(credentialEntity ->
                            credentialRepository.save(credentialEntity)
                                    .doOnSuccess(savedCredential ->
                                            log.info("[Process ID: {}] Deferred credential with ID {} saved successfully.",
                                                    processId,
                                                    savedCredential.getCredentialId())
                                    )
                                    .thenReturn(credentialEntity.getCredentialId())
                    )
                    .onErrorResume(e -> {
                        log.error("[Process ID: {}] Error saving credential (no format): {}", processId, e.getMessage(), e);
                        return Mono.error(new RuntimeException("Error processing Verifiable Credential with no format", e));
                    });
        }

        // Otherwise, handle known formats (JWT_VC, CWT_VC)
        return extractCredentialFormat(credentialResponse)
                .flatMap(credentialFormat -> extractVcJson(credentialResponse)
                        .flatMap(vcJson -> Mono.zip(
                                extractCredentialTypes(vcJson),
                                extractVerifiableCredentialIdFromVcJson(vcJson),
                                (credentialTypes, credentialId) ->
                                        buildCredentialEntity(
                                                UUID.fromString(credentialId),
                                                userId,
                                                credentialTypes,
                                                credentialFormat,
                                                credentialResponse.credential(),  // store raw credential data
                                                vcJson,
                                                CredentialStatus.VALID.getCode(), // store as VALID code
                                                currentTimestamp
                                        )
                        ))
                        .flatMap(credentialEntity ->
                                credentialRepository.save(credentialEntity)
                                        .doOnSuccess(savedCredential ->
                                                log.info("[Process ID: {}] Credential with ID {} saved successfully.",
                                                        processId,
                                                        savedCredential.getCredentialId())
                                        )
                                        .thenReturn(credentialEntity.getCredentialId())
                        )
                )
                .onErrorResume(e -> {
                    log.error("[Process ID: {}] Error saving credential: {}", processId, e.getMessage(), e);
                    return Mono.error(new RuntimeException("Error processing Verifiable Credential", e));
                });
    }

    /**
     * Builds the Credential entity based on the provided parameters.
     */
    private Credential buildCredentialEntity(
            UUID credentialId,
            UUID userUuid,
            List<String> credentialTypes,
            Integer credentialFormat,
            String credentialData,
            JsonNode vcJson,
            int credentialStatus,
            Timestamp timestamp
    ) {
        return Credential.builder()
                .credentialId(credentialId)
                .userId(userUuid)
                .credentialType(credentialTypes)
                .credentialStatus(credentialStatus)  // store int code for status
                .credentialFormat(credentialFormat)  // store int code for format
                .credentialData(credentialData)
                .jsonVc(vcJson.toString())
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }

    /**
     * Helper method that parses a plain JSON string (non-signed format).
     */
    private Mono<JsonNode> parseAsPlainJson(String rawJson) {
        return Mono.fromCallable(() -> {
            if (rawJson == null || rawJson.isBlank()) {
                throw new ParseErrorException("Credential data is empty or null");
            }
            return objectMapper.readTree(rawJson);
        }).onErrorMap(e -> new ParseErrorException("Error parsing plain JSON credential: " + e.getMessage()));
    }

    /**
     * Extracts the credential format from a known set of formats (JWT_VC, CWT_VC).
     * If it's unsupported, raises an error.
     */
    private Mono<Integer> extractCredentialFormat(CredentialResponse credentialResponse) {
        if (credentialResponse.format() == null) {
            return Mono.error(new IllegalArgumentException("CredentialResponse format is null"));
        }
        return switch (credentialResponse.format()) {
            case JWT_VC -> Mono.just(CredentialFormats.JWT_VC.getCode());
            case CWT_VC -> Mono.just(CredentialFormats.CWT_VC.getCode());
            default -> Mono.error(new IllegalArgumentException(
                    "Unsupported credential format: " + credentialResponse.format()
            ));
        };
    }

    /**
     * Extracts the Verifiable Credential JSON based on the format.
     * If format is null, use parseAsPlainJson directly (handled in the main code).
     */
    private Mono<JsonNode> extractVcJson(CredentialResponse credentialResponse) {
        // We assume credentialResponse.format() is NOT null here
        return switch (credentialResponse.format()) {
            case JWT_VC -> extractVcJsonFromJwt(credentialResponse.credential());
            case CWT_VC -> extractVcJsonFromCwt(credentialResponse.credential());
            default -> Mono.error(new IllegalArgumentException(
                    "Unsupported credential format: " + credentialResponse.format()
            ));
        };
    }

    private Mono<JsonNode> extractVcJsonFromJwt(String jwtVc) {
        return Mono.fromCallable(() -> SignedJWT.parse(jwtVc))
                .flatMap(parsedJwt -> {
                    try {
                        JsonNode payload = objectMapper.readTree(parsedJwt.getPayload().toString());
                        JsonNode vcJson = payload.get("vc");
                        if (vcJson == null) {
                            return Mono.error(new ParseErrorException("VC JSON is missing in the payload"));
                        }
                        log.debug("Verifiable Credential JSON extracted from JWT: {}", vcJson);
                        return Mono.just(vcJson);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new ParseErrorException("Error while processing JWT payload: " + e.getMessage()));
                    }
                });
    }

    private Mono<JsonNode> extractVcJsonFromCwt(String cwtVc) {
        return Mono.fromCallable(() -> {
            String vpJson = decodeToJSONstring(cwtVc);
            JsonNode vpNode = objectMapper.readTree(vpJson);
            JsonNode vcCbor = vpNode.at("/vp/verifiableCredential");
            if (vcCbor == null || !vcCbor.isTextual()) {
                throw new ParseErrorException("Verifiable Credential is missing in the CWT");
            }
            String vcJson = decodeToJSONstring(vcCbor.asText());
            return objectMapper.readTree(vcJson);
        }).onErrorMap(e -> new ParseErrorException("Error processing CWT: " + e.getMessage()));
    }

    private Mono<String> extractVerifiableCredentialIdFromVcJson(JsonNode vcJson) {
        return Mono.defer(() -> {
            if (vcJson == null) {
                return Mono.error(new IllegalArgumentException("vcJson is null"));
            }
            JsonNode idNode = vcJson.get("id");
            if (idNode != null && idNode.isTextual()) {
                log.debug("Verifiable Credential ID extracted: {}", idNode.asText());
                return Mono.just(idNode.asText());
            }
            return Mono.error(new IllegalArgumentException("Verifiable Credential ID is missing"));
        });
    }

    private Mono<List<String>> extractCredentialTypes(JsonNode vcJson) {
        return Mono.defer(() -> {
            if (vcJson == null) {
                return Mono.error(new IllegalArgumentException("vcJson is null"));
            }
            JsonNode typesNode = vcJson.get("type");
            if (typesNode != null && typesNode.isArray()) {
                List<String> types = new ArrayList<>();
                typesNode.forEach(typeNode -> types.add(typeNode.asText()));
                return Mono.just(types);
            }
            return Mono.error(new IllegalArgumentException("Credential types not found or not an array in vcJson"));
        });
    }

    private String decodeToJSONstring(String encodedData) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] decodedData = Base45.getDecoder().decode(encodedData);
            CompressorInputStream inputStream = new CompressorStreamFactory()
                    .createCompressorInputStream(CompressorStreamFactory.DEFLATE, new ByteArrayInputStream(decodedData));
            IOUtils.copy(inputStream, out);
            CBORObject cbor = CBORObject.DecodeFromBytes(out.toByteArray());
            return cbor.ToJSONString();
        } catch (Exception e) {
            throw new ParseErrorException("Error decoding data: " + e.getMessage());
        }
    }

    @Override
    public Mono<Void> saveDeferredCredential(
            String processId,
            String userId,
            String credentialId,
            CredentialResponse credentialResponse
    ) {
        return parseCredentialId(credentialId)
                .flatMap(uuid -> fetchAndValidateExistingCredential(userId, uuid))
                .flatMap(existingCredential -> updateCredentialEntity(existingCredential, credentialResponse))
                .doOnSuccess(updatedEntity ->
                        log.info("[Process ID: {}] Deferred credential {} updated to VALID for user {}",
                                processId, updatedEntity.getCredentialId(), userId)
                )
                .then()
                .doOnError(error ->
                        log.error("[Process ID: {}] Error saving deferred credential ID {}: {}",
                                processId, credentialId, error.getMessage(), error)
                );
    }

    /**
     * Converts the incoming credentialId (String) to a UUID.
     *    Returns a Mono.error(...) if credentialId is null, blank, or invalid.
     */
    private Mono<UUID> parseCredentialId(String credentialId) {
        return Mono.fromCallable(() -> {
            if (credentialId == null || credentialId.isBlank()) {
                throw new IllegalArgumentException("Credential ID is null or blank");
            }
            return UUID.fromString(credentialId);
        });
    }

    /**
     * Retrieves the existing credential entity from the DB and validates:
     *    - The credential belongs to the given userId
     *    - The credential is in ISSUED status
     */
    private Mono<Credential> fetchAndValidateExistingCredential(String userId, UUID credentialUuid) {
        return credentialRepository.findById(credentialUuid)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No credential found for ID: " + credentialUuid
                )))
                .flatMap(existingCredential -> {
                    if (!existingCredential.getUserId().toString().equalsIgnoreCase(userId)) {
                        return Mono.error(new IllegalStateException(
                                "User ID mismatch. Credential belongs to user " + existingCredential.getUserId()
                        ));
                    }
                    if (existingCredential.getCredentialStatus() != CredentialStatus.ISSUED.getCode()) {
                        return Mono.error(new IllegalStateException(
                                "Credential is not in ISSUED status (found " + existingCredential.getCredentialStatus() + ")"
                        ));
                    }
                    return Mono.just(existingCredential);
                });
    }

    /**
     * Updates the existing credential entity:
     *    - Sets status from ISSUED to VALID
     *    - Sets format, raw credential data, updatedAt timestamp
     *    - Saves the entity
     */
    private Mono<Credential> updateCredentialEntity(Credential existingCredential, CredentialResponse credentialResponse) {
        return extractCredentialFormat(credentialResponse)
                .flatMap(credentialFormat -> {
                    // set status from ISSUED to VALID
                    existingCredential.setCredentialStatus(CredentialStatus.VALID.getCode());
                    existingCredential.setCredentialFormat(credentialFormat);
                    existingCredential.setCredentialData(credentialResponse.credential());
                    existingCredential.setUpdatedAt(Timestamp.from(Instant.now()));

                    return credentialRepository.save(existingCredential);
                });
    }

    @Override
    public Mono<List<CredentialsBasicInfo>> getCredentialsByUserId(String processId, String userId) {
        return parseUserId(userId)
                .flatMap(credentialRepository::findCredentialsByUserId)
                .flatMap(credentials -> {
                    if (credentials.isEmpty()) {
                        return Mono.error(new NoSuchVerifiableCredentialException(
                                "No credentials found for userId: " + userId
                        ));
                    }
                    List<CredentialsBasicInfo> infoList = credentials.stream()
                            .map(this::mapToCredentialsBasicInfo)
                            .toList();

                    return Mono.just(infoList);
                });
    }


    private CredentialsBasicInfo mapToCredentialsBasicInfo(Credential credential) {
        JsonNode jsonVc = parseJsonVc(credential.getJsonVc());
        JsonNode credentialSubject = jsonVc.get("credentialSubject");

        // if there's a 'validUntil' node, parse it
        ZonedDateTime validUntil = null;
        JsonNode validUntilNode = jsonVc.get("validUntil");
        if (validUntilNode != null && !validUntilNode.isNull()) {
            validUntil = parseZonedDateTime(validUntilNode.asText());
        }

        // Convert the int in DB -> enum constant
        CredentialStatus status = CredentialStatus.fromCode(credential.getCredentialStatus());

        return CredentialsBasicInfo.builder()
                .id(credential.getCredentialId().toString())
                .vcType(credential.getCredentialType())   // e.g., ["VerifiableCredential","SomeOtherType"]
                .credentialStatus(status)
                .availableFormats(determineAvailableFormats(credential.getCredentialFormat()))
                .credentialSubject(credentialSubject)
                .validUntil(validUntil)
                .build();
    }

    private List<String> determineAvailableFormats(Integer credentialFormat) {
        if (credentialFormat == null) {
            return Collections.emptyList();
        }
        // Here we do the "reverse" mapping from an int code to a format name
        try {
            CredentialFormats formatEnum = CredentialFormats.fromCode(credentialFormat);
            // Return whichever representation you prefer
            return List.of(formatEnum.name());
        } catch (IllegalArgumentException e) {
            return List.of("UNKNOWN_FORMAT");
        }
    }

    private ZonedDateTime parseZonedDateTime(String dateString) {
        try {
            // Try parsing as standard ISO-8601, e.g. "2024-04-21T09:29:30Z"
            return ZonedDateTime.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for validUntil: " + dateString, e);
        }
    }

    private JsonNode parseJsonVc(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            // Return an empty object node or throw an exception, depending on your needs
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse credential JSON: " + e.getMessage(), e);
        }
    }


    private Mono<UUID> parseUserId(String userId) {
        return Mono.fromCallable(() -> {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("userId is null or blank");
            }
            return UUID.fromString(userId);
        });
    }

}
