package es.in2.wallet.domain.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.application.dto.CredentialEntityBuildParams;
import es.in2.wallet.application.dto.CredentialResponse;
import es.in2.wallet.application.dto.CredentialsBasicInfo;
import es.in2.wallet.domain.entities.Credential;
import es.in2.wallet.domain.enums.CredentialFormats;
import es.in2.wallet.domain.enums.CredentialStatus;
import es.in2.wallet.domain.exceptions.NoSuchVerifiableCredentialException;
import es.in2.wallet.domain.exceptions.ParseErrorException;
import es.in2.wallet.domain.repositories.CredentialRepository;
import es.in2.wallet.domain.services.CredentialService;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static es.in2.wallet.domain.utils.ApplicationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialServiceImpl implements CredentialService {

    private final CredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------------
    // Save Credential
    // ---------------------------------------------------------------------
    @Override
    public Mono<String> saveCredential(String processId, UUID userId, CredentialResponse credentialResponse, String format) {
        Instant currentTime = Instant.now();

        if (credentialResponse == null) {
            return Mono.error(new IllegalArgumentException("CredentialResponse is null"));
        }

        // If transactionId is present, treat it as a plain (non-signed) credential
        if (credentialResponse.transactionId() != null) {
            return extractCredentialFormat(format)
                    .flatMap(credentialFormat ->
                            parseAsPlainJson(credentialResponse.credentials().get(0).credential())
                                    .flatMap(vcJson -> Mono.zip(
                                            extractCredentialTypes(vcJson),
                                            extractVerifiableCredentialIdFromVcJson(vcJson),
                                            (types, credId) -> buildCredentialEntity(
                                                    CredentialEntityBuildParams.builder()
                                                            .credentialId(credId)
                                                            .userId(userId)
                                                            .credentialTypes(types)
                                                            .credentialFormat(credentialFormat)
                                                            .credentialData(null)
                                                            .vcJson(vcJson)
                                                            .credentialStatus(CredentialStatus.ISSUED)  // Deferred => ISSUED
                                                            .currentTime(currentTime)
                                                            .build()
                                            )
                                    ))
                    )
                    .flatMap(credentialEntity ->
                            credentialRepository.save(credentialEntity)
                                    .doOnSuccess(saved -> log.info(
                                            "[Process ID: {}] Deferred credential with ID {} saved successfully.",
                                            processId,
                                            saved.getCredentialId()
                                    ))
                                    .thenReturn(credentialEntity.getCredentialId())
                    );
        }

        // Otherwise, handle known formats (JWT_VC, CWT_VC)
        return extractCredentialFormat(format)
                .flatMap(credentialFormat ->
                        extractVcJson(credentialResponse, format)
                                .flatMap(vcJson -> Mono.zip(
                                        extractCredentialTypes(vcJson),
                                        extractVerifiableCredentialIdFromVcJson(vcJson),
                                        (credentialTypes, credentialId) -> buildCredentialEntity(
                                                CredentialEntityBuildParams.builder()
                                                        .credentialId(credentialId)
                                                        .userId(userId)
                                                        .credentialTypes(credentialTypes)
                                                        .credentialFormat(credentialFormat)
                                                        .credentialData(credentialResponse.credentials().get(0).credential()) // raw credential data
                                                        .vcJson(vcJson)
                                                        .credentialStatus(CredentialStatus.VALID) // store as VALID code
                                                        .currentTime(currentTime)
                                                        .build()
                                        )
                                ))
                                .flatMap(credentialEntity ->

                                        credentialRepository.save(credentialEntity)
                                                .doOnSuccess(saved -> log.info(
                                                        "[Process ID: {}] Credential with ID {} saved successfully.",
                                                        processId,
                                                        saved.getCredentialId()
                                                ))
                                                .thenReturn(credentialEntity.getCredentialId())
                                )
                );
    }


    // ---------------------------------------------------------------------
    // Deferred Credential
    // ---------------------------------------------------------------------
    @Override
    public Mono<Void> saveDeferredCredential(
            String processId,
            String userId,
            String credentialId,
            CredentialResponse credentialResponse
    ) {
        return parseStringToUuid(userId, USER_ID)
                .zipWith(Mono.just(credentialId))
                .flatMap(tuple -> {
                    UUID userUuid = tuple.getT1();
                    String cred = tuple.getT2();
                    // We need to ensure the credential is in ISSUED status
                    return fetchCredentialOrErrorInIssuedStatus(cred, userUuid);
                })
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

    // ---------------------------------------------------------------------
    // Fetch All Credentials by User
    // ---------------------------------------------------------------------
    @Override
    public Mono<List<CredentialsBasicInfo>> getCredentialsByUserId(String processId, String userId) {
        return parseStringToUuid(userId, USER_ID)
                .flatMapMany(credentialRepository::findAllByUserId)
                .map(this::mapToCredentialsBasicInfo)
                .onErrorContinue((throwable, raw) ->
                        log.error("[{}] Error while mapping credential {} for user {}",
                                processId, raw, userId, throwable))
                .collectList()
                .flatMap(list -> list.isEmpty()
                        ? Mono.error(new NoSuchVerifiableCredentialException(
                        "No valid credentials found for user " + userId))
                        : Mono.just(list));
    }

    // ---------------------------------------------------------------------
    // Helper to map from Credential entity to DTO
    // ---------------------------------------------------------------------
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
        CredentialStatus status = CredentialStatus.valueOf(credential.getCredentialStatus());

        return CredentialsBasicInfo.builder()
                .id(credential.getCredentialId())
                .vcType(credential.getCredentialType())   // e.g., ["VerifiableCredential", "SomeOtherType"]
                .credentialStatus(status)
                .availableFormats(determineAvailableFormats(credential.getCredentialFormat()))
                .credentialSubject(credentialSubject)
                .validUntil(validUntil)
                .build();
    }

    // ---------------------------------------------------------------------
    // Filter credentials by user AND type in JWT_VC format
    // ---------------------------------------------------------------------
    @Override
    public Mono<List<CredentialsBasicInfo>> getCredentialsByUserIdAndType(
            String processId,
            String userId,
            String requiredType
    ) {
        return parseStringToUuid(userId, USER_ID)
                .flatMapMany(credentialRepository::findAllByUserId)
                .filter(credential -> {
                    boolean matchesType = credential.getCredentialType().contains(requiredType);
                    boolean isJwtVc = credential.getCredentialFormat() != null
                            && credential.getCredentialFormat().equals(CredentialFormats.JWT_VC.toString());
                    return matchesType && isJwtVc;
                })
                .map(this::mapToCredentialsBasicInfo)
                .collectList()
                .flatMap(credentialsInfo -> {
                    if (credentialsInfo.isEmpty()) {
                        return Mono.error(new NoSuchVerifiableCredentialException(
                                "No credentials found for userId=" + userId
                                        + " with type=" + requiredType
                                        + " in JWT_VC format."
                        ));
                    }
                    return Mono.just(credentialsInfo);
                });
    }

    // ---------------------------------------------------------------------
    // Return raw credential data (checked ownership)
    // ---------------------------------------------------------------------
    @Override
    public Mono<String> getCredentialDataByIdAndUserId(
            String processId,
            String userId,
            String credentialId
    ) {
        return parseStringToUuid(userId, USER_ID)
                .zipWith(Mono.just(credentialId))
                .flatMap(tuple -> {
                    UUID userUuid = tuple.getT1();
                    String cred = tuple.getT2();
                    return fetchCredentialOrError(cred, userUuid);  // no special status required
                })
                .map(credential -> {
                    String data = credential.getCredentialData();
                    log.info("[Process ID: {}] Successfully retrieved credential data for credentialId={}, userId={}",
                            processId, credential.getCredentialId(), userId);
                    return data;
                });
    }

    // ---------------------------------------------------------------------
    // Extract DID
    // ---------------------------------------------------------------------
    @Override
    public Mono<String> extractDidFromCredential(String processId, String credentialId, String userId) {
        return parseStringToUuid(userId, USER_ID)
                .zipWith(Mono.just(credentialId))
                .flatMap(tuple -> {
                    UUID userUuid = tuple.getT1();
                    String cred = tuple.getT2();
                    return fetchCredentialOrError(cred, userUuid);
                })
                .flatMap(credential -> {
                    // Parse the VC JSON
                    JsonNode vcNode = parseJsonVc(credential.getJsonVc());

                    // Decide if LEARCredentialEmployee
                    boolean isLear = credential.getCredentialType().stream()
                            .anyMatch("LEARCredentialEmployee"::equals);

                    // Extract DID from the correct path
                    JsonNode didNode = isLear
                            ? vcNode.at("/credentialSubject/mandate/mandatee/id")
                            : vcNode.at("/credentialSubject/id");

                    if (didNode.isMissingNode() || didNode.asText().isBlank()) {
                        return Mono.error(new NoSuchVerifiableCredentialException("DID not found in credential"));
                    }
                    return Mono.just(didNode.asText());
                });
    }

    // ---------------------------------------------------------------------
    // Delete credential
    // ---------------------------------------------------------------------
    @Override
    public Mono<Void> deleteCredential(String processId, String credentialId, String userId) {
        return parseStringToUuid(userId, USER_ID)
                .zipWith(Mono.just(credentialId))
                .flatMap(tuple -> {
                    UUID userUuid = tuple.getT1();
                    String cred = tuple.getT2();
                    return fetchCredentialOrError(cred, userUuid);
                })
                .flatMap(credentialRepository::delete)
                .doOnSuccess(unused ->
                        log.info("[Process ID: {}] Credential with ID {} successfully deleted for user {}",
                                processId, credentialId, userId)
                );
    }

    // ---------------------------------------------------------------------
    // Private Helper to fetch credential from DB and check ownership
    // (optionally can also check status)
    // ---------------------------------------------------------------------
    private Mono<Credential> fetchCredentialOrError(String credId, UUID userId) {
        // No status check
        return credentialRepository.findByCredentialId(credId)
                .switchIfEmpty(Mono.error(new NoSuchVerifiableCredentialException(
                        "No credential found for ID: " + credId
                )))
                .flatMap(credential -> {
                    if (!credential.getUserId().equals(userId)) {
                        return Mono.error(new IllegalStateException(
                                "User ID mismatch. Credential belongs to user " + credential.getUserId()
                        ));
                    }
                    return Mono.just(credential);
                });
    }

    private Mono<Credential> fetchCredentialOrErrorInIssuedStatus(String credId, UUID userId) {
        return fetchCredentialOrError(credId, userId)
                .flatMap(credential -> {
                    if (!Objects.equals(credential.getCredentialStatus(), CredentialStatus.ISSUED.toString())) {
                        return Mono.error(new IllegalStateException(
                                "Credential is not in ISSUED status (found " + credential.getCredentialStatus() + ")"
                        ));
                    }
                    return Mono.just(credential);
                });
    }


    // ---------------------------------------------------------------------
    // Parsing Helpers
    // ---------------------------------------------------------------------
    private Mono<UUID> parseStringToUuid(String value, String fieldName) {
        return Mono.fromCallable(() -> {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " is null or blank");
            }
            return UUID.fromString(value);
        });
    }

    // ---------------------------------------------------------------------
    // Build Credential Entity
    // ---------------------------------------------------------------------
    private Credential buildCredentialEntity(
            CredentialEntityBuildParams params
    ) {
        return Credential.builder()
                .credentialId(params.credentialId())
                .userId(params.userId())
                .credentialType(params.credentialTypes())
                .credentialStatus(params.credentialStatus().toString())
                .credentialFormat(params.credentialFormat().toString())
                .credentialData(params.credentialData())
                .jsonVc(params.vcJson().toString())
                .createdAt(params.currentTime())
                .updatedAt(params.currentTime())
                .build();
    }

    // ---------------------------------------------------------------------
    // Parsing JSON for Non-Signed Credential
    // ---------------------------------------------------------------------
    private Mono<JsonNode> parseAsPlainJson(String rawJson) {
        return Mono.fromCallable(() -> {
            if (rawJson == null || rawJson.isBlank()) {
                throw new ParseErrorException("Credential data is empty or null");
            }
            return objectMapper.readTree(rawJson);
        }).onErrorMap(e -> new ParseErrorException("Error parsing plain JSON credential: " + e.getMessage()));
    }

    // ---------------------------------------------------------------------
    // Extract Format
    // ---------------------------------------------------------------------
    private Mono<CredentialFormats> extractCredentialFormat(String format) {
        if (format == null || format.isBlank()) {
            return Mono.error(new IllegalArgumentException("CredentialResponse format is null"));
        }
        return switch (format) {
            case JWT_VC, JWT_VC_JSON -> Mono.just(CredentialFormats.JWT_VC);
            case CWT_VC -> Mono.just(CredentialFormats.CWT_VC);
            default -> Mono.error(new IllegalArgumentException(
                    "Unsupported credential format: " + format
            ));
        };
    }

    // ---------------------------------------------------------------------
    // Extract VC JSON based on Format
    // ---------------------------------------------------------------------
    private Mono<JsonNode> extractVcJson(CredentialResponse credentialResponse, String format) {
        return switch (format) {
            case JWT_VC, JWT_VC_JSON -> extractVcJsonFromJwt(credentialResponse.credentials().get(0).credential());
            case CWT_VC -> extractVcJsonFromCwt(credentialResponse.credentials().get(0).credential());
            default -> Mono.error(new IllegalArgumentException(
                    "Unsupported credential format"
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

    // ---------------------------------------------------------------------
    // Extract ID and Types from the VC JSON
    // ---------------------------------------------------------------------
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

    // ---------------------------------------------------------------------
    // CWT Decoding (Base45 -> DEFLATE -> CBOR -> JSON)
    // ---------------------------------------------------------------------
    private String decodeToJSONstring(String encodedData) {
        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                CompressorInputStream inputStream = new CompressorStreamFactory()
                        .createCompressorInputStream(
                                CompressorStreamFactory.DEFLATE,
                                new ByteArrayInputStream(Base45.getDecoder().decode(encodedData)))
        ) {
            IOUtils.copy(inputStream, out);
            CBORObject cbor = CBORObject.DecodeFromBytes(out.toByteArray());
            return cbor.ToJSONString();
        } catch (Exception e) {
            throw new ParseErrorException("Error decoding data: " + e.getMessage());
        }
    }


    // ---------------------------------------------------------------------
    // Update Credential (Deferred: ISSUED -> VALID, data, etc.)
    // ---------------------------------------------------------------------
    private Mono<Credential> updateCredentialEntity(Credential existingCredential, CredentialResponse credentialResponse) {
        existingCredential.setCredentialStatus(CredentialStatus.VALID.toString());
        existingCredential.setCredentialData(credentialResponse.credentials().get(0).credential());
        existingCredential.setUpdatedAt(Instant.now());
        return credentialRepository.save(existingCredential);
    }


    // ---------------------------------------------------------------------
    // Determine Available Formats for the BasicInfo DTO
    // ---------------------------------------------------------------------
    private List<String> determineAvailableFormats(String credentialFormat) {
        if (credentialFormat == null) {
            return Collections.emptyList();
        }
        try {
            CredentialFormats formatEnum = CredentialFormats.valueOf(credentialFormat);
            return List.of(formatEnum.name());
        } catch (IllegalArgumentException e) {
            return List.of("UNKNOWN_FORMAT");
        }
    }

    // ---------------------------------------------------------------------
    // Parse ZonedDateTime
    // ---------------------------------------------------------------------
    private ZonedDateTime parseZonedDateTime(String dateString) {
        try {
            return ZonedDateTime.parse(dateString);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format for validUntil: " + dateString, e);
        }
    }

    // ---------------------------------------------------------------------
    // parseJsonVc - If blank, returns empty object node
    // ---------------------------------------------------------------------
    private JsonNode parseJsonVc(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse credential JSON: " + e.getMessage(), e);
        }
    }
}