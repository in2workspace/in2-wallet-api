package es.in2.wallet.api.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.NoSuchDidException;
import es.in2.wallet.api.exception.NoSuchVerifiableCredentialException;
import es.in2.wallet.api.exception.ParseErrorException;
import es.in2.wallet.api.model.*;
import es.in2.wallet.api.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.*;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataServiceImpl implements UserDataService {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<String> createUserEntity(String id) {
        // Construct the UserEntity
        UserEntity userEntity = new UserEntity(
                "urn:entities:userId:" + id,
                "userEntity",
                new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>()),
                new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>())
        );

        // Log the creation of the entity
        log.debug("UserEntity created for: {}", id);

        return deserializeUserEntityToString(userEntity);
    }
    private Mono<String> deserializeUserEntityToString(UserEntity userEntity){
        try {
            String user = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userEntity);
            log.debug(user);
            return Mono.just(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userEntity));

        }
        catch (JsonProcessingException e) {
            return Mono.error(new ParseErrorException("Error deserializing UserEntity: " + e));
        }
    }
    private Mono<UserEntity> serializeUserEntity(String userEntity){
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
            log.error("Error while deserializing UserEntity: " , e);
            return Mono.error(new ParseErrorException("Error deserializing UserEntity: "));
        }
    }
    @Override
    public Mono<String> saveVC(String userEntity, String vcJwt) {
        // Extract the JSON content from the VC JWT.
        return serializeUserEntity(userEntity)
                .flatMap(entity ->extractVcJsonFromVcJwt(vcJwt)
                .flatMap(vcJson -> extractVerifiableCredentialIdFromVcJson(vcJson)
                        .flatMap(vcId -> {
                            // Create new VCAttributes for both the VC JWT and its JSON content.
                            VCAttribute newVCJwt = new VCAttribute(vcId, VC_JWT, vcJwt);
                            VCAttribute newVCJson = new VCAttribute(vcId, VC_JSON, vcJson);

                            // Update the list of VCAttributes in the UserEntity.
                            List<VCAttribute> updatedVCs = new ArrayList<>(entity.vcs().value());
                            updatedVCs.add(newVCJwt);
                            updatedVCs.add(newVCJson);

                            // Create a new EntityAttribute for the updated list of VCAttributes.
                            EntityAttribute<List<VCAttribute>> vcs = new EntityAttribute<>(entity.vcs().type(), updatedVCs);

                            // Return the updated UserEntity with the new list of VCAttributes.
                            UserEntity user = UserEntity.builder().id(entity.id()).type(entity.type()).vcs(vcs).dids(entity.dids()).build();
                            return deserializeUserEntityToString(user);
                        }))
                // Log a success message when the VC has been successfully added to the UserEntity.
                .doOnSuccess(updatedUserEntity -> log.info("Verifiable Credential saved successfully: {}", updatedUserEntity)));
    }

    private Mono<JsonNode> extractVcJsonFromVcJwt(String vcJwt) {
        return Mono.fromCallable(() -> SignedJWT.parse(vcJwt))
                .onErrorMap(ParseException.class, e -> new ParseErrorException("Error while parsing VC JWT: " + e.getMessage()))
                .handle((parsedVcJwt, sink) -> {
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

    private Mono<String> extractVerifiableCredentialIdFromVcJson(JsonNode vcJson) {
        return Mono.justOrEmpty(vcJson.get("id").asText())
                .flatMap(vcId -> {
                    if (vcId == null || vcId.trim().isEmpty()) {
                        log.error("The Verifiable Credential does not contain an ID.");
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Verifiable Credential does not contain an ID."));
                    }
                    log.debug("Verifiable Credential ID extracted: {}", vcId);
                    return Mono.just(vcId);
                });
    }

    @Override
    public Mono<List<CredentialsBasicInfo>> getUserVCsInJson(String userEntity) {
        return serializeUserEntity(userEntity)
                .flatMapMany(user -> Flux.fromIterable(user.vcs().value()))
                .filter(vcAttribute -> VC_JSON.equals(vcAttribute.type()))
                .flatMap(item -> {
                    LinkedHashMap<?, ?> vcDataValue = (LinkedHashMap<?, ?>) item.value();
                    JsonNode jsonNode = objectMapper.convertValue(vcDataValue, JsonNode.class);

                    return getVcTypeListFromVcJson(jsonNode)
                            .map(vcTypeList -> new CredentialsBasicInfo(
                                    item.id(),
                                    vcTypeList,
                                    jsonNode.get(CREDENTIAL_SUBJECT)
                            ));
                })
                .collectList()
                .onErrorResume(NoSuchVerifiableCredentialException.class, Mono::error);
    }

    @Override
    public Mono<List<CredentialsBasicInfo>> getSelectableVCsByVcTypeList(List<String> vcTypeList, String userEntity) {
        // Primero, obtén todas las credenciales disponibles como una lista.
        return getVerifiableCredentialsByFormat(userEntity, VC_JSON)
                .flatMapMany(Flux::fromIterable) // Convierte la lista en un Flux para procesar cada VC.
                .collectList() // Recolecta todas las VC en una lista para procesarlas.
                .flatMap(vcs -> {
                    // Prepara una lista para recoger las credenciales que coincidan.
                    List<CredentialsBasicInfo> matchingVCs = new ArrayList<>();

                    // Itera sobre la lista de tipos de VC dada.
                    for (String vcType : vcTypeList) {
                        // Para cada tipo, busca en las credenciales del usuario.
                        for (VCAttribute vcAttribute : vcs) {
                            // Convierte el valor de la VC a JsonNode.
                            JsonNode jsonNode = objectMapper.convertValue(vcAttribute.value(), JsonNode.class);

                            // Obtiene la lista de tipos de la VC.
                            List<String> vcDataTypeList = new ArrayList<>();
                            jsonNode.get("type").forEach(node -> vcDataTypeList.add(node.asText()));

                            // Verifica si alguno de los tipos coincide con el tipo actual en vcTypeList.
                            if (vcDataTypeList.contains(vcType)) {
                                // Si hay una coincidencia, crea un objeto CredentialsBasicInfo y lo añade a la lista.
                                CredentialsBasicInfo dto = new CredentialsBasicInfo(
                                        jsonNode.get("id").asText(),
                                        vcDataTypeList,
                                        jsonNode.get("credentialSubject")
                                );
                                matchingVCs.add(dto);
                                // Rompe el bucle interno después de encontrar la primera coincidencia para este tipo.
                                break;
                            }
                        }
                    }
                    // Devuelve la lista de credenciales coincidentes envuelta en un Mono.
                    log.debug(matchingVCs.toString());
                    return Mono.just(matchingVCs);
                });
    }



    @Override
    public Mono<String> extractDidFromVerifiableCredential(String userEntity, String vcId) {
        // Defer the execution until subscription
        return serializeUserEntity(userEntity)
                .flatMap(entity -> {

                    List<VCAttribute> vcAttributes = entity.vcs().value();

                    // Find the specified VC by ID and type, then wrap it in a Mono
                    return Mono.justOrEmpty(vcAttributes.stream()
                                    .filter(vc -> vc.id().equals(vcId) && vc.type().equals(VC_JSON))
                                    .findFirst())
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

    @Override
    public Mono<String> deleteVerifiableCredential(String userEntity, String vcId, String did) {
        return serializeUserEntity(userEntity).flatMap(
                entity ->{
                    // Remove the associated DID from the user entity's DID list
                    List<DidAttribute> updatedDids = entity.dids().value().stream()
                            .filter(didAttr -> !didAttr.value().equals(did))
                            .toList();

                    // Remove the credential from the user entity's VC list
                    List<VCAttribute> updatedVCs = entity.vcs().value().stream()
                            .filter(vcAttribute -> !vcAttribute.id().equals(vcId))
                            .toList();

                    // Create a new UserEntity with the updated lists
                    UserEntity updatedUserEntity = new UserEntity(
                            entity.id(),
                            entity.type(),
                            new EntityAttribute<>(entity.dids().type(), updatedDids),
                            new EntityAttribute<>(entity.vcs().type(), updatedVCs)
                    );
                    return deserializeUserEntityToString(updatedUserEntity);
                })
                .doOnSuccess(updateEntity -> // Log the successful operation and return the updated entity
                        log.info("Verifiable Credential with ID: {} and associated DID deleted successfully: {}", vcId, updateEntity));
    }


    @Override
    public Mono<List<VCAttribute>> getVerifiableCredentialsByFormat(String userEntity, String format) {
        return serializeUserEntity(userEntity)
                .flatMap(entity -> {
                    // Filter VCAttributes based on the given format
                    List<VCAttribute> filteredVCs = entity.vcs().value().stream()
                            .filter(vcAttribute -> vcAttribute.type().equals(format))
                            .toList();

                    // Return the filtered list of VCAttributes wrapped in a Mono
                    return Mono.just(filteredVCs);
                });
    }

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

    @Override
    public Mono<String> getVerifiableCredentialByIdAndFormat(String userEntity, String id, String format) {
        return serializeUserEntity(userEntity)
                .flatMap(entity -> {

                    Optional<VCAttribute> optionalVcAttribute = entity.vcs().value().stream()
                            .filter(vc -> vc.id().equals(id) && vc.type().equals(format))
                            .findFirst();

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



    @Override
    public Mono<String> saveDid(String userEntity, String did, String didMethod) {
        return serializeUserEntity(userEntity)
                .flatMap(entity -> {
                    // Create new DidAttribute for the provided DID
                    DidAttribute newDid = new DidAttribute(didMethod, did);

                    // Add the new DID to the list of existing DIDs
                    List<DidAttribute> updatedDids = new ArrayList<>(entity.dids().value());
                    updatedDids.add(newDid);

                    // Construct the updated EntityAttribute for DIDs
                    EntityAttribute<List<DidAttribute>> dids = new EntityAttribute<>(PROPERTY_TYPE, updatedDids);

                    // Create the updated user entity with the new DID
                    UserEntity updatedUserEntity = new UserEntity(
                            entity.id(),
                            entity.type(),
                            dids,
                            entity.vcs()
                    );
                    return deserializeUserEntityToString(updatedUserEntity);
                })
                .doOnSuccess(entity -> log.info("DID saved successfully for user: {}", entity))
                .onErrorResume(e -> {
                    log.error("Error while saving DID for user: " + userEntity, e);
                    return Mono.error(e); // Re-throw the error to be handled upstream
                });
    }

    @Override
    public Mono<List<String>> getDidsByUserEntity(String userEntity) {
        return serializeUserEntity(userEntity)
                .flatMap(entity -> {
                    // Extract the DIDs from the UserEntity
                    List<String> dids = entity.dids().value().stream()
                            .map(DidAttribute::value)
                            .toList(); // Use Stream.toList() for an unmodifiable list

                    // Log the operation result
                    log.info("Fetched DIDs for user: {}", entity.id());
                    // Return the list of DIDs
                    return Mono.just(dids);
                });
    }


    @Override
    public Mono<String> deleteSelectedDidFromUserEntity(String did, String userEntity) {
        return serializeUserEntity(userEntity)
                .flatMap(entity -> {
                    // Create a list of DIDs without the one to be deleted
                    List<DidAttribute> originalDids = entity.dids().value();
                    List<DidAttribute> updatedDids = originalDids.stream()
                            .filter(didAttr -> !didAttr.value().equals(did))
                            .toList(); // Use Stream.toList() for an unmodifiable list

                    // Check if the DID was found and deleted
                    if (originalDids.size() == updatedDids.size()) {
                        return Mono.error(new NoSuchDidException("DID not found: " + did));
                    }

                    // Create an updated UserEntity with the remaining DIDs
                    UserEntity updatedUserEntity = new UserEntity(
                            entity.id(),
                            entity.type(),
                            new EntityAttribute<>(entity.dids().type(), updatedDids),
                            entity.vcs()
                    );

                    // Log the operation result
                    log.info("Deleted DID: {} for user: {}", did, entity.id());

                    // Return the updated UserEntity wrapped in a Mono
                    return deserializeUserEntityToString(updatedUserEntity);
                });
    }


}
