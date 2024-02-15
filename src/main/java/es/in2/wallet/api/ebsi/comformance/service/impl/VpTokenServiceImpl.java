package es.in2.wallet.api.ebsi.comformance.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.ebsi.comformance.service.VpTokenService;
import es.in2.wallet.api.exception.FailedCommunicationException;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.exception.FailedSerializingException;
import es.in2.wallet.api.model.*;
import es.in2.wallet.api.service.PresentationService;
import es.in2.wallet.api.service.UserDataService;
import es.in2.wallet.api.util.MessageUtils;
import es.in2.wallet.broker.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static es.in2.wallet.api.util.MessageUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpTokenServiceImpl implements VpTokenService {
    private final ObjectMapper objectMapper;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final PresentationService presentationService;

    @Override
    public Mono<TokenResponse> getVpRequest(String processId, String authorizationToken, AuthorisationServerMetadata authorisationServerMetadata, String did, Tuple2<String, String> jwtAndCodeVerifier) {
        return completeTokenExchange(processId, authorizationToken, authorisationServerMetadata, did, jwtAndCodeVerifier)
                .doOnSuccess(tokenResponse -> log.info("ProcessID: {} - Token Response: {}", processId, tokenResponse));
    }

    /**
     * Completes the token exchange process using the provided parameters and code verifier.
     */
    private Mono<TokenResponse> completeTokenExchange(String processId, String authorizationToken,AuthorisationServerMetadata authorisationServerMetadata, String did, Tuple2<String, String> paramsAndCodeVerifier) {
        String jwt = paramsAndCodeVerifier.getT1();
        String codeVerifier = paramsAndCodeVerifier.getT2();
        return buildVpTokenResponse(processId,authorizationToken,jwt,authorisationServerMetadata)
                .flatMap(MessageUtils::extractAllQueryParams)
                .flatMap(codeAndState -> sendTokenRequest(codeVerifier, did, authorisationServerMetadata, codeAndState));
    }


    private Mono<String> buildVpTokenResponse(String processId, String authorizationToken, String jwt, AuthorisationServerMetadata authorisationServerMetadata) {
        return extractRequiredParamFromJwt(jwt)
                .flatMap(params -> processPresentationDefinition(params.get(3))
                        .flatMap(map -> {
                            @SuppressWarnings("unchecked")
                            List<String> vcTypeList = (List<String>) map.get("types");
                            @SuppressWarnings("unchecked")
                            List<String> inputDescriptorIdsList = (List<String>) map.get("inputDescriptorIds");
                            String presentationDefinitionId = (String) map.get("presentationDefinitionId");

                            return buildSignedJwtVerifiablePresentationByVcTypeList(processId, authorizationToken, vcTypeList,params.get(0),authorisationServerMetadata)
                                    .flatMap(vp -> buildPresentationSubmission(inputDescriptorIdsList,presentationDefinitionId)
                                            .flatMap(presentationSubmission -> sendVpTokenResponse(vp,params,presentationSubmission))
                                    );
                        })
                );
    }

    private Mono<TokenResponse> sendTokenRequest(String codeVerifier, String did, AuthorisationServerMetadata authorisationServerMetadata,Map<String, String> params){
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
    private Mono<String> sendVpTokenResponse(String vpToken, List<String> params,PresentationSubmission presentationSubmission){
        try {
            String body = "vp_token=" + URLEncoder.encode(vpToken, StandardCharsets.UTF_8)
                    + "&presentation_submission=" + URLEncoder.encode(objectMapper.writeValueAsString(presentationSubmission), StandardCharsets.UTF_8)
                    + "&state=" + URLEncoder.encode(params.get(1), StandardCharsets.UTF_8);
            List<Map.Entry<String, String>> headers = new ArrayList<>();
            headers.add(new AbstractMap.SimpleEntry<>(CONTENT_TYPE, CONTENT_TYPE_URL_ENCODED_FORM));


            return postRequest(params.get(2),headers,body)
                    .onErrorResume(e -> Mono.error(new FailedCommunicationException("Error while sending Id Token Response")));
        }
        catch (JsonProcessingException e){
            return Mono.error(new FailedSerializingException("Error while serializing Presentation Submission"));
        }
    }
    private Mono<String> buildSignedJwtVerifiablePresentationByVcTypeList(String processId,String authorizationToken,List<String> vcTypeList, String nonce,AuthorisationServerMetadata authorisationServerMetadata){
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId,userId))
                .flatMap(entity -> userDataService.getSelectableVCsByVcTypeList(vcTypeList,entity.get()))
                .flatMap(list -> {
                    log.debug(list.toString());
                    VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().selectedVcList(list).build();
                    return presentationService.createSignedVerifiablePresentation(processId,authorizationToken,vcSelectorResponse,nonce,authorisationServerMetadata.issuer());
                });
    }

    private Mono<List<String>> extractRequiredParamFromJwt(String jwt) {
        try {
            log.debug(jwt);
            SignedJWT signedJwt = SignedJWT.parse(jwt);
            List<String> params = new ArrayList<>(List.of(
                    signedJwt.getJWTClaimsSet().getClaim("nonce").toString(),
                    signedJwt.getJWTClaimsSet().getClaim("state").toString(),
                    signedJwt.getJWTClaimsSet().getClaim("redirect_uri").toString())
            );

            if (signedJwt.getJWTClaimsSet().getClaim("presentation_definition") != null) {
                String presentationDefinition = objectMapper.writeValueAsString(signedJwt.getJWTClaimsSet().getClaim("presentation_definition"));
                params.add(presentationDefinition);
                return Mono.just(params);
            } else if (signedJwt.getJWTClaimsSet().getClaim("presentation_definition_uri") != null) {
                String presentationDefinitionUri = signedJwt.getJWTClaimsSet().getClaim("presentation_definition_uri").toString();
                List<Map.Entry<String, String>> headers = new ArrayList<>();
                return getRequest(presentationDefinitionUri, headers)
                        .flatMap(presentationDefinition -> {
                            try {
                                String presentationDefinitionStr = objectMapper.writeValueAsString(presentationDefinition);
                                params.add(presentationDefinitionStr);
                                return Mono.just(params);
                            }catch (JsonProcessingException e){
                                return Mono.error(new IllegalArgumentException("Error getting property"));
                            }

                        });
            } else {
                throw new IllegalArgumentException("not known property");
            }
        } catch (ParseException | JsonProcessingException e) {
            return Mono.error(new IllegalArgumentException("Error getting property"));
        }
    }

    private Mono<Map<String, Object>> processPresentationDefinition(String jsonDefinition) {
        return Mono.fromCallable(() -> {
            PresentationDefinition definition = objectMapper.readValue(jsonDefinition, PresentationDefinition.class);
            Map<String, Object> propertiesMap = new HashMap<>();
            List<String> typesList = new ArrayList<>();
            List<String> inputDescriptorIds = new ArrayList<>();

            propertiesMap.put("presentationDefinitionId", definition.id());

            for (PresentationDefinition.InputDescriptor descriptor : definition.inputDescriptors()) {
                inputDescriptorIds.add(descriptor.id());

                for (PresentationDefinition.InputDescriptor.Constraint.Field field : descriptor.constraints().fields()) {
                    JsonNode filterNode = field.filter();
                    if (filterNode != null && filterNode.isObject()) {
                        JsonNode containsNode = filterNode.path("contains");
                        if (!containsNode.isMissingNode() && containsNode.isObject()) {
                            JsonNode constNode = containsNode.path("const");
                            if (!constNode.isMissingNode() && constNode.isTextual()) {
                                typesList.add(constNode.asText());
                            }
                        }
                    }
                }
            }

            propertiesMap.put("types", typesList);
            propertiesMap.put("inputDescriptorIds", inputDescriptorIds);
            return propertiesMap;
        });
    }

    private Mono<PresentationSubmission> buildPresentationSubmission(List<String> ids, String presentationDefinitionId) {
        List<PresentationSubmission.DescriptorMap> descriptorMaps = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            PresentationSubmission.DescriptorMap nestedDescriptorMap = PresentationSubmission.DescriptorMap.builder()
                    .id(id)
                    .format("jwt_vc")
                    .path("$.verifiableCredential[" + i + "]")
                    .pathNested(null).build();

            PresentationSubmission.DescriptorMap descriptorMap = PresentationSubmission.DescriptorMap.builder()
                    .id(id)
                    .format("jwt_vp")
                    .path("$.vp")
                    .pathNested(nestedDescriptorMap).build();

            descriptorMaps.add(descriptorMap);
        }

        return Mono.just(PresentationSubmission.builder()
                .id(UUID.randomUUID().toString())
                .definitionId(presentationDefinitionId)
                .descriptorMap(descriptorMaps).build());
    }
}
