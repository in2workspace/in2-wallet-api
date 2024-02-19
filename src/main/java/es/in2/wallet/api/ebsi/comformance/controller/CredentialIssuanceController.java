package es.in2.wallet.api.ebsi.comformance.controller;

import es.in2.wallet.api.ebsi.comformance.facade.impl.EbsiCredentialServiceFacadeImpl;
import es.in2.wallet.api.ebsi.comformance.model.CredentialOfferContent;
import es.in2.wallet.api.facade.CredentialIssuanceServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.api.util.ApplicationUtils.getCleanBearerToken;
import static es.in2.wallet.api.util.MessageUtils.EBSI_CREDENTIAL_OFFER_PATTERN;


@Slf4j
@RestController
@RequestMapping("/api/v2/request-credential")
@RequiredArgsConstructor
public class CredentialIssuanceController {

    private final EbsiCredentialServiceFacadeImpl ebsiCredentialIssuanceServiceFacade;
    private final CredentialIssuanceServiceFacade credentialIssuanceServiceFacade;

    /**
     * Processes a request for a verifiable credential when the credential offer is received via a redirect.
     * This endpoint is designed to handle the scenario where a user is redirected to this service with a credential
     * offer URI, as opposed to receiving the offer directly from scanning a QR code.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> requestVerifiableCredential(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                                @RequestBody CredentialOfferContent credentialOfferContent) {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken -> {
                            if (EBSI_CREDENTIAL_OFFER_PATTERN.matcher(credentialOfferContent.credentialOfferUri()).matches()){
                                return ebsiCredentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, credentialOfferContent.credentialOfferUri());
                            }
                            else {
                                return credentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, credentialOfferContent.credentialOfferUri());
                            }
                        });
    }

}
