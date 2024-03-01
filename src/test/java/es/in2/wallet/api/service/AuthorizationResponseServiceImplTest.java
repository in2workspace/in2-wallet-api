package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import es.in2.wallet.api.exception.FailedDeserializingException;
import es.in2.wallet.api.model.VcSelectorResponse;
import es.in2.wallet.api.model.VerifiableCredential;
import es.in2.wallet.api.model.VerifiablePresentation;
import es.in2.wallet.api.service.impl.AuthorizationResponseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthorizationResponseServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private AuthorizationResponseServiceImpl authorizationResponseService;

    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentationTest() throws JsonProcessingException {
        String processId = "123";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();
        String verifiablePresentation = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2cCI6eyJpZCI6InVybjp1dWlkOjczMTdhMWI5LTVjYWEtNDE2ZC04YTMwLTM3MzllZmQyZWNiZiIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJob2xkZXIiOiJkaWQ6a2V5OnpEbmFlaXVjYURkUXJKVG5DSzcyekFSbnNNQ3NtN0V2YXE2Y29VZjJwUms5dWQ1UXQiLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6IjZCRk0rS0daOU9KMiRSMkgzOFYvTVI6MlJLQjUkRy8rUUYqVERNSUNRS1UrTy1HMyRQUU84MTBRVSouVERPOUpTVEwyNzE3UU5BUS1aTTguTk5WN1dDOCVLQTEkMlZZTiUqQkhURjQtM0FCSy8kUTg5MlJHNUdLVU5EOFdJU0w3QlFHS0QgT0otTkhDVSU5U0hDVVlZVVpQTi8zTllSRDdPVkdOOE9QQTk3TEQ1QzZYUTkvVUdXQk05TS8kRUszT0I4RUlJRk5OUEskMU4gRSUzTkpHM1MxR1k4RiQ3Q1c1VU1URkslM0UxRSAgTVdKVTJZSEIuTTMlQ1VEVkJVN0E2RFBRT1hXVS82S0dUNVczVVU3Nkw3Qk1VUEIvTVhZNVU2QjdERSVaNlgwSVYyVjoxM1olVk0lR0cvOTVDMFoyUVNMVEk3TDJBMDFKUDVXNzFOTVdSMUY5MCVVTUcqMyU1QjY3NVo5Q1BIMkEuS1dURyQzNk02VEQ3VzElTjg6Q0kgREdCS01EOStYRlA0NFpOSUY0MkFHTVVLMSRRUFgrTFk0REhWViVWRS5GM0wtSFhJTEpPS1BXSVI5MktLR1FKNTVCTC9KUVdSSTRHSFotSVVSMUZUVTFKUk1WNU5TSC1GQUpZNUowVjRLR0srVVVCTypWRFlBODgxVjJTU1ArVjBEUUlGNzNBRkVIOUJHUiAzSC4zV1o5OFZMUUc0VUdFU0hGNC06M1YtQTdYUjlSSTVXNVUgNkpMOC0xTk1CSkdRUFJLSC00Tk9WNVVJSkUgT001VFIgQiUkUUQvNFJTQ0JZQUdIOC4wNFhHOC5TQkVISUpNNFZZNTlVUVRBRC1PS1Q2SUQuMU1GOUozQjlLSytCSkYxUFZIRUlNM0pETExaQyQtOTlVREcqSk8yMC4wMTkrMC03NUEqOTNEQU9KSkFYMkhWMUIzMkxVTTFXSUckSlEkTEhUSCpCT1ZMS0gqNENJTDhSSEslNVJZTEk4OC8kQkRPSlJKSDg0Ni5IRS45VCo3NjE0RjM0MiUvVE5PSE5FTTRSQVpDT0NFSVI5STlTMlhHVFBQNFdBOThNNio6Ty83UCtMREJaM0lETTNYQkI1NjgyVTVWVUdWMUElVklPNVcgNEk5VzQ6MVlISlkzTFdVRDFUVDRESlEwSS0kUkYqMTpONy0qMVhRNzg5T0RGVS1IOCpQVktPU1VUUjBYUVQ0QlM3TkZGVkVSOTo6VUMqUjEyV05DUVotUjEvNSA5UU5HVTkvSyowT1IzTiQ5Qi9aSjVHSC83VldFNzhNMCpLUSo0In0sImV4cCI6MTcwODc1MDYxNiwiaWF0IjoxNzA4NjkwNjE2LCJpc3MiOiJkaWQ6a2V5OnpEbmFlaXVjYURkUXJKVG5DSzcyekFSbnNNQ3NtN0V2YXE2Y29VZjJwUms5dWQ1UXQiLCJqdGkiOiJ1cm46dXVpZDo3MzE3YTFiOS01Y2FhLTQxNmQtOGEzMC0zNzM5ZWZkMmVjYmYiLCJuYmYiOjE3MDg2OTA2MTYsInN1YiI6ImRpZDprZXk6ekRuYWVpdWNhRGRRckpUbkNLNzJ6QVJuc01Dc203RXZhcTZjb1VmMnBSazl1ZDVRdCIsIm5vbmNlIjoidXJuOnV1aWQ6ZTA4ZGI3NDAtY2VhMi00ZjkyLWEzZmItMjdhN2FhMzk0NGZiIn0.J_45n5Ra3iXCYI2G9zcGkjEXw6LMBbQjnG_XBmjUKOA";
        String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String vc = "6BFM+KGZ9OJ2$R2H38V/MR:2RKB5$G/+QF*TDMICQKU+O-G3$PQO810QU*.TDO9JSTL2717QNAQ-ZM8.NNV7WC8%KA1$2VYN%*BHTF4-3ABK/$Q892RG5GKUND8WISL7BQGKD OJ-NHCU%9SHCUYYUZPN/3NYRD7OVGN8OPA97LD5C6XQ9/UGWBM9M/$EK3OB8EIIFNNPK$1N E%3NJG3S1GY8F$7CW5UMTFK%3E1E  MWJU2YHB.M3%CUDVBU7A6DPQOXWU/6KGT5W3UU76L7BMUPB/MXY5U6B7DE%Z6X0IV2V:13Z%VM%GG/95C0Z2QSLTI7L2A01JP5W71NMWR1F90%UMG*3%5B675Z9CPH2A.KWTG$36M6TD7W1%N8:CI DGBKMD9+XFP44ZNIF42AGMUK1$QPX+LY4DHVV%VE.F3L-HXILJOKPWIR92KKGQJ55BL/JQWRI4GHZ-IUR1FTU1JRMV5NSH-FAJY5J0V4KGK+UUBO*VDYA881V2SSP+V0DQIF73AFEH9BGR 3H.3WZ98VLQG4UGESHF4-:3V-A7XR9RI5W5U 6JL8-1NMBJGQPRKH-4NOV5UIJE OM5TR B%$QD/4RSCBYAGH8.04XG8.SBEHIJM4VY59UQTAD-OKT6ID.1MF9J3B9KK+BJF1PVHEIM3JDLLZC$-99UDG*JO20.019+0-75A*93DAOJJAX2HV1B32LUM1WIG$JQ$LHTH*BOVLKH*4CIL8RHK%5RYLI88/$BDOJRJH846.HE.9T*7614F342%/TNOHNEM4RAZCOCEIR9I9S2XGTPP4WA98M6*:O/7P+LDBZ3IDM3XBB5682U5VUGV1A%VIO5W 4I9W4:1YHJY3LWUD1TT4DJQ0I-$RF*1:N7-*1XQ789ODFU-H8*PVKOSUTR0XQT4BS7NFFVER9::UC*R12WNCQZ-R1/5 9QNGU9/K*0OR3N$9B/ZJ5GH/7VWE78M0*KQ*4";

        VerifiableCredential expectedVerifiableCredential = VerifiableCredential.builder().build();
        VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc)).build();

        JsonNode vpNodeMock = Mockito.mock(JsonNode.class);
        JWTClaimsSet vpClaimsMock = Mockito.mock(JWTClaimsSet.class);
        when(objectMapper.valueToTree(vpClaimsMock.getClaim("vp"))).thenReturn(vpNodeMock);
        when(objectMapper.treeToValue(vpNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);

        JsonNode vcNodeMock = Mockito.mock(JsonNode.class);
        JWTClaimsSet vcClaimsMock = Mockito.mock(JWTClaimsSet.class);
        when(objectMapper.valueToTree(vcClaimsMock.getClaim("vc"))).thenReturn(vcNodeMock);
        when(objectMapper.treeToValue(vcNodeMock, VerifiableCredential.class)).thenReturn(expectedVerifiableCredential);

        StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId,vcSelectorResponse,verifiablePresentation,authorizationToken))
                .expectError(FailedDeserializingException.class)
                .verify();

    }
    @Test
    void buildAndPostAuthorizationResponseWithVerifiablePresentation_throwsFailedDeserializingException_onDeserializeVCTest() throws JsonProcessingException {
        String processId = "123";
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().build();
        String verifiablePresentation = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2cCI6eyJpZCI6InVybjp1dWlkOjczMTdhMWI5LTVjYWEtNDE2ZC04YTMwLTM3MzllZmQyZWNiZiIsInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJob2xkZXIiOiJkaWQ6a2V5OnpEbmFlaXVjYURkUXJKVG5DSzcyekFSbnNNQ3NtN0V2YXE2Y29VZjJwUms5dWQ1UXQiLCJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6IjZCRk0rS0daOU9KMiRSMkgzOFYvTVI6MlJLQjUkRy8rUUYqVERNSUNRS1UrTy1HMyRQUU84MTBRVSouVERPOUpTVEwyNzE3UU5BUS1aTTguTk5WN1dDOCVLQTEkMlZZTiUqQkhURjQtM0FCSy8kUTg5MlJHNUdLVU5EOFdJU0w3QlFHS0QgT0otTkhDVSU5U0hDVVlZVVpQTi8zTllSRDdPVkdOOE9QQTk3TEQ1QzZYUTkvVUdXQk05TS8kRUszT0I4RUlJRk5OUEskMU4gRSUzTkpHM1MxR1k4RiQ3Q1c1VU1URkslM0UxRSAgTVdKVTJZSEIuTTMlQ1VEVkJVN0E2RFBRT1hXVS82S0dUNVczVVU3Nkw3Qk1VUEIvTVhZNVU2QjdERSVaNlgwSVYyVjoxM1olVk0lR0cvOTVDMFoyUVNMVEk3TDJBMDFKUDVXNzFOTVdSMUY5MCVVTUcqMyU1QjY3NVo5Q1BIMkEuS1dURyQzNk02VEQ3VzElTjg6Q0kgREdCS01EOStYRlA0NFpOSUY0MkFHTVVLMSRRUFgrTFk0REhWViVWRS5GM0wtSFhJTEpPS1BXSVI5MktLR1FKNTVCTC9KUVdSSTRHSFotSVVSMUZUVTFKUk1WNU5TSC1GQUpZNUowVjRLR0srVVVCTypWRFlBODgxVjJTU1ArVjBEUUlGNzNBRkVIOUJHUiAzSC4zV1o5OFZMUUc0VUdFU0hGNC06M1YtQTdYUjlSSTVXNVUgNkpMOC0xTk1CSkdRUFJLSC00Tk9WNVVJSkUgT001VFIgQiUkUUQvNFJTQ0JZQUdIOC4wNFhHOC5TQkVISUpNNFZZNTlVUVRBRC1PS1Q2SUQuMU1GOUozQjlLSytCSkYxUFZIRUlNM0pETExaQyQtOTlVREcqSk8yMC4wMTkrMC03NUEqOTNEQU9KSkFYMkhWMUIzMkxVTTFXSUckSlEkTEhUSCpCT1ZMS0gqNENJTDhSSEslNVJZTEk4OC8kQkRPSlJKSDg0Ni5IRS45VCo3NjE0RjM0MiUvVE5PSE5FTTRSQVpDT0NFSVI5STlTMlhHVFBQNFdBOThNNio6Ty83UCtMREJaM0lETTNYQkI1NjgyVTVWVUdWMUElVklPNVcgNEk5VzQ6MVlISlkzTFdVRDFUVDRESlEwSS0kUkYqMTpONy0qMVhRNzg5T0RGVS1IOCpQVktPU1VUUjBYUVQ0QlM3TkZGVkVSOTo6VUMqUjEyV05DUVotUjEvNSA5UU5HVTkvSyowT1IzTiQ5Qi9aSjVHSC83VldFNzhNMCpLUSo0In0sImV4cCI6MTcwODc1MDYxNiwiaWF0IjoxNzA4NjkwNjE2LCJpc3MiOiJkaWQ6a2V5OnpEbmFlaXVjYURkUXJKVG5DSzcyekFSbnNNQ3NtN0V2YXE2Y29VZjJwUms5dWQ1UXQiLCJqdGkiOiJ1cm46dXVpZDo3MzE3YTFiOS01Y2FhLTQxNmQtOGEzMC0zNzM5ZWZkMmVjYmYiLCJuYmYiOjE3MDg2OTA2MTYsInN1YiI6ImRpZDprZXk6ekRuYWVpdWNhRGRRckpUbkNLNzJ6QVJuc01Dc203RXZhcTZjb1VmMnBSazl1ZDVRdCIsIm5vbmNlIjoidXJuOnV1aWQ6ZTA4ZGI3NDAtY2VhMi00ZjkyLWEzZmItMjdhN2FhMzk0NGZiIn0.J_45n5Ra3iXCYI2G9zcGkjEXw6LMBbQjnG_XBmjUKOA";
        String authorizationToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String vc = "6BFM+KGZ9OJ2$R2H38V/MR:2RKB5$G/+QF*TDMICQKU+O-G3$PQO810QU*.TDO9JSTL2717QNAQ-ZM8.NNV7WC8%KA1$2VYN%*BHTF4-3ABK/$Q892RG5GKUND8WISL7BQGKD OJ-NHCU%9SHCUYYUZPN/3NYRD7OVGN8OPA97LD5C6XQ9/UGWBM9M/$EK3OB8EIIFNNPK$1N E%3NJG3S1GY8F$7CW5UMTFK%3E1E  MWJU2YHB.M3%CUDVBU7A6DPQOXWU/6KGT5W3UU76L7BMUPB/MXY5U6B7DE%Z6X0IV2V:13Z%VM%GG/95C0Z2QSLTI7L2A01JP5W71NMWR1F90%UMG*3%5B675Z9CPH2A.KWTG$36M6TD7W1%N8:CI DGBKMD9+XFP44ZNIF42AGMUK1$QPX+LY4DHVV%VE.F3L-HXILJOKPWIR92KKGQJ55BL/JQWRI4GHZ-IUR1FTU1JRMV5NSH-FAJY5J0V4KGK+UUBO*VDYA881V2SSP+V0DQIF73AFEH9BGR 3H.3WZ98VLQG4UGESHF4-:3V-A7XR9RI5W5U 6JL8-1NMBJGQPRKH-4NOV5UIJE OM5TR B%$QD/4RSCBYAGH8.04XG8.SBEHIJM4VY59UQTAD-OKT6ID.1MF9J3B9KK+BJF1PVHEIM3JDLLZC$-99UDG*JO20.019+0-75A*93DAOJJAX2HV1B32LUM1WIG$JQ$LHTH*BOVLKH*4CIL8RHK%5RYLI88/$BDOJRJH846.HE.9T*7614F342%/TNOHNEM4RAZCOCEIR9I9S2XGTPP4WA98M6*:O/7P+LDBZ3IDM3XBB5682U5VUGV1A%VIO5W 4I9W4:1YHJY3LWUD1TT4DJQ0I-$RF*1:N7-*1XQ789ODFU-H8*PVKOSUTR0XQT4BS7NFFVER9::UC*R12WNCQZ-R1/5 9QNGU9/K*0OR3N$9B/ZJ5GH/7VWE78M0*KQ*4";
        VerifiablePresentation expectedVerifiablePresentation = VerifiablePresentation.builder().id("id").verifiableCredential(List.of(vc)).build();

        JsonNode rootNodeMock = Mockito.mock(JsonNode.class);
        when(objectMapper.valueToTree(any())).thenReturn(rootNodeMock);
        when(objectMapper.treeToValue(rootNodeMock, VerifiablePresentation.class)).thenReturn(expectedVerifiablePresentation);

        StepVerifier.create(authorizationResponseService.buildAndPostAuthorizationResponseWithVerifiablePresentation(processId,vcSelectorResponse,verifiablePresentation,authorizationToken))
                .expectError(FailedDeserializingException.class)
                .verify();

    }

}
