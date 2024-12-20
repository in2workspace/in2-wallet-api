package es.in2.wallet.domain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.DomeVerifiablePresentation;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.model.VerifiablePresentation;
import es.in2.wallet.domain.service.DataService;
import es.in2.wallet.domain.service.PresentationService;
import es.in2.wallet.domain.service.SignerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static es.in2.wallet.domain.util.ApplicationConstants.*;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationServiceImpl implements PresentationService {

    private final ObjectMapper objectMapper;
    private final DataService dataService;
    private final BrokerService brokerService;
    private final SignerService signerService;

    private final AppConfig appConfig;

    /**
     * Creates and signs a Verifiable Presentation (VP) using the selected Verifiable Credentials (VCs).
     * This method retrieves the subject DID from the first VC, constructs an unsigned VP, and signs it.
     *
     * @param authorizationToken   The authorization token to identify the user.
     * @param vcSelectorResponse   The response containing the selected VCs for the VP.
     * @param nonce                A unique nonce for the VP.
     * @param audience             The intended audience of the VP.
     */
    @Override
    public Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse,String nonce, String audience) {
        return createSignedVerifiablePresentation(processId, authorizationToken, nonce, audience, vcSelectorResponse.selectedVcList(), VC_JWT);
    }

    /**
     * Creates and signs a Verifiable Presentation (VP) using the selected Verifiable Credential (VC).
     * This method retrieves the subject DID from the first VC, constructs an unsigned VP, and signs it.
     *
     * @param authorizationToken   The authorization token to identify the user.
     * @param credentialsBasicInfo The selected VC for the VP.
     * @param nonce                A unique nonce for the VP.
     * @param audience             The intended audience of the VP.
     */
    @Override
    public Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, CredentialsBasicInfo credentialsBasicInfo, String nonce, String audience) {
        return createSignedVerifiablePresentation(processId, authorizationToken, nonce, audience, List.of(credentialsBasicInfo), VC_CWT);
    }

    @Override
    public Mono<String> createEncodedVerifiablePresentationForDome(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse) {
        return createVerifiablePresentationForDome(processId, authorizationToken,vcSelectorResponse);
    }

    private Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken,String nonce, String audience, List<CredentialsBasicInfo> selectedVcList, String format) {
        log.info("Starting to create Signed Verifiable Presentation for processId: {}", processId);

        // Step 1: Get User ID from the authorization token
        return getUserIdFromToken(authorizationToken)
                .doOnSubscribe(subscription -> log.debug("Getting user ID from the authorization token for processId: {}", processId))
                .flatMap(userId -> {
                            log.debug("User ID obtained successfully for processId: {}", processId);

                            // Step 2: Get Verifiable Credentials in JWT format
                            return getVerifiableCredentials(processId,userId,selectedVcList, VC_JWT)
                                    .doOnSubscribe(sub -> log.debug("Fetching Verifiable Credentials in JWT format for processId: {}", processId))
                                    .flatMap(verifiableCredentialsListJWT -> {
                                                log.debug("Successfully fetched Verifiable Credentials in JWT format for processId: {}", processId);

                                                // Step 3: Extract DID from the first Verifiable Credential
                                                return getSubjectDidFromTheFirstVcOfTheList(verifiableCredentialsListJWT)
                                                        .doOnSubscribe(sub -> log.debug("Extracting DID from the first Verifiable Credential for processId: {}", processId))
                                                        .flatMap(did -> {
                                                                    log.debug("Successfully extracted DID for processId: {}", processId);

                                                                    // Step 4: Get Verifiable Credentials in the selected format
                                                                    return getVerifiableCredentials(processId,userId,selectedVcList, format)
                                                                            .doOnSubscribe(sub -> log.debug("Fetching Verifiable Credentials in {} format for processId: {}", format, processId))
                                                                            .flatMap(verifiableCredentialsList -> // Create the unsigned verifiable presentation
                                                                                    {
                                                                                        log.debug("Successfully fetched Verifiable Credentials in {} format for processId: {}", format, processId);

                                                                                        // Step 5: Create unsigned Verifiable Presentation for signing
                                                                                        return createUnsignedPresentationForSigning(verifiableCredentialsList, did,nonce,audience)
                                                                                                .doOnSubscribe(sub -> log.debug("Creating unsigned Verifiable Presentation for processId: {}", processId))
                                                                                                .flatMap(document -> {
                                                                                                    log.debug("Successfully created unsigned Verifiable Presentation for processId: {}", processId);

                                                                                                    // Step 6: Build JWT for Verifiable Presentation
                                                                                                    return signerService.buildJWTSFromJsonNode(document, did, "vp")
                                                                                                            .doOnSubscribe(sub -> log.debug("Building JWT for Verifiable Presentation for processId: {}", processId))
                                                                                                            .flatMap(jwt -> {
                                                                                                                log.debug("Successfully built JWT for Verifiable Presentation for processId: {}", processId);

                                                                                                                // Step 7: Encode the presentation
                                                                                                                return encodePresentation(jwt)
                                                                                                                        .doOnSubscribe(sub -> log.debug("Encoding Verifiable Presentation for processId: {}", processId))
                                                                                                                        .doOnSuccess(encodedPresentation -> log.info("ProcessID: {} - Verifiable Presentation created successfully: {}", processId, encodedPresentation))
                                                                                                                        .doOnError(error -> log.error("Error occurred while encoding Verifiable Presentation for processId: {}: {}", processId, error.getMessage()));
                                                                                                            });
                                                                                                });
                                                                                    });
                                                                }
                                                                );
                                            }
                                            )
                                            // Handle errors
                                            .onErrorResume(e -> {
                                                log.warn("Error in creating Verifiable Presentation: ", e);
                                                return Mono.error(e);
                                            });
                        }
                        );
    }

    private Mono<String> createVerifiablePresentationForDome(String processId, String authorizationToken,VcSelectorResponse vcSelectorResponse) {
        return  getUserIdFromToken(authorizationToken)
                .flatMap(userId ->getVerifiableCredentials(processId,userId,vcSelectorResponse.selectedVcList(), VC_JSON)
                                .flatMap(verifiableCredentialsList -> createDomePresentation(verifiableCredentialsList, vcSelectorResponse.nonce()))
                                .flatMap(this::encodePresentation)
                        )
                        // Log success
                        .doOnSuccess(verifiablePresentation -> log.info("ProcessID: {} - DOME Verifiable Presentation created successfully: {}", processId, verifiablePresentation))
                        // Handle errors
                        .onErrorResume(e -> {
                            log.warn("Error in creating Verifiable Presentation: ", e);
                            return Mono.error(e);
                        });
    }
    /**
     * Retrieves a list of Verifiable Credential JWTs based on the VCs selected in the VcSelectorResponse.
     *
     * @param selectedVcList       The selected VCs.
     * @param format               The format of the VCs
     */
    private Mono<List<String>> getVerifiableCredentials(String processId, String userId, List<CredentialsBasicInfo> selectedVcList, String format) {
        return Flux.fromIterable(selectedVcList)
                .flatMap(credential -> brokerService.getCredentialByIdAndUserId(processId,credential.id(),userId))
                .flatMap(credentialEntity -> dataService.getVerifiableCredentialOnRequestedFormat(credentialEntity,format))
                .collectList();
    }

    /**
     * Extracts the subject DID from the first Verifiable Credential in the list.
     *
     * @param verifiableCredentialsList The list of VC JWTs.
     */
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

    /**
     * Creates an unsigned Verifiable Presentation containing the selected VCs.
     *
     * @param vcs       The list of VC JWTs to include in the VP.
     * @param holderDid The DID of the holder of the VPs.
     * @param nonce     A unique nonce for the VP.
     * @param audience  The intended audience of the VP.
     */
    private Mono<JsonNode> createUnsignedPresentationForSigning(
            List<String> vcs,
            String holderDid,
            String nonce,
            String audience) {
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
            Instant expirationTime = issueTime.plus(appConfig.getCredentialPresentationExpirationTime(), ChronoUnit.valueOf(appConfig.getCredentialPresentationExpirationUnit().toUpperCase()));
            Map<String, Object> vpParsed = JWTClaimsSet.parse(objectMapper.writeValueAsString(vpBuilder)).getClaims();
            JWTClaimsSet.Builder payloadBuilder = new JWTClaimsSet.Builder()
                    .issuer(holderDid)
                    .subject(holderDid)
                    .notBeforeTime(java.util.Date.from(issueTime))
                    .expirationTime(java.util.Date.from(expirationTime))
                    .issueTime(java.util.Date.from(issueTime))
                    .jwtID(id)
                    .claim("vp", vpParsed)
                    .claim("nonce", nonce);

            if (audience != null) {
                payloadBuilder.audience(audience);
            }

            JWTClaimsSet payload = payloadBuilder.build();
            log.debug(payload.toString());
            return objectMapper.readTree(payload.toString());
        });
    }

    /**
     * Creates an unsigned Verifiable Presentation containing the selected VCs.
     *
     * @param vcs       The list of VC JWTs to include in the VP.
     */
    private Mono<String> createDomePresentation(
            List<String> vcs, String nonce) {
        return Mono.fromCallable(() -> {
            List<JsonNode> vcsJsonList = vcs.stream()
                    .map(vc -> {
                        try {
                            return objectMapper.readTree(vc);
                        } catch (Exception e) {
                            throw new ParseErrorException("Error parsing VC string to JsonNode");
                        }
                    })
                    .toList();

            DomeVerifiablePresentation vp = DomeVerifiablePresentation
                    .builder()
                    .holder("did:my:wallet")
                    .nonce(nonce)
                    .context(List.of(JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1))
                    .type(List.of(VERIFIABLE_PRESENTATION))
                    .verifiableCredential(vcsJsonList)
                    .build();

            return objectMapper.writeValueAsString(vp);

        });
    }

    private Mono<String> encodePresentation(String vp) {
        return Mono.fromCallable(() -> Base64.getUrlEncoder().withoutPadding().encodeToString(vp.getBytes()));
    }


}
