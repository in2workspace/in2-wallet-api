package es.in2.wallet.infrastructure.core.controller;

import es.in2.wallet.application.workflow.presentation.AttestationExchangeCommonWorkflow;
import es.in2.wallet.application.workflow.presentation.AttestationExchangeDOMEWorkflow;
import es.in2.wallet.application.workflow.presentation.AttestationExchangeTurnstileWorkflow;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
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

import static es.in2.wallet.domain.util.ApplicationRegexPattern.DOME_REDIRECT_URI_PATTERN;
import static es.in2.wallet.domain.util.ApplicationUtils.getCleanBearerToken;

@RestController
@RequestMapping("/api/v1/vp")
@Slf4j
@RequiredArgsConstructor
public class VerifiablePresentationController {

    private final AttestationExchangeTurnstileWorkflow attestationExchangeTurnstileWorkflow;
    private final AttestationExchangeCommonWorkflow attestationExchangeCommonWorkflow;
    private final AttestationExchangeDOMEWorkflow attestationExchangeDOMEWorkflow;
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
                .flatMap(authorizationToken ->{
                        // Since the attestation exchange of DOME does not follow the standard, we check if the content of the
                        // redirect_uri belongs to the DOME verifier in order to continue with their use case.
                        if (DOME_REDIRECT_URI_PATTERN.matcher(vcSelectorResponse.redirectUri()).matches()){
                            return attestationExchangeDOMEWorkflow.publishAuthorisationResponseWithSelectedVCs(processId,authorizationToken,vcSelectorResponse);
                        }

                        else {
                            return attestationExchangeCommonWorkflow.buildVerifiablePresentationWithSelectedVCs(processId, authorizationToken, vcSelectorResponse);
                        }

                }).doOnSuccess(aVoid -> log.info("Attestation exchange successful"));
    }
    @PostMapping("/cbor")
    @ResponseStatus(HttpStatus.CREATED)
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
                .flatMap(authorizationToken -> attestationExchangeTurnstileWorkflow.createVerifiablePresentationForTurnstile(processId, authorizationToken, credentialsBasicInfo));
    }
}
