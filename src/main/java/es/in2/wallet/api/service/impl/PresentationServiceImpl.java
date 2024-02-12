package es.in2.wallet.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.model.VcSelectorResponse;
import es.in2.wallet.api.model.VerifiablePresentation;
import es.in2.wallet.api.service.PresentationService;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.api.service.UserDataService;
import es.in2.wallet.broker.service.BrokerService;
import es.in2.wallet.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static es.in2.wallet.api.util.MessageUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresentationServiceImpl implements PresentationService {
    private final ObjectMapper objectMapper;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final VaultService vaultService;
    private final SignerService signerService;

    @Override
    public Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse) {
        // Get the subject DID from the first credential in the list
        return  getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId,userId))
                .flatMap(optionalEntity -> optionalEntity
                        .map(entity -> getVerifiableCredentials(entity,vcSelectorResponse))
                        .orElseGet(() -> Mono.error(new RuntimeException("Failed to retrieve entity."))
                )
                .flatMap(verifiableCredentialsList -> getSubjectDidFromTheFirstVcOfTheList(verifiableCredentialsList)
                        .flatMap(did ->
                                // Create the unsigned verifiable presentation
                                createUnsignedPresentation(verifiableCredentialsList, did)
                                        .flatMap(document -> vaultService.getSecretByKey(did,PRIVATE_KEY_TYPE)
                                            .flatMap(privateKey -> signerService.buildJWTSFromJsonNode(document,did,"vp",privateKey)))
                        )
                )
                        // Log success
                        .doOnSuccess(verifiablePresentation -> log.info("ProcessID: {} - Verifiable Presentation created successfully: {}", processId, verifiablePresentation))
                    // Handle errors
                    .onErrorResume(e -> {
                        log.error("Error in creating Verifiable Presentation: ", e);
                        return Mono.error(e);
                    })
                );
    }

    private Mono<List<String>> getVerifiableCredentials(String entity, VcSelectorResponse vcSelectorResponse) {
        return Flux.fromIterable(vcSelectorResponse.selectedVcList())
                .flatMap(verifiableCredential -> userDataService.getVerifiableCredentialByIdAndFormat(entity,verifiableCredential.id(),"vc_jwt"))
                .collectList();
    }
    private Mono<String> getSubjectDidFromTheFirstVcOfTheList(List<String> verifiableCredentialsList) {
        return Mono.fromCallable(() -> {
            // Check if the list is not empty
            try {
                if (!verifiableCredentialsList.isEmpty()) {
                    // Get the first verifiable credential's JWT and parse it
                    String verifiableCredential = verifiableCredentialsList.get(0);
                    SignedJWT parsedVerifiableCredential = SignedJWT.parse(verifiableCredential);
                    // Extract the subject DID from the JWT claims
                    return (String) parsedVerifiableCredential.getJWTClaimsSet().getClaim("sub");
                } else {
                    // Throw an exception if the credential list is empty
                    throw new NoSuchElementException("Verifiable credentials list is empty");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error obtaining the subject DID from the verifiable credential" + e);
            }
        });
    }

    private Mono<JsonNode> createUnsignedPresentation(
            List<String> vcs,
            String holderDid) {
        return Mono.fromCallable(() -> {
            String id = "urn:uuid:" + UUID.randomUUID();

            VerifiablePresentation vpBuilder = VerifiablePresentation
                    .builder()
                    .id(id)
                    .holder(holderDid)
                    .context(List.of(JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1))
                    .type(List.of(VERIFIABLE_PRESENTATION))
                    .verifiableCredential(vcs)
                    .build();

            Instant issueTime = Instant.now();
            Instant expirationTime = issueTime.plus(10, ChronoUnit.DAYS);
            Map<String, Object> vpParsed = JWTClaimsSet.parse(objectMapper.writeValueAsString(vpBuilder)).getClaims();
            JWTClaimsSet payload = new JWTClaimsSet.Builder()
                    .issuer(holderDid)
                    .subject(holderDid)
                    .notBeforeTime(java.util.Date.from(issueTime))
                    .expirationTime(java.util.Date.from(expirationTime))
                    .issueTime(java.util.Date.from(issueTime))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("vp", vpParsed)
                    .build();
            log.debug(payload.toString());
            return objectMapper.readTree(payload.toString());
        });
    }

}
