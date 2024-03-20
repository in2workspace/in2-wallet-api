package es.in2.wallet.domain.service.impl;

import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.domain.exception.FailedCommunicationException;
import es.in2.wallet.domain.model.AuthorizationRequest;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.service.DomeVpTokenService;
import es.in2.wallet.domain.service.PresentationService;
import es.in2.wallet.domain.service.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static es.in2.wallet.domain.util.ApplicationUtils.postRequest;
import static es.in2.wallet.domain.util.MessageUtils.CONTENT_TYPE;
import static es.in2.wallet.domain.util.MessageUtils.CONTENT_TYPE_URL_ENCODED_FORM;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomeVpTokenServiceImpl implements DomeVpTokenService {
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final PresentationService presentationService;

    /**
     * Initiates the process to exchange the authorization token and JWT for a VP Token Request,
     * logging the authorization response with the code upon success.
     *
     * @param processId An identifier for the process, used for logging.
     * @param authorizationToken The authorization token provided by the client.
     */
    @Override
    public Mono<Void> getVpRequest(String processId, String authorizationToken, AuthorizationRequest authorizationRequest) {
        return completeVpTokenExchange(processId, authorizationToken, authorizationRequest)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }


    /**
     * Completes the VP Token exchange process by building a VP token response and extracting query parameters from it.
     */
    private Mono<Void> completeVpTokenExchange(String processId, String authorizationToken, AuthorizationRequest authorizationRequest) {
        return buildVpTokenResponse(processId,authorizationToken,authorizationRequest);
    }

    private Mono<Void> buildVpTokenResponse(String processId, String authorizationToken, AuthorizationRequest authorizationRequest) {
        List<String> vcTypeList = List.of("LegalPersonCredential");

        return buildBase64VerifiablePresentationByVcTypeList(processId, authorizationToken, vcTypeList)
                .flatMap(vp -> sendVpTokenResponse(vp,authorizationRequest));
    }
    /**
     * Sends the VP Token response to the redirect URI specified in the JWT, as an application/x-www-form-urlencoded payload.
     * This includes the VP token, presentation submission, and state parameters.
     */
    private Mono<Void> sendVpTokenResponse(String vpToken, AuthorizationRequest authorizationRequest) {
            String body = "vp_token=" + vpToken;
            List<Map.Entry<String, String>> headers = new ArrayList<>();
            headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));

        String urlWithState = authorizationRequest.redirectUri() + "?state=" + authorizationRequest.state();

        return postRequest(urlWithState, headers,body)
                    .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Vp Token Response")))
                    .then();

    }

    /**
     * Builds a signed JWT Verifiable Presentation by extracting user data and credentials based on the VC type list provided.
     */
    private Mono<String> buildBase64VerifiablePresentationByVcTypeList(String processId, String authorizationToken, List<String> vcTypeList) {
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId, userId))
                .flatMap(optionalEntity -> optionalEntity
                        .map(entity ->
                                userDataService.getSelectableVCsByVcTypeList(vcTypeList, entity)
                                        .flatMap(list -> {
                                            log.debug(list.toString());
                                            VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().selectedVcList(list).build();
                                            return presentationService.createEncodedVerifiablePresentationForDome(processId, authorizationToken, vcSelectorResponse);
                                        })
                        )
                        .orElseGet(() ->
                                Mono.error(new RuntimeException("Entity not found for provided ID."))
                        )
                );
    }
}
