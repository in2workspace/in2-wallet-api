package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.domain.exception.AttestationClientErrorException;
import es.in2.wallet.domain.exception.AttestationServerErrorException;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.model.VerifiableCredential;
import es.in2.wallet.domain.model.VerifiablePresentation;
import es.in2.wallet.domain.service.impl.AuthorizationResponseServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.infrastructure.core.config.WebClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationResponseServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebClientConfig webClientConfig;
    @InjectMocks
    private AuthorizationResponseServiceImpl authorizationResponseService;

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentation_throwsFailedDeserializingException_onDeserializeVPTest() throws JsonProcessingException {
        String processId = "123";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();
        String verifiablePresentation = "invalid_vp";
        String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId,vcSelectorResponse,verifiablePresentation,authorizationToken))
                .expectError(FailedDeserializingException.class)
                .verify();

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentation_throwsFailedDeserializingException_onDeserializeVCTest() throws JsonProcessingException {
        String processId = "123";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();
        String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
        String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String vc = "6BFM+KGZ9OJ2$R2H38V/MR:2RKB5$G/+QF*TDMICQKU+O-G3$PQO810QU*.TDO9JSTL2717QNAQ-ZM8.NNV7WC8%KA1$2VYN%*BHTF4-3ABK/$Q892RG5GKUND8WISL7BQGKD OJ-NHCU%9SHCUYYUZPN/3NYRD7OVGN8OPA97LD5C6XQ9/UGWBM9M/$EK3OB8EIIFNNPK$1N E%3NJG3S1GY8F$7CW5UMTFK%3E1E  MWJU2YHB.M3%CUDVBU7A6DPQOXWU/6KGT5W3UU76L7BMUPB/MXY5U6B7DE%Z6X0IV2V:13Z%VM%GG/95C0Z2QSLTI7L2A01JP5W71NMWR1F90%UMG*3%5B675Z9CPH2A.KWTG$36M6TD7W1%N8:CI DGBKMD9+XFP44ZNIF42AGMUK1$QPX+LY4DHVV%VE.F3L-HXILJOKPWIR92KKGQJ55BL/JQWRI4GHZ-IUR1FTU1JRMV5NSH-FAJY5J0V4KGK+UUBO*VDYA881V2SSP+V0DQIF73AFEH9BGR 3H.3WZ98VLQG4UGESHF4-:3V-A7XR9RI5W5U 6JL8-1NMBJGQPRKH-4NOV5UIJE OM5TR B%$QD/4RSCBYAGH8.04XG8.SBEHIJM4VY59UQTAD-OKT6ID.1MF9J3B9KK+BJF1PVHEIM3JDLLZC$-99UDG*JO20.019+0-75A*93DAOJJAX2HV1B32LUM1WIG$JQ$LHTH*BOVLKH*4CIL8RHK%5RYLI88/$BDOJRJH846.HE.9T*7614F342%/TNOHNEM4RAZCOCEIR9I9S2XGTPP4WA98M6*:O/7P+LDBZ3IDM3XBB5682U5VUGV1A%VIO5W 4I9W4:1YHJY3LWUD1TT4DJQ0I-$RF*1:N7-*1XQ789ODFU-H8*PVKOSUTR0XQT4BS7NFFVER9::UC*R12WNCQZ-R1/5 9QNGU9/K*0OR3N$9B/ZJ5GH/7VWE78M0*KQ*4";
        VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc)).build();

        JsonNode rootNodeMock = mock(JsonNode.class);
        when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
        when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);

        StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId,vcSelectorResponse,verifiablePresentation,authorizationToken))
                .expectError(FailedDeserializingException.class)
                .verify();

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentation_throwsFailedRuntimeException_onParsingPresentationSubmissionTest() throws JsonProcessingException {
        String processId = "123";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();
        String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
        String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String vc = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc)).build();
        VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().id("id").build();

        JsonNode rootNodeMock = mock(JsonNode.class);
        when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
        when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);
        when(objectMapper.treeToValue(rootNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);

        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException());
        StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId,vcSelectorResponse,verifiablePresentation,authorizationToken))
                .expectError(RuntimeException.class)
                .verify();

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentationTest_Success() throws JsonProcessingException {
            String processId = "123";
            VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("redirectUri").state("state").build();
            String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
            String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String vc = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc, vc)).build();
            VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().id("id").build();
            String expectedPresentationSubmission = "mockPresentationSubmission";

            JsonNode rootNodeMock = mock(JsonNode.class);
            when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
            when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);
            when(objectMapper.treeToValue(rootNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);

            when(objectMapper.writeValueAsString(any())).thenReturn(expectedPresentationSubmission);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(authorizationToken)
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken))
                    .expectNext(authorizationToken)
                    .verifyComplete();

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentationTest_BadRequest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("redirectUri").state("state").build();
            String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
            String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String vc = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc, vc)).build();
            VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().id("id").build();
            String expectedPresentationSubmission = "mockPresentationSubmission";

            JsonNode rootNodeMock = mock(JsonNode.class);
            when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
            when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);
            when(objectMapper.treeToValue(rootNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);

            when(objectMapper.writeValueAsString(any())).thenReturn(expectedPresentationSubmission);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken))
                    .expectError(AttestationClientErrorException.class)
                    .verify();
        }

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentationTest_InternalServerError() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("redirectUri").state("state").build();
            String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
            String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String vc = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc, vc)).build();
            VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().id("id").build();
            String expectedPresentationSubmission = "mockPresentationSubmission";

            JsonNode rootNodeMock = mock(JsonNode.class);
            when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
            when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);
            when(objectMapper.treeToValue(rootNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);

            when(objectMapper.writeValueAsString(any())).thenReturn(expectedPresentationSubmission);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body("error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken))
                    .expectError(AttestationServerErrorException.class)
                    .verify();
        }

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentationTest_Redirection() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("redirectUri").state("state").build();
            String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
            String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String vc = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc, vc)).build();
            VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().id("id").build();
            String expectedPresentationSubmission = "mockPresentationSubmission";

            JsonNode rootNodeMock = mock(JsonNode.class);
            when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
            when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);
            when(objectMapper.treeToValue(rootNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);

            when(objectMapper.writeValueAsString(any())).thenReturn(expectedPresentationSubmission);

            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);

            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.FOUND)
                    .header("Content-Type", "application/json")
                    .body("error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken))
                    .expectError(RuntimeException.class)
                    .verify();
        }

    }

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentationTest_RunTimeExceptionInvalidToken() throws JsonProcessingException {
            String processId = "123";
            VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().redirectUri("redirectUri").state("state").build();
            String verifiablePresentation = "ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SjJjQ0k2ZXlKcFpDSTZJbk1pTENKMGVYQmxJanBiSW5NaVhTd2lhRzlzWkdWeUlqb2ljeUlzSWtCamIyNTBaWGgwSWpwYkluTWlYU3dpZG1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aU9pSTJRa1pOSzB0SFdqbFBTaklrVWpKSU16aFdMMDFTT2pKU1MwSTFKRWN2SzFGR0tsUkVUVWxEVVV0VkswOHRSek1rVUZGUE9ERXdVVlVxTGxSRVR6bEtVMVJNTWpjeE4xRk9RVkV0V2swNExrNU9WamRYUXpnbFMwRXhKREpXV1U0bEtrSklWRVkwTFROQlFrc3ZKRkU0T1RKU1J6VkhTMVZPUkRoWFNWTk1OMEpSUjB0RUlFOUtMVTVJUTFVbE9WTklRMVZaV1ZWYVVFNHZNMDVaVWtRM1QxWkhUamhQVUVFNU4weEVOVU0yV0ZFNUwxVkhWMEpOT1UwdkpFVkxNMDlDT0VWSlNVWk9UbEJMSkRGT0lFVWxNMDVLUnpOVE1VZFpPRVlrTjBOWE5WVk5WRVpMSlRORk1VVWdJRTFYU2xVeVdVaENMazB6SlVOVlJGWkNWVGRCTmtSUVVVOVlWMVV2Tmt0SFZEVlhNMVZWTnpaTU4wSk5WVkJDTDAxWVdUVlZOa0kzUkVVbFdqWllNRWxXTWxZNk1UTmFKVlpOSlVkSEx6azFRekJhTWxGVFRGUkpOMHd5UVRBeFNsQTFWemN4VGsxWFVqRkdPVEFsVlUxSEtqTWxOVUkyTnpWYU9VTlFTREpCTGt0WFZFY2tNelpOTmxSRU4xY3hKVTQ0T2tOSklFUkhRa3ROUkRrcldFWlFORFJhVGtsR05ESkJSMDFWU3pFa1VWQllLMHhaTkVSSVZsWWxWa1V1UmpOTUxVaFlTVXhLVDB0UVYwbFNPVEpMUzBkUlNqVTFRa3d2U2xGWFVrazBSMGhhTFVsVlVqRkdWRlV4U2xKTlZqVk9VMGd0UmtGS1dUVktNRlkwUzBkTEsxVlZRazhxVmtSWlFUZzRNVll5VTFOUUsxWXdSRkZKUmpjelFVWkZTRGxDUjFJZ00wZ3VNMWRhT1RoV1RGRkhORlZIUlZOSVJqUXRPak5XTFVFM1dGSTVVa2sxVnpWVklEWktURGd0TVU1TlFrcEhVVkJTUzBndE5FNVBWalZWU1VwRklFOU5OVlJTSUVJbEpGRkVMelJTVTBOQ1dVRkhTRGd1TURSWVJ6Z3VVMEpGU0VsS1RUUldXVFU1VlZGVVFVUXRUMHRVTmtsRUxqRk5SamxLTTBJNVMwc3JRa3BHTVZCV1NFVkpUVE5LUkV4TVdrTWtMVGs1VlVSSEtrcFBNakF1TURFNUt6QXROelZCS2prelJFRlBTa3BCV0RKSVZqRkNNekpNVlUweFYwbEhKRXBSSkV4SVZFZ3FRazlXVEV0SUtqUkRTVXc0VWtoTEpUVlNXVXhKT0RndkpFSkVUMHBTU2tnNE5EWXVTRVV1T1ZRcU56WXhORVl6TkRJbEwxUk9UMGhPUlUwMFVrRmFRMDlEUlVsU09VazVVekpZUjFSUVVEUlhRVGs0VFRZcU9rOHZOMUFyVEVSQ1dqTkpSRTB6V0VKQ05UWTRNbFUxVmxWSFZqRkJKVlpKVHpWWElEUkpPVmMwT2pGWlNFcFpNMHhYVlVReFZGUTBSRXBSTUVrdEpGSkdLakU2VGpjdEtqRllVVGM0T1U5RVJsVXRTRGdxVUZaTFQxTlZWRkl3V0ZGVU5FSlROMDVHUmxaRlVqazZPbFZES2xJeE1sZE9RMUZhTFZJeEx6VWdPVkZPUjFVNUwwc3FNRTlTTTA0a09VSXZXa28xUjBndk4xWlhSVGM0VFRBcVMxRXFOQ0o5TENKbGVIQWlPakUzTURnM05UQTJNVFlzSW1saGRDSTZNVGN3T0RZNU1EWXhOaXdpYVhOeklqb2ljeUlzSW1wMGFTSTZJbk1pTENKdVltWWlPakUzTURnMk9UQTJNVFlzSW5OMVlpSTZJbk1pTENKdWIyNWpaU0k2SW5NaWZRLkdkd2phSGRuZEZJX1M2WmpOS0JGeG1LeWdZSU5DS1pQYkxVeGo0YXY3VGc=";
            String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            String vc = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc, vc)).build();
            VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().id("id").build();
            String expectedPresentationSubmission = "mockPresentationSubmission";

            JsonNode rootNodeMock = mock(JsonNode.class);
            when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
            when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);
            when(objectMapper.treeToValue(rootNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);


            ExchangeFunction exchangeFunction = mock(ExchangeFunction.class);
            // Create a mock ClientResponse for a successful response
            ClientResponse clientResponse = ClientResponse.create(HttpStatus.BAD_REQUEST)
                    .header("Content-Type", "application/json")
                    .body("error")
                    .build();

            // Stub the exchange function to return the mock ClientResponse
            when(exchangeFunction.exchange(any())).thenReturn(Mono.just(clientResponse));

            WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
            when(webClientConfig.centralizedWebClient()).thenReturn(webClient);

            when(objectMapper.writeValueAsString(any())).thenReturn(expectedPresentationSubmission);
            StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId, vcSelectorResponse, verifiablePresentation, authorizationToken))
                    .expectError(AttestationClientErrorException.class)
                    .verify();


    }
}
