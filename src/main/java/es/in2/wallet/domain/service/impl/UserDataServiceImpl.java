package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;
import com.upokecenter.cbor.CBORObject;
import es.in2.wallet.domain.exception.NoSuchDidException;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

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
     * Creates a new UserEntity instance with an empty list of credentials and DIDs.
     *
     * @param id The unique identifier for the user.
     */
    @Override
    public Mono<String> createUserEntity(String id) {
        // Construct the UserEntity
        UserEntity userEntity = new UserEntity("urn:entities:userId:" + id, "userEntity", new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>()), new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>()));

        // Log the creation of the entity
        log.debug("UserEntity created for: {}", id);

        return deserializeUserEntityToString(userEntity);
    }

    /**
     * Deserializes a UserEntity object to a JSON string.
     *
     * @param userEntity The UserEntity object to be deserialized.
     */
    private Mono<String> deserializeUserEntityToString(UserEntity userEntity) {
        try {
            return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userEntity));

        } catch (JsonProcessingException e) {
            return Mono.error(new ParseErrorException("Error deserializing UserEntity: " + e));
        }
    }

    /**
     * Serializes a JSON string to a UserEntity object.
     *
     * @param userEntity The JSON string representing a UserEntity.
     */
    private Mono<UserEntity> serializeUserEntity(String userEntity) {
        try {
            // Setting up ObjectMapper for deserialization
            objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

            // Deserializing the response into a UserEntity
            UserEntity entity = objectMapper.readValue(userEntity, UserEntity.class);
            log.debug("User Entity: {}", userEntity);

            // Returning the UserEntity wrapped in a Mono
            return Mono.just(entity);
        } catch (JsonProcessingException e) {
            // Logging and returning an error Mono if deserialization fails
            log.error("Error while deserializing UserEntity: ", e);
            return Mono.error(new ParseErrorException("Error deserializing UserEntity: "));
        }
    }

    /**
     * Saves Verifiable Credentials (VCs) for a user entity, handling both JWT and CWT formats.
     * This method serializes the user entity, processes each credential to extract or convert to JSON,
     * and updates the user entity with both the original format credentials and their JSON representation.
     * The JSON is used for displaying purposes, while the original formats are retained for integrity.
     *
     * @param userEntity  The serialized user entity to which the credentials are to be saved.
     * @param credentials The list of CredentialResponse objects containing the VCs in various formats.
     */

    @Override
    public Mono<String> saveVC(String userEntity, List<CredentialResponse> credentials) {
        return serializeUserEntity(userEntity).flatMap(entity -> {
            CredentialResponse selectedCredential = credentials.stream().filter(cred -> VC_JWT.equals(cred.format()) || JWT_VC_JSON.equals(cred.format()) || VC_CWT.equals(cred.format())).findFirst().orElseThrow(() -> new RuntimeException("No suitable credential format found."));

            Mono<JsonNode> vcJsonMono = selectedCredential.format().equals(VC_JWT) || selectedCredential.format().equals(JWT_VC_JSON) ? extractVcJsonFromVcJwt((String) selectedCredential.credential()) : fromVpCborToVcJsonReactive((String) selectedCredential.credential());

            return vcJsonMono.flatMap(vcJson -> extractVerifiableCredentialIdFromVcJson(vcJson).flatMap(vcId -> {
                List<VCAttribute> vcAttributes = new ArrayList<>();
                for (CredentialResponse cred : credentials) {
                    vcAttributes.add(new VCAttribute(vcId, cred.format(), cred.credential()));
                }
                vcAttributes.add(new VCAttribute(vcId, VC_JSON, vcJson));
                List<VCAttribute> updatedVCs = new ArrayList<>(entity.vcs().value());
                updatedVCs.addAll(vcAttributes);
                EntityAttribute<List<VCAttribute>> vcs = new EntityAttribute<>(entity.vcs().type(), updatedVCs);

                UserEntity updatedUserEntity = UserEntity.builder().id(entity.id()).type(entity.type()).vcs(vcs).dids(entity.dids()).build();

                return deserializeUserEntityToString(updatedUserEntity);
            }));
        }).doOnSuccess(updatedUserEntity -> log.info("Verifiable Credential saved successfully: {}", updatedUserEntity)).onErrorResume(e -> {
            log.error("Error saving Verifiable Credential: {}", e.getMessage());
            return Mono.error(new RuntimeException("Error processing Verifiable Credential", e));
        });
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
     * Retrieves the user's Verifiable Credentials in JSON format.
     *
     * @param userEntity The user entity whose VCs are to be retrieved.
     */
    @Override
    public Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String userEntity) {
        return serializeUserEntity(userEntity).flatMapMany(user -> Flux.fromIterable(user.vcs().value()))
                .filter(vcAttribute -> VC_JSON.equals(vcAttribute.type()))
                .flatMap(item -> {

                    LinkedHashMap<?, ?> vcDataValue = (LinkedHashMap<?, ?>) item.value();
                    JsonNode jsonNode = objectMapper.convertValue(vcDataValue, JsonNode.class);

                    return Mono.zip(
                            getVcTypeListFromVcJson(jsonNode),
                            getAvailableFormatListById(item.id(),userEntity),
                            Mono.just(jsonNode)
                    )
                            .flatMap(tuple -> {
                                List<String> vcTypeList = tuple.getT1();
                                List<String> availableFormats = tuple.getT2();
                                JsonNode credentialSubject = tuple.getT3().get(CREDENTIAL_SUBJECT);

                                ZonedDateTime expirationDate = null;
                                if (jsonNode.has(EXPIRATION_DATE) && !jsonNode.get(EXPIRATION_DATE).isNull()) {
                                    expirationDate = parseZonedDateTime(jsonNode.get(EXPIRATION_DATE).asText());
                                }
                            return Mono.just(CredentialsBasicInfo.builder()
                                .id(item.id())
                                .vcType(vcTypeList)
                                .availableFormats(availableFormats)
                                .credentialSubject(credentialSubject)
                                .expirationDate(expirationDate)
                                .build());

                            });
                }).collectList()
                .onErrorResume(
                        NoSuchVerifiableCredentialException.class, Mono::error
                );
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
     * Retrieves a list of user's Verifiable Credentials (VCs) that match a given list of VC types.
     * This method filters the user's VCs based on the specified VC types and returns a list of
     * CredentialsBasicInfo objects representing the matching VCs.
     *
     * TODO: Instead of using JsonNode directly to extract VC type, map the VC type to its schema to ensure
     * better type safety and schema validation. This could involve creating a model class for the VCs and
     * using a more structured approach to parsing and validating VCs based on their defined schemas.
     *
     * @param vcTypeList A list of VC types to filter the user's VCs by.
     * @param userEntity The identifier for the user entity whose VCs are to be filtered.
     * @return A Mono emitting a list of CredentialsBasicInfo objects representing the user's VCs
     * that match the specified types.
     */
    @Override
    public Mono<List<CredentialsBasicInfo>> getSelectableVCsByVcTypeList(List<String> vcTypeList, String userEntity) {
        // First, retrieve all VCs for the user in the VC_JSON format.
        return getVerifiableCredentialsByFormat(userEntity, VC_JSON)
                .flatMapMany(Flux::fromIterable) // Convert the list of VCs to a Flux stream for further processing.
                .flatMap(vcAttribute -> {
                    // Convert each VC attribute value to a JsonNode for easier data extraction.
                    JsonNode jsonNode = objectMapper.convertValue(vcAttribute.value(), JsonNode.class);

                    // Fetch the available formats asynchronously.
                    Mono<List<String>> availableFormatsMono = getAvailableFormatListById(vcAttribute.id(), userEntity);


                    // Since expiration date is handled synchronously, only zip the asynchronous operations.
                    return availableFormatsMono.map(availableFormats -> Tuples.of(vcAttribute.id(), availableFormats, jsonNode));
                })
                .filter(tuple -> {
                    // Extract VC types list from the JSON node.
                    JsonNode jsonNode = tuple.getT3();
                    List<String> vcDataTypeList = new ArrayList<>();
                    jsonNode.withArray("type").elements().forEachRemaining(node -> vcDataTypeList.add(node.asText()));
                    // Ensure the VC matches at least one of the specified types.
                    return vcTypeList.stream().anyMatch(vcDataTypeList::contains);
                })
                .map(tuple -> {
                    // Map the tuple to a CredentialsBasicInfo object.
                    String vcId = tuple.getT1();
                    List<String> availableFormats = tuple.getT2();
                    JsonNode jsonNode = tuple.getT3();

                    ZonedDateTime expirationDate = null;
                    if (jsonNode.has(EXPIRATION_DATE) && !jsonNode.get(EXPIRATION_DATE).isNull()) {
                        expirationDate = parseZonedDateTime(jsonNode.get(EXPIRATION_DATE).asText());
                    }

                    List<String> vcDataTypeList = new ArrayList<>();
                    jsonNode.withArray("type").elements().forEachRemaining(node -> vcDataTypeList.add(node.asText()));
                    JsonNode credentialSubject = jsonNode.path(CREDENTIAL_SUBJECT);


                    // Construct and return the CredentialsBasicInfo object.
                    return CredentialsBasicInfo.builder()
                            .id(vcId)
                            .vcType(vcDataTypeList)
                            .availableFormats(availableFormats)
                            .credentialSubject(credentialSubject)
                            .expirationDate(expirationDate)
                            .build();
                })
                .collectList(); // Collect the stream of CredentialsBasicInfo objects into a list.
    }



    /**
     * Extracts the DID from a Verifiable Credential.
     *
     * @param userEntity The user entity containing the VC.
     * @param vcId       The ID of the Verifiable Credential.
     */
    @Override
    public Mono<String> extractDidFromVerifiableCredential(String userEntity, String vcId) {
        // Defer the execution until subscription
        return serializeUserEntity(userEntity).flatMap(entity -> {

            List<VCAttribute> vcAttributes = entity.vcs().value();

            // Find the specified VC by ID and type, then wrap it in a Mono
            return Mono.justOrEmpty(vcAttributes.stream().filter(vc -> vc.id().equals(vcId) && vc.type().equals(VC_JSON)).findFirst())
                    // If the VC is not found, return an error Mono
                    .switchIfEmpty(Mono.error(new NoSuchVerifiableCredentialException("VC not found: " + vcId)))
                    // Extract the DID from the VC
                    .flatMap(vcToExtract -> {
                        try {
                            JsonNode credentialNode = objectMapper.convertValue(vcToExtract.value(), JsonNode.class);
                            JsonNode didNode = credentialNode.path(CREDENTIAL_SUBJECT).path("id");

                            // If the DID is missing in the VC, return an error Mono
                            if (didNode.isMissingNode()) {
                                return Mono.error(new NoSuchVerifiableCredentialException("DID not found in VC: " + vcId));
                            }

                            // Return the DID as a Mono<String>
                            return Mono.just(didNode.asText());
                        } catch (Exception e) {
                            // If an error occurs during processing, return an error Mono
                            return Mono.error(new RuntimeException("Error processing VC: " + vcId, e));
                        }
                    });
        });
    }

    /**
     * Deletes a Verifiable Credential and its associated DID from a user entity.
     *
     * @param userEntity The user entity from which the VC and DID will be deleted.
     * @param vcId       The ID of the Verifiable Credential to be deleted.
     * @param did        The DID associated with the VC to be deleted.
     */
    @Override
    public Mono<String> deleteVerifiableCredential(String userEntity, String vcId, String did) {
        return serializeUserEntity(userEntity).flatMap(entity -> {
            // Remove the associated DID from the user entity's DID list
            List<DidAttribute> updatedDids = entity.dids().value().stream().filter(didAttr -> !didAttr.value().equals(did)).toList();

            // Remove the credential from the user entity's VC list
            List<VCAttribute> updatedVCs = entity.vcs().value().stream().filter(vcAttribute -> !vcAttribute.id().equals(vcId)).toList();

            // Create a new UserEntity with the updated lists
            UserEntity updatedUserEntity = new UserEntity(entity.id(), entity.type(), new EntityAttribute<>(entity.dids().type(), updatedDids), new EntityAttribute<>(entity.vcs().type(), updatedVCs));
            return deserializeUserEntityToString(updatedUserEntity);
        }).doOnSuccess(updateEntity -> // Log the successful operation and return the updated entity
                log.info("Verifiable Credential with ID: {} and associated DID deleted successfully: {}", vcId, updateEntity));
    }

    /**
     * Retrieves Verifiable Credentials by format for a user entity.
     *
     * @param userEntity The user entity whose VCs are to be retrieved.
     * @param format     The format of the VCs to retrieve.
     */
    @Override
    public Mono<List<VCAttribute>> getVerifiableCredentialsByFormat(String userEntity, String format) {
        return serializeUserEntity(userEntity).flatMap(entity -> {
            // Filter VCAttributes based on the given format
            List<VCAttribute> filteredVCs = entity.vcs().value().stream().filter(vcAttribute -> vcAttribute.type().equals(format)).toList();

            // Return the filtered list of VCAttributes wrapped in a Mono
            return Mono.just(filteredVCs);
        });
    }

    /**
     * Extracts a list of VC types from a VC's JSON representation.
     *
     * @param jsonNode The JSON node representing the VC.
     */
    private Mono<List<String>> getVcTypeListFromVcJson(JsonNode jsonNode) {
        // Initialize an empty list to store the types.
        List<String> result = new ArrayList<>();

        // Check if the "type" field is present and is an array.
        if (jsonNode.has("type") && jsonNode.get("type").isArray()) {
            // Iterate through the array elements and add them to the result list.
            jsonNode.get("type").forEach(node -> result.add(node.asText()));
            // Return the result list wrapped in a Mono.
            return Mono.just(result);
        } else {
            // Log a warning or throw an exception if the "type" field is not present or is not an array.
            return Mono.error(new IllegalStateException("The 'type' field is missing or is not an array in the provided JSON node."));
        }
    }

    /**
     * Extracts a list of VC available format from a VC id.
     *
     * @param credentialId The id of the VC.
     */
    private Mono<List<String>> getAvailableFormatListById(String credentialId,String userEntity) {
        return serializeUserEntity(userEntity).flatMap(user -> {
            List<VCAttribute> vcAttributeList = user.vcs().value().stream().filter(vc -> vc.id().equals(credentialId)&& !vc.type().equals(VC_JSON)).toList();
            List<String> availableFormats = new ArrayList<>();
            vcAttributeList.forEach(cred -> availableFormats.add(cred.type()));
            return Mono.just(availableFormats);
        });
    }

    /**
     * Retrieves a specific Verifiable Credential by its ID and format for a user entity.
     *
     * @param userEntity The user entity whose VC is to be retrieved.
     * @param id         The ID of the Verifiable Credential to retrieve.
     * @param format     The format of the Verifiable Credential to retrieve.
     */
    @Override
    public Mono<String> getVerifiableCredentialByIdAndFormat(String userEntity, String id, String format) {
        return serializeUserEntity(userEntity).flatMap(entity -> {

            Optional<VCAttribute> optionalVcAttribute = entity.vcs().value().stream().filter(vc -> vc.id().equals(id) && vc.type().equals(format)).findFirst();

            if (optionalVcAttribute.isEmpty()) {
                String errorMessage = "No VCAttribute found for id " + id + " and format " + format;
                log.error(errorMessage);
                return Mono.error(new NoSuchElementException(errorMessage));
            }

            VCAttribute vcAttribute = optionalVcAttribute.get();

            Object value = vcAttribute.value();
            if (value instanceof String) {
                return Mono.just(value.toString());
            } else {
                try {
                    String jsonValue = objectMapper.writeValueAsString(value);
                    return Mono.just(jsonValue);
                } catch (JsonProcessingException e) {
                    log.error("Error processing VCAttribute value to JSON string", e);
                    return Mono.error(e);
                }
            }
        });
    }


    /**
     * Saves a Decentralized Identifier (DID) for a user entity.
     *
     * @param userEntity The user entity for which the DID will be saved.
     * @param did        The DID to be saved.
     * @param didMethod  The method of the DID to be saved.
     */
    @Override
    public Mono<String> saveDid(String userEntity, String did, String didMethod) {
        return serializeUserEntity(userEntity).flatMap(entity -> {
            DidAttribute newDid = new DidAttribute(didMethod, did);

            // Add the new DID to the list of existing DIDs
            List<DidAttribute> updatedDids = new ArrayList<>(entity.dids().value());
            updatedDids.add(newDid);

            // Construct the updated EntityAttribute for DIDs
            EntityAttribute<List<DidAttribute>> dids = new EntityAttribute<>(PROPERTY_TYPE, updatedDids);

            // Create the updated user entity with the new DID
            UserEntity updatedUserEntity = new UserEntity(entity.id(), entity.type(), dids, entity.vcs());
            return deserializeUserEntityToString(updatedUserEntity);
        }).doOnSuccess(entity -> log.info("DID saved successfully for user: {}", entity)).onErrorResume(e -> {
            log.error("Error while saving DID for user: " + userEntity, e);
            return Mono.error(e); // Re-throw the error to be handled upstream
        });
    }

    /**
     * Retrieves the Decentralized Identifiers (DIDs) for a user entity.
     *
     * @param userEntity The user entity whose DIDs are to be retrieved.
     * @return A Mono emitting a list of DIDs associated with the user entity.
     */
    @Override
    public Mono<List<String>> getDidsByUserEntity(String userEntity) {
        return serializeUserEntity(userEntity).flatMap(entity -> {
            // Extract the DIDs from the UserEntity
            List<String> dids = entity.dids().value().stream().map(DidAttribute::value).toList(); // Use Stream.toList() for an unmodifiable list

            // Log the operation result
            log.info("Fetched DIDs for user: {}", entity.id());
            // Return the list of DIDs
            return Mono.just(dids);
        });
    }

    /**
     * Deletes a selected Decentralized Identifier (DID) from a user entity.
     *
     * @param did        The DID to be deleted.
     * @param userEntity The user entity from which the DID will be deleted.
     */
    @Override
    public Mono<String> deleteSelectedDidFromUserEntity(String did, String userEntity) {
        return serializeUserEntity(userEntity).flatMap(entity -> {
            // Create a list of DIDs without the one to be deleted
            List<DidAttribute> originalDids = entity.dids().value();
            List<DidAttribute> updatedDids = originalDids.stream().filter(didAttr -> !didAttr.value().equals(did)).toList(); // Use Stream.toList() for an unmodifiable list

            // Check if the DID was found and deleted
            if (originalDids.size() == updatedDids.size()) {
                return Mono.error(new NoSuchDidException("DID not found: " + did));
            }

            // Create an updated UserEntity with the remaining DIDs
            UserEntity updatedUserEntity = new UserEntity(entity.id(), entity.type(), new EntityAttribute<>(entity.dids().type(), updatedDids), entity.vcs());

            // Log the operation result
            log.info("Deleted DID: {} for user: {}", did, entity.id());

            // Return the updated UserEntity wrapped in a Mono
            return deserializeUserEntityToString(updatedUserEntity);
        });
    }
    
}
