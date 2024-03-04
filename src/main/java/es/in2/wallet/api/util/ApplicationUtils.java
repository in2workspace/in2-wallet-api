package es.in2.wallet.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static es.in2.wallet.api.util.MessageUtils.BEARER;

@Slf4j
public class ApplicationUtils {
    private ApplicationUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final WebClient WEB_CLIENT = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(false)))
            .build();
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
    public static Mono<String> getCleanBearerAndUserIdFromToken(String authorizationHeader) {
        return Mono.just(authorizationHeader)
                .filter(header -> header.startsWith(BEARER))
                .map(header -> header.substring(7))
                .flatMap(token -> {
                    try {
                        SignedJWT parsedVcJwt = SignedJWT.parse(token);
                        JsonNode jsonObject = new ObjectMapper().readTree(parsedVcJwt.getPayload().toString());
                        return Mono.just(jsonObject.get("sub").asText());
                    } catch (ParseException | JsonProcessingException e) {
                        return Mono.error(e);
                    }
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid")));
    }

    public static Mono<Map<String, String>> extractAllQueryParams(String url) {
        log.debug(url);
        return Mono.fromCallable(() -> {
            Map<String, String> params = new HashMap<>();
            try {
                URI uri = new URI(url);
                String query = uri.getQuery();
                if (query != null) {
                    String[] pairs = query.split("&");
                    for (String pair : pairs) {
                        int idx = pair.indexOf("=");
                        String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                        params.put(key, value);
                    }
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
            }
            return params;
        });
    }

    /**
     * Extracts the response type from a JWT, which determines the next steps in the authorization flow.
     * The response type can dictate whether an ID token or VP token flow should be initiated.
     */
    public static Mono<String> extractResponseType(String jwt){
        return Mono.fromCallable(() -> {
            log.debug(jwt);
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            return signedJwt.getJWTClaimsSet().getClaim("response_type").toString();
        });
    }
}
