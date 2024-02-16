package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.api.ebsi.comformance.service.AuthorisationResponseService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorisationResponseServiceImpl implements AuthorisationResponseService {
    private final ObjectMapper objectMapper;
    @Override
    public Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata, Map<String, String> params){
        if(Objects.equals(params.get("state"), GLOBAL_STATE)){
            String code = params.get("code");
            List<Map.Entry<String, String>> headers = new ArrayList<>();
            headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));
            // Build URL encoded form data request body
            Map<String, String> formDataMap = Map.of("grant_type", AUTH_CODE_GRANT_TYPE, "client_id", did, "code" ,code, "code_verifier",codeVerifier);
            String xWwwFormUrlencodedBody = formDataMap.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            return postRequest(authorisationServerMetadata.tokenEndpoint(),headers,xWwwFormUrlencodedBody)
                    .flatMap(response ->{
                        try {
                            TokenResponse tokenResponse = objectMapper.readValue(response, TokenResponse.class);
                            return Mono.just(tokenResponse);
                        } catch (Exception e) {
                            log.error("Error while deserializing TokenResponse from the auth server", e);
                            return Mono.error(new FailedDeserializingException("Error while deserializing TokenResponse: " + response));
                        }

                    })
                    .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Token Request")));
        }
        else {
            return Mono.error(new IllegalArgumentException("state mismatch"));
        }
    }
}
