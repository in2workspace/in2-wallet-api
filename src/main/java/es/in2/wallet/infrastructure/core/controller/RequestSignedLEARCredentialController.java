package es.in2.wallet.infrastructure.core.controller;

import es.in2.wallet.application.workflow.issuance.DeferredCredentialDomeProfileWorkflow;
import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.domain.util.ApplicationUtils.getCleanBearerAndUserIdFromToken;

@RestController
@RequestMapping("/api/v1/request-signed-credential")
@Slf4j
@RequiredArgsConstructor
public class RequestSignedLEARCredentialController {

    private final DeferredCredentialDomeProfileWorkflow deferredCredentialDomeProfileWorkflow;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Request a signed credential",
            description = "Request a signed credential of a determinate credential",
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    @ApiResponse(responseCode = "200", description = "Request completed.")
    @ApiResponse(responseCode = "400", description = "Invalid request.")
    @ApiResponse(responseCode = "500", description = "Internal server error.")

    public Mono<Void> getSignedCredential(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestParam String credentialId) {
        log.debug("VerifiableCredentialController.getVerifiableCredential()");

        String processId = UUID.randomUUID().toString();

        return getCleanBearerAndUserIdFromToken(authorizationHeader)
                .flatMap(userId ->
                        deferredCredentialDomeProfileWorkflow.requestDeferredCredential(processId,userId,credentialId));
    }

}
