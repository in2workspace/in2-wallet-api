package es.in2.wallet.api.ebsi.comformance.controller;

import es.in2.wallet.api.ebsi.comformance.facade.impl.EbsiCredentialIssuanceServiceFacadeImpl;
import es.in2.wallet.api.ebsi.comformance.model.CredentialOfferContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.api.util.MessageUtils.getCleanBearerToken;

@Slf4j
@RestController
@RequestMapping("/api/v2/request-credential")
@RequiredArgsConstructor
public class CredentialIssuanceController {

    private final EbsiCredentialIssuanceServiceFacadeImpl ebsiCredentialIssuanceServiceFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> requestVerifiableCredential(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                                @RequestBody CredentialOfferContent credentialOfferContent) {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken ->
                        ebsiCredentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, credentialOfferContent.credentialOfferUri()));
    }

}
