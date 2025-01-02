package es.in2.wallet.infrastructure.ebsi.controller;

import es.in2.wallet.application.workflow.issuance.CredentialIssuanceCommonWorkflow;
import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
import es.in2.wallet.application.workflow.issuance.CredentialIssuanceEbsiWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.domain.util.ApplicationUtils.getCleanBearerToken;


@Slf4j
@RestController
@RequestMapping("/api/v1/openid-credential-offer")
@RequiredArgsConstructor
public class OpenidCredentialOfferController {

    private final CredentialIssuanceEbsiWorkflow ebsiCredentialIssuanceServiceFacade;
    private final CredentialIssuanceCommonWorkflow commonCredentialIssuanceServiceFacade;


    /**
     * Processes a request for a verifiable credential when the credential offer is received via a redirect.
     * This endpoint is designed to handle the scenario where a user is redirected to this service with a credential
     * offer URI, as opposed to receiving the offer directly from scanning a QR code.
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    public Mono<Void> requestOpenidCredentialOffer(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                   @RequestParam String credentialOfferUri) {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken -> {
                    if (credentialOfferUri.contains("ebsi")) {
                        return ebsiCredentialIssuanceServiceFacade.identifyAuthMethod(
                                processId, authorizationToken, credentialOfferUri);
                    } else {
                        return commonCredentialIssuanceServiceFacade.identifyAuthMethod(
                                processId, authorizationToken, credentialOfferUri);
                    }
                });
    }

}
