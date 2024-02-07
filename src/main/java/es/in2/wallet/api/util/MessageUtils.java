package es.in2.wallet.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class MessageUtils {

    private MessageUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final WebClient WEB_CLIENT = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(false)))
            .build();
    public static final String RESOURCE_UPDATED_MESSAGE = "ProcessId: {}, Resource updated successfully.";
    public static final String ERROR_UPDATING_RESOURCE_MESSAGE = "Error while updating resource: {}";
    public static final String ENTITY_PREFIX = "/urn:entities:userId:";
    public static final String ATTRIBUTES = "/attrs:";
    public static final String PROCESS_ID = "ProcessId";
    public static final String PRIVATE_KEY_TYPE = "privateKey";
    public static final String PUBLIC_KEY_TYPE = "publicKey";
    public static final String DID = "did";
    public static final long MSB = 0x80L;
    public static final long LSB = 0x7FL;
    public static final long MSBALL = 0xFFFFFF80L;
    public static final String PRE_AUTH_CODE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    public static final String BEARER = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CREDENTIALS = "credentials";
    public static final String JWT_PROOF_CLAIM = "openid4vci-proof+jwt";
    public static final Pattern PROOF_DOCUMENT_PATTERN = Pattern.compile("proof");
    public static final Pattern VP_DOCUMENT_PATTERN = Pattern.compile("vp");
    public static final Pattern VC_DOCUMENT_PATTERN = Pattern.compile("vc");
    public static final String JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1 = "https://www.w3.org/2018/credentials/v1";
    public static final String VERIFIABLE_PRESENTATION = "VerifiablePresentation";

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_URL_ENCODED_FORM = "application/x-www-form-urlencoded";

    public static final String VC_JWT = "vc_jwt";
    public static final String VC_JSON = "vc_json";
    public static final String PROPERTY_TYPE = "Property";
    public static final String CREDENTIAL_SUBJECT = "credentialSubject";

    public static Mono<String> postRequest(String url, List<Map.Entry<String, String>> headers, String body) {
        return WEB_CLIENT.post()
                .uri(url)
                .headers(httpHeaders -> headers.forEach(entry -> httpHeaders.add(entry.getKey(), entry.getValue())))
                .bodyValue(body)
                .exchangeToMono(response -> {
                    if (response.statusCode().is3xxRedirection()) {
                        return Mono.just(Objects.requireNonNull(response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION)));
                    } else {
                        return response.bodyToMono(String.class);
                    }
                });
    }

    public static Mono<String> getRequest(String url, List<Map.Entry<String, String>> headers) {
        return WEB_CLIENT.get()
                .uri(URI.create(url))
                .headers(httpHeaders -> headers.forEach(entry -> httpHeaders.add(entry.getKey(), entry.getValue())))
                .exchangeToMono(response -> {
                    if (response.statusCode().is3xxRedirection()) {
                        return Mono.just(Objects.requireNonNull(response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION)));
                    } else {
                        return response.bodyToMono(String.class);
                    }
                });
    }
    public static Mono<String> getCleanBearerToken(String authorizationHeader) {
        return Mono.just(authorizationHeader)
                .filter(header -> header.startsWith(BEARER))
                .map(header -> header.replace(BEARER, "").trim())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid")));
    }
    public static Mono<String> getUserIdFromToken(String authorizationToken) {
        try {
            SignedJWT parsedVcJwt = SignedJWT.parse(authorizationToken);
            JsonNode jsonObject = new ObjectMapper().readTree(parsedVcJwt.getPayload().toString());
            return Mono.just(jsonObject.get("sub").asText());
        } catch (ParseException | JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
