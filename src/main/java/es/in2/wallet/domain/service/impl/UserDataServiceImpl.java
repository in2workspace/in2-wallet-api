package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.domain.exception.NoSuchVerifiableCredentialException;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.minvws.encoding.Base45;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.in2.wallet.domain.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataServiceImpl implements UserDataService {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new UserEntity.
     *
     * @param id The unique identifier for the user.
     */
    @Override
    public Mono<String> createUserEntity(String id) {
        // Construct the UserEntity
        WalletUser walletUser = WalletUser.builder().id(USER_ENTITY_PREFIX + id).type(WALLET_USER_TYPE).build();

        // Log the creation of the entity
        log.debug("UserEntity created for: {}", id);

        return deserializeEntityToString(walletUser);
    }

    /**
     * Deserializes an entity object to a JSON string.
     *
     * @param entity The entity object to be deserialized.
     */
    private Mono<String> deserializeEntityToString(Object entity) {
        try {
            return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity));

        } catch (JsonProcessingException e) {
            return Mono.error(new ParseErrorException("Error deserializing entity: " + e));
        }
    }

    /**
     * Saves verifiable credentials (VCs) for a user, supporting multiple formats like JWT, CWT, and JSON.
     * This method consolidates credential formats into a single entity and ensures the `vc_json` format is always present,
     * extracting it from other formats if necessary. It also determines the credential status based on the presence
     * of signed formats (JWT or CWT), setting it to ISSUED if any are present, otherwise to GENERATED.
     *
     * @param userId The ID of the user for whom the credentials are being saved.
     * @param credentials The list of credential responses which may contain various formats of the same credential.
     */
    @Override
    public Mono<String> saveVC(String userId, List<CredentialResponse> credentials) {
        Map<String, CredentialAttribute> formatMap = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // Process each credential format and handle errors
        processCredentialFormats(credentials, formatMap, errors);
        if (!errors.isEmpty()) {
            return Mono.error(new IllegalArgumentException(String.join(", ", errors)));
        }

        // Determine the status of the credential based on the presence of signed formats
        CredentialStatus status = determineCredentialStatus(formatMap);
        // Build and save the credential entity based on the processed data
        return buildAndSaveCredentialEntity(formatMap, status, userId)
                .doOnSuccess(entity -> log.info("Verifiable Credential saved successfully: {}", entity))
                .onErrorResume(e -> {
                    log.error("Error saving Verifiable Credential: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Error processing Verifiable Credential", e));
                });
    }

    private void processCredentialFormats(List<CredentialResponse> credentials, Map<String, CredentialAttribute> formatMap, List<String> errors) {
        credentials.forEach(cred -> {
            switch (cred.format()) {
                case JWT_VC, JWT_VC_JSON:
                    formatMap.put(JWT_VC, new CredentialAttribute(PROPERTY_TYPE, cred.credential()));
                    break;
                case VC_CWT:
                    formatMap.put(VC_CWT, new CredentialAttribute(PROPERTY_TYPE, cred.credential()));
                    break;
                case VC_JSON:
                    formatMap.put(VC_JSON, new CredentialAttribute(PROPERTY_TYPE, cred.credential()));
                    break;
                default:
                    errors.add("Unsupported credential format: " + cred.format());
                    break;
            }
        });
    }

    private CredentialStatus determineCredentialStatus(Map<String, CredentialAttribute> formatMap) {
        return (formatMap.containsKey(JWT_VC) || formatMap.containsKey(VC_CWT)) ? CredentialStatus.VALID : CredentialStatus.ISSUED;
    }

    private Mono<String> buildAndSaveCredentialEntity(Map<String, CredentialAttribute> formatMap, CredentialStatus status, String userId) {
        return Mono.justOrEmpty(formatMap.get(VC_JSON))
                .switchIfEmpty(extractVcJsonIfNecessary(formatMap))
                .flatMap(vcJsonAttribute -> extractVerifiableCredentialIdFromVcJson((JsonNode) vcJsonAttribute.value())
                        .flatMap(vcId -> {
                            List<String> types = extractCredentialTypes((JsonNode) vcJsonAttribute.value());
                            CredentialEntity credentialEntity = buildCredentialEntity(formatMap, status, userId, vcJsonAttribute, vcId, types);
                            return deserializeEntityToString(credentialEntity);
                        }));
    }

    private Mono<CredentialAttribute> extractVcJsonIfNecessary(Map<String, CredentialAttribute> formatMap) {
        if (formatMap.containsKey(JWT_VC)) {
            return extractVcJsonFromVcJwt(formatMap.get(JWT_VC).value().toString())
                    .map(jsonNode -> new CredentialAttribute(PROPERTY_TYPE, jsonNode));
        } else if (formatMap.containsKey(VC_CWT)) {
            return fromVpCborToVcJsonReactive(formatMap.get(VC_CWT).value().toString())
                    .map(jsonNode -> new CredentialAttribute(PROPERTY_TYPE, jsonNode));
        }
        return Mono.error(new RuntimeException("No suitable format available to extract vc_json"));
    }

    private List<String> extractCredentialTypes(JsonNode vcJson) {
        List<String> types = new ArrayList<>();
        JsonNode typesNode = vcJson.get("type");
        if (typesNode != null && typesNode.isArray()) {
            typesNode.forEach(typeNode -> types.add(typeNode.asText()));
        }
        return types;
    }

    private CredentialEntity buildCredentialEntity(Map<String, CredentialAttribute> formatMap, CredentialStatus status, String userId, CredentialAttribute vcJsonAttribute, String vcId, List<String> types) {
        CredentialEntity.CredentialEntityBuilder builder = CredentialEntity.builder()
                .id(CREDENTIAL_ENTITY_PREFIX + vcId)
                .type(CREDENTIAL_TYPE)
                .jsonCredentialAttribute(vcJsonAttribute)
                .credentialStatusAttribute(new CredentialStatusAttribute(PROPERTY_TYPE, status))
                .credentialTypeAttribute(new CredentialTypeAttribute(PROPERTY_TYPE, types))
                .relationshipAttribute(new RelationshipAttribute(RELATIONSHIP_TYPE, USER_ENTITY_PREFIX + userId));

        if (formatMap.containsKey(JWT_VC)) {
            builder.jwtCredentialAttribute(formatMap.get(JWT_VC));
        }
        if (formatMap.containsKey(VC_CWT)) {
            builder.cwtCredentialAttribute(formatMap.get(VC_CWT));
        }
        return builder.build();
    }

    private Mono<JsonNode> fromVpCborToVcJsonReactive(String qrData) {
        return Mono.fromCallable(() -> {
            String vp = decodeToJSONstring(qrData);

            try {
                JsonNode vpJsonObject = objectMapper.readTree(vp);
                JsonNode vpContent = objectMapper.readTree(vpJsonObject.get("vp").toString());
                String cvId = vpJsonObject.get("nonce").asText();

                String vcCbor = vpContent.get("verifiableCredential").asText();
                String vc = decodeToJSONstring(vcCbor);

                JsonNode vcJsonObject = objectMapper.readTree(vc);
                JsonNode vcContent = objectMapper.readTree(vcJsonObject.get("vc").toString());
                ((ObjectNode) vcContent).put("id", cvId);
                ((ObjectNode) vcJsonObject).set("vc", vcContent);

                return vcJsonObject;
            } catch (JsonProcessingException e) {
                log.error("Error processing JSON", e);
                throw new ParseErrorException(e.getMessage());
            }
        });
    }

    private String decodeToJSONstring(String qrData) {
        String rawStringData = removeQuotes(qrData);
        byte[] zip = Base45.getDecoder().decode(rawStringData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(zip);
            CompressorStreamFactory factory = new CompressorStreamFactory();
            IOUtils.copy(factory.createCompressorInputStream(CompressorStreamFactory.DEFLATE, bais), baos);
            byte[] cose = baos.toByteArray();

            CBORObject cborObject = CBORObject.DecodeFromBytes(cose);
            ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
            cborObject.WriteJSONTo(jsonOut);

            return jsonOut.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ParseErrorException("Error processing data: " + e);
        }
    }

    private String removeQuotes(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        } else {
            return input;
        }
    }

    /**
     * Extracts the JSON content from a Verifiable Credential JWT.
     *
     * @param vcJwt The VC JWT from which to extract the JSON content.
     */
    private Mono<JsonNode> extractVcJsonFromVcJwt(String vcJwt) {
        return Mono.fromCallable(() -> SignedJWT.parse(vcJwt)).onErrorMap(ParseException.class, e -> new ParseErrorException("Error while parsing VC JWT: " + e.getMessage())).handle((parsedVcJwt, sink) -> {
            try {
                JsonNode jsonObject = objectMapper.readTree(parsedVcJwt.getPayload().toString());
                JsonNode vcJson = jsonObject.get("vc");
                if (vcJson != null) {
                    log.debug("Verifiable Credential JSON extracted from VC JWT: {}", vcJson);
                    sink.next(vcJson);
                } else {
                    sink.error(new ParseErrorException("VC JSON is missing in the payload"));
                }
            } catch (JsonProcessingException e) {
                sink.error(new ParseErrorException("Error while processing JSON: " + e.getMessage()));
            }
        });
    }

    /**
     * Extracts the Verifiable Credential ID from its JSON representation.
     *
     * @param vcJson The VC JSON node from which to extract the ID.
     */
    private Mono<String> extractVerifiableCredentialIdFromVcJson(JsonNode vcJson) {
        return Mono.justOrEmpty(vcJson.get("id").asText()).flatMap(vcId -> {
            if (vcId == null || vcId.trim().isEmpty()) {
                log.error("The Verifiable Credential does not contain an ID.");
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Verifiable Credential does not contain an ID."));
            }
            log.debug("Verifiable Credential ID extracted: {}", vcId);
            return Mono.just(vcId);
        });
    }

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
            List<CredentialEntity> credentials = objectMapper.readValue(credentialsJson, new TypeReference<>() {
            });

            List<CredentialsBasicInfo> credentialsInfo = new ArrayList<>();
            for (CredentialEntity credential : credentials) {
                List<String> availableFormats = new ArrayList<>();
                availableFormats.add(VC_JSON);
                if (credential.jwtCredentialAttribute() != null) {
                    availableFormats.add(JWT_VC);
                }
                if (credential.cwtCredentialAttribute() != null) {
                    availableFormats.add(VC_CWT);
                }

                JsonNode jsonCredential = objectMapper.convertValue(credential.jsonCredentialAttribute().value(), JsonNode.class);
                JsonNode credentialSubject = jsonCredential.get(CREDENTIAL_SUBJECT);
                ZonedDateTime expirationDate = null;

                if (jsonCredential.has(EXPIRATION_DATE) && !jsonCredential.get(EXPIRATION_DATE).isNull()) {
                    expirationDate = parseZonedDateTime(jsonCredential.get(EXPIRATION_DATE).asText());
                }

                CredentialsBasicInfo info = CredentialsBasicInfo.builder()
                        .id(credential.id())
                        .vcType(credential.credentialTypeAttribute().value())
                        .credentialStatus(credential.credentialStatusAttribute().credentialStatus())
                        .availableFormats(availableFormats)
                        .credentialSubject(credentialSubject)
                        .expirationDate(expirationDate)
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
                            case VC_JSON:
                                credentialAttribute = credential.jsonCredentialAttribute();
                                break;
                            case JWT_VC:
                                credentialAttribute = credential.jwtCredentialAttribute();
                                break;
                            case VC_CWT:
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

    @Override
    public Mono<String> saveTransaction(String credentialId, String transactionId, String accessToken, String deferredEndpoint) {
        // Construct the Transaction Entity
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .id(TRANSACTION_ENTITY_PREFIX + UUID.randomUUID())
                .type(TRANSACTION_TYPE)
                .transactionDataAttribute(EntityAttribute.<TransactionDataAttribute>builder()
                        .type(PROPERTY_TYPE)
                        .value(TransactionDataAttribute.builder()
                                .transactionId(transactionId)
                                .accessToken(accessToken)
                                .deferredEndpoint(deferredEndpoint)
                                .build()).build())
                .relationshipAttribute(RelationshipAttribute.builder()
                        .type(RELATIONSHIP_TYPE)
                        .object(CREDENTIAL_ENTITY_PREFIX + credentialId)
                        .build())
                .build();

        // Log the creation of the entity
        log.debug("Transaction Entity created for: {}", credentialId);

        return deserializeEntityToString(transactionEntity);
    }

    @Override
    public Mono<String> updateVCEntityWithSignedFormat(String credentialEntityJson, CredentialResponse signedCredential) {
        return Mono.just(credentialEntityJson)
                .flatMap(json -> {
                    try {
                        // Deserialize the credential entity from JSON.
                        CredentialEntity credentialEntity = objectMapper.readValue(json, CredentialEntity.class);
                        // Start building a new credential entity with unchanged properties.
                        CredentialEntity.CredentialEntityBuilder updatedCredentialEntity = CredentialEntity.builder()
                                .id(credentialEntity.id())
                                .type(credentialEntity.type())
                                .jsonCredentialAttribute(credentialEntity.jsonCredentialAttribute())
                                .relationshipAttribute(credentialEntity.relationshipAttribute());

                        // Check the format of the signed credential and update the entity accordingly.
                        switch (signedCredential.format()) {
                            case JWT_VC:
                                // Set the JWT credential attribute if the format is JWT.
                                updatedCredentialEntity.jwtCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE, signedCredential.credential()));
                                break;
                            case VC_CWT:
                                // Set the CWT credential attribute if the format is CWT.
                                updatedCredentialEntity.cwtCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE, signedCredential.credential()));
                                break;
                            default:
                                // Return an error if the credential format is unsupported.
                                return Mono.error(new IllegalArgumentException("Unsupported credential format: " + signedCredential.format()));
                        }

                        // Change the credential status to ISSUED since it is now signed.
                        updatedCredentialEntity.credentialStatusAttribute(new CredentialStatusAttribute(PROPERTY_TYPE, CredentialStatus.VALID));

                        // Serialize the updated credential entity back to JSON and convert to string.
                        return deserializeEntityToString(updatedCredentialEntity.build());
                    } catch (JsonProcessingException e) {
                        // Handle JSON parsing errors.
                        return Mono.error(new ParseErrorException("Error processing credential entity: " + e));
                    }
                });
    }



    @Override
    public Mono<String> updateTransactionWithNewTransactionId(String transactionEntityJson, String transactionId) {
        return Mono.just(transactionEntityJson)
                .flatMap(json -> {
                    try {
                        // Deserialize the transaction entity from JSON.
                        TransactionEntity transactionEntity = objectMapper.readValue(json, TransactionEntity.class);

                        // Build a new transaction entity using the builder pattern with updated transactionId
                        TransactionEntity updatedTransactionEntity = TransactionEntity.builder()
                                .id(transactionEntity.id()) // Preserve the original ID
                                .type(transactionEntity.type()) // Preserve the original type
                                .transactionDataAttribute(EntityAttribute.<TransactionDataAttribute>builder()
                                        .type(transactionEntity.transactionDataAttribute().type()) // Preserve the original attribute type
                                        .value(TransactionDataAttribute.builder() // Build new transaction data
                                                .transactionId(transactionId) // Update with new transaction ID
                                                .accessToken(transactionEntity.transactionDataAttribute().value().accessToken()) // Preserve the original access token
                                                .deferredEndpoint(transactionEntity.transactionDataAttribute().value().deferredEndpoint()) // Preserve the original deferred endpoint
                                                .build())
                                        .build())
                                .relationshipAttribute(transactionEntity.relationshipAttribute()) // Preserve the original relationship attribute
                                .build();

                        // Serialize the updated transaction entity back to JSON
                        return deserializeEntityToString(updatedTransactionEntity);
                    } catch (JsonProcessingException e) {
                        // Handle JSON parsing errors
                        return Mono.error(new ParseErrorException("Error processing transaction entity: " + e));
                    }
                });
    }



}
