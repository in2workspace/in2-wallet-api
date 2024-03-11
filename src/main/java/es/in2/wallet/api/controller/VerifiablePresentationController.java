package es.in2.wallet.api.controller;

import es.in2.wallet.api.config.SwaggerConfig;
import es.in2.wallet.api.facade.AttestationExchangeServiceFacade;
import es.in2.wallet.api.facade.CredentialPresentationForTurnstileServiceFacade;
import es.in2.wallet.api.model.CredentialsBasicInfo;
import es.in2.wallet.api.model.VcSelectorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.api.util.ApplicationUtils.getCleanBearerToken;

@RestController
@RequestMapping("/api/vp")
@Slf4j
@RequiredArgsConstructor
public class VerifiablePresentationController {

    private final CredentialPresentationForTurnstileServiceFacade credentialPresentationForTurnstileServiceFacade;
    private final AttestationExchangeServiceFacade attestationExchangeServiceFacade;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Save DID",
            description = "Save a Decentralized Identifier (DID)",
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    public Mono<Void> createVerifiablePresentation(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                   @RequestBody VcSelectorResponse vcSelectorResponse) {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken ->
                        attestationExchangeServiceFacade.buildVerifiablePresentationWithSelectedVCs(processId, authorizationToken, vcSelectorResponse));
    }
    @PostMapping("/cbor")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Verifiable Presentation in CBOR format",
            description = "Create a Verifiable Presentation in CBOR format",
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    @ApiResponse(responseCode = "200", description = "Verifiable presentation retrieved successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request.")
    @ApiResponse(responseCode = "500", description = "Internal server error.")
    public Mono<String> createVerifiablePresentationInCborFormat(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody CredentialsBasicInfo credentialsBasicInfo) {
        log.debug("VerifiablePresentationController.createVerifiablePresentationInCborFormat()");

        String processId = UUID.randomUUID().toString();

        MDC.put("processId", processId);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken -> credentialPresentationForTurnstileServiceFacade.createVerifiablePresentationForTurnstile(processId, authorizationToken, credentialsBasicInfo));
    }
}
