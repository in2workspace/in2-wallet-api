package es.in2.wallet.api;

import es.in2.wallet.api.facade.CredentialIssuanceServiceFacade;
import es.in2.wallet.api.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.api.util.MessageUtils.getCleanBearerToken;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/test")
@RequiredArgsConstructor
public class Controller {

    private final CredentialIssuanceServiceFacade credentialIssuanceServiceFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> registerUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody Message message) {
        String processId = UUID.randomUUID().toString();
        return getCleanBearerToken(authorizationHeader)
                .flatMap(token ->  credentialIssuanceServiceFacade.identifyAuthMethod(processId,token,message.url()));
    }

}
