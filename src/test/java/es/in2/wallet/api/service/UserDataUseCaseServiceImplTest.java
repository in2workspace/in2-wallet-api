package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.impl.UserDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.*;

import static es.in2.wallet.domain.util.MessageUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDataUseCaseServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserDataServiceImpl userDataServiceImpl;

    @Test
    void testRegisterUserInContextBroker() throws JsonProcessingException {
        String id = "123";

        UserEntity expectedUserEntity = new UserEntity(
                "urn:entities:userId:" + id,
                "userEntity",
                new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>()),
                new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>())
        );

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(expectedUserEntity)).thenReturn("user entity");

        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.createUserEntity(id))
                .expectNext("user entity")
                .verifyComplete();
    }
    @Test
    void testSaveVCWithJwtFormat() throws JsonProcessingException {
        // Sample JWT token for a verifiable credential
        String vcJwt = "eyJraWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4jelEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6a2V5OnpEbmFlZnk3amhwY0ZCanp0TXJFSktFVHdFU0NoUXd4cEpuVUpLb3ZzWUQ1ZkpabXAiLCJuYmYiOjE2OTgxMzQ4NTUsImlzcyI6ImRpZDprZXk6elEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsImV4cCI6MTcwMDcyNjg1NSwiaWF0IjoxNjk4MTM0ODU1LCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWwiXSwiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL2RvbWUtbWFya2V0cGxhY2UuZXUvLzIwMjIvY3JlZGVudGlhbHMvbGVhcmNyZWRlbnRpYWwvdjEiXSwiaWQiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkiLCJpc3N1ZXIiOnsiaWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4ifSwiaXNzdWFuY2VEYXRlIjoiMjAyMy0xMC0yNFQwODowNzozNVoiLCJpc3N1ZWQiOiIyMDIzLTEwLTI0VDA4OjA3OjM1WiIsInZhbGlkRnJvbSI6IjIwMjMtMTAtMjRUMDg6MDc6MzVaIiwiZXhwaXJhdGlvbkRhdGUiOiIyMDIzLTExLTIzVDA4OjA3OjM1WiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmtleTp6RG5hZWZ5N2pocGNGQmp6dE1yRUpLRVR3RVNDaFF3eHBKblVKS292c1lENWZKWm1wIiwidGl0bGUiOiJNci4iLCJmaXJzdF9uYW1lIjoiSm9obiIsImxhc3RfbmFtZSI6IkRvZSIsImdlbmRlciI6Ik0iLCJwb3N0YWxfYWRkcmVzcyI6IiIsImVtYWlsIjoiam9obmRvZUBnb29kYWlyLmNvbSIsInRlbGVwaG9uZSI6IiIsImZheCI6IiIsIm1vYmlsZV9waG9uZSI6IiszNDc4NzQyNjYyMyIsImxlZ2FsUmVwcmVzZW50YXRpdmUiOnsiY24iOiI1NjU2NTY1NlYgSmVzdXMgUnVpeiIsInNlcmlhbE51bWJlciI6IjU2NTY1NjU2ViIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRFUy0xMjM0NTY3OCIsIm8iOiJHb29kQWlyIiwiYyI6IkVTIn0sInJvbGVzQW5kRHV0aWVzIjpbeyJ0eXBlIjoiTEVBUkNyZWRlbnRpYWwiLCJpZCI6Imh0dHBzOi8vZG9tZS1tYXJrZXRwbGFjZS5ldS8vbGVhci92MS82NDg0OTk0bjRyOWU5OTA0OTQifV0sImtleSI6InZhbHVlIn19LCJqdGkiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkifQ.2_YNY515CaohirD4AHDBMvzDagEn-p8uAsaiMT0H4ltK2uVfG8IWWqV_OOR6lFlXMzUhJd7nKsaWkhnAQY8kyA";
        String vcCbor = "NCF/-KED9OQ39S2HGJU52LPGTNGFLQ$$A-UTKVU09WTF11BMD-FQTVBM7 MF81K$K2W0HB2KP:JIKC$244-3ARSJQ7:P1072VAF38F9WSKG4FDHCQHA/NEP42:25U5ZH4HXMHTUEKUA4WFGW:JS8.PFVVC-IK9TC+JYB7THU26D OUPSE-IF SRJ:PSQV4TO:YTOOV+$VD U::D7:V0/UMJ2*HJ**FF*R4BWV%ATSPT$HR8U//B2OE6UT:3CH-F*WDA3UB3VXGB1IO%6J3R3BCOC:T8TLZZFD4WE6QOQV4YEDEK807L0OK3USCNU8GEWH$+LSP9%SP2E4+/9 EG0%5DF9KMNM8EF0J4773CUD MG:E%/EGI7S$1+9L*WCR:RG6DWKONONRUJW.L:G31RJU574.DZORN0VQM34B84Z81%ME53VOV1JM 7UZ$1W.ONZ7I$J.GLI256C28-AELQ3D6W0GQX7$NQUPUSTV/3SX5TX 2ADROZU%*L6ZPB9URI6GF005OOTP3Q36ORQ-C7+B/7L9OA+$T1FJ.$RZ 8$ 7EN74Y82:A24VO%0J$33XC%M42T0S-C5/7IGI9GR-17SFLQ21L$73EEZ1LLFGSQQMU0:KHI$4UMC84V5AGJBGA+89H9IKKTS89JKZON RVP74YOK$49HD77Z4OMGYXFM693FPR2N7YV7J6.TQ2+0II9ZDSVZOBV3 F7TECOGB6E3TE9VD1W%J1I91HRZD3ZE3*C0YTQ+G5GT55M9%QO8W4L3CBHI0RN0V26GH-BGKQCOK8/KR7142.MZ92I-B587OY5LP2Y8C5H7UMTN3OK12OF1/0IOD0 Z8 +H0EK%OK.TEJNJC10P5MUU+12:8H TLYOOYGAUSI6+SLOE26OC90$A49DIQZDCH9KDRXDD%EPLZ4$V6WAF SAC245UP$8BSL4M8DDC0W94LFIRBLO%1PB6CKJTVGO5HU-D9V46N499LEY31-8-AD1/D4Y4EFRV0M6UU7W3W-37EDN%E7IB3 JD249UIZXGA.EA37-IC7D5TS1G3OP8LHS4SDGZPQYPE9XR77DP26I97B+B: 2V%QE8DYYUNNPOG1VBN4-5B65NF1D4R-T2UYF57K7M1U692M9FU6SUERQM 2IO46-KR**MNIA3*O6MRD3J4WTDW06A5CZ0.1B+NF4JIY78%5J8HIU6LY4TOYMU/O0XG%CQR%OBYIMV37G21LOJDB.29Q-CCI5VHHD+LF%F$I7-/2OX8BD8*6T9E8$.F/SNOI0DK1CE0%5O2 8VV0XOO1:RKIUG44 WEU.G J362TYU777EP.EAKB2Q6*E1+$2Y0KYMU+80JCOU372-K8K23G91:906AX 59CSYV8UX1++P9B9*RL$1BGBB7B3W6JY/HCRPEOCZ66FO21CHJ74BS4%29ML2DJKLFD++358TITC0AVXSPZRIMQ5WDL8J6XRHUZ4CY05IDD8R% GXIKNHCZL9S0NKDKA$G-N9M19XCC+5V.0U2+MY IV20C$5B:BRIN*GI.S2YW8-Z9$F5ISA4J300LD*IDD109UHV7CNLT.2M3C5T59461JREFC7OP02C+NF%DQ 9CF 1WJ4-P869L047LD4LH4$LU1XRB-IDQ6Q 6/351/K/EALGRGGL79U*SGTMTD3OV 10Q35ANU4UIEE9CQWAL9K6+293PS*5C+7RTFUPIQ8WD45N4XUR*NI-QM5A:B7G QZ2JCB1HNTS36%SQAG69Y0Y80-RSC6B-FWHIQD14QA31EVJ$7/CT2RVX:5YJUJN9N:FDQUJQCP+E IHV5SCVTICR%$GR+3FK79MQ3RD/H70:F%.FA2V0BU1H6E.5S/V9:208V UN+%V%ZGCQU";
        String jwtPayload = """
                        {
                          "sub": "did:key:zDnaefy7jhpcFBjztMrEJKETwESChQwxpJnUJKovsYD5fJZmp",
                          "nbf": 1698134855,
                          "iss": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                          "exp": 1700726855,
                          "iat": 1698134855,
                          "vc": {
                            "type": [
                              "VerifiableCredential",
                              "LEARCredential"
                            ],
                            "@context": [
                              "https://www.w3.org/2018/credentials/v1",
                              "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                            "issuer": {
                              "id": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn"
                            },
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                              "id": "did:key:zDnaefy7jhpcFBjztMrEJKETwESChQwxpJnUJKovsYD5fJZmp",
                              "title": "Mr.",
                              "first_name": "John",
                              "last_name": "Doe",
                              "gender": "M",
                              "postal_address": "",
                              "email": "johndoe@goodair.com",
                              "telephone": "",
                              "fax": "",
                              "mobile_phone": "+34787426623",
                              "legalRepresentative": {
                                "cn": "56565656V Jesus Ruiz",
                                "serialNumber": "56565656V",
                                "organizationIdentifier": "VATES-12345678",
                                "o": "GoodAir",
                                "c": "ES"
                              },
                              "rolesAndDuties": [
                                {
                                  "type": "LEARCredential",
                                  "id": "https://dome-marketplace.eu//lear/v1/6484994n4r9e990494"
                                }
                              ],
                              "key": "value"
                            }
                          },
                          "jti": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9"
                        }
                """;

        List<CredentialResponse> credentails = List.of(CredentialResponse.builder().credential(vcJwt).format(JWT_VC).build(),CredentialResponse.builder().credential(vcCbor).format(VC_CWT).build());
        // Sample JSON response entity
        UserEntity mockUserEntity = UserEntity.builder()
                .id("urn:entities:userId:1234")
                .type("userEntity")
                .dids(EntityAttribute.<List<DidAttribute>>builder()
                        .type("Property")
                        .value(List.of())
                        .build())
                .vcs(EntityAttribute.<List<VCAttribute>>builder()
                        .type("Property")
                        .value(List.of())
                        .build())
                .build();

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(mockUserEntity);
        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn("user entity with updated credential");

        ObjectMapper objectMapper2 = new ObjectMapper();
        JsonNode jsonNode = objectMapper2.readTree(jwtPayload);

        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);


        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.saveVC("entity not updated", credentails))
                .expectNext("user entity with updated credential")
                .verifyComplete();
    }
    @Test
    void testGetSelectableVCsByVcTypeList() throws JsonProcessingException {
        List<String> vcTypeList = List.of("SpecificCredentialType", "LEARCredential");

        // User entity JSON string as provided in your test setup
        String userEntityString = "userEntity";

        // Mock VCs
        List<VCAttribute> mockVcs = List.of(
                VCAttribute.builder()
                        .id("vc1")
                        .type("json_vc")
                        .value(Map.of(
                                "id", "vc1",
                                "type", List.of("VerifiableCredential", "SpecificCredentialType"),
                                "credentialSubject", Map.of("name", "John Doe")
                        ))
                        .build(),
                VCAttribute.builder()
                        .id("vc2")
                        .type("json_vc")
                        .value(Map.of(
                                "id", "vc2",
                                "type", List.of("VerifiableCredential", "LEARCredential"),
                                "credentialSubject", Map.of("name", "John Doe", "age", "25")
                        ))
                        .build()
        );

        UserEntity userEntity = UserEntity.builder()
                .id("urn:entities:userId:1234")
                .type("userEntity")
                .dids(new EntityAttribute<>("Property", new ArrayList<>()))
                .vcs(new EntityAttribute<>("Property", mockVcs))
                .build();

        // Deserializing userEntityJson into UserEntity
        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);

        when(objectMapper.convertValue(any(Map.class), eq(JsonNode.class)))
                .thenAnswer(invocation -> {
                    Map<?, ?> vcDataValue = invocation.getArgument(0);
                    return new ObjectMapper().valueToTree(vcDataValue);
                });


        StepVerifier.create(userDataServiceImpl.getSelectableVCsByVcTypeList(vcTypeList, userEntityString))
                .assertNext(vcBasicDataDTOList -> {
                    assertEquals(2, vcBasicDataDTOList.size());

                    CredentialsBasicInfo vc1Info = vcBasicDataDTOList.get(0);
                    assertEquals("vc1", vc1Info.id());
                    assertTrue(vc1Info.type().contains("SpecificCredentialType"));

                    CredentialsBasicInfo vc2Info = vcBasicDataDTOList.get(1);
                    assertEquals("vc2", vc2Info.id());
                    assertTrue(vc2Info.type().contains("LEARCredential"));
                })
                .verifyComplete();
    }

    @Test
    void testExtractDidFromVerifiableCredential() throws JsonProcessingException {
        String userEntityString = "userEntity";
        String vcId = "vc1";
        String expectedDid = "did:example:123";

        // Mock VC con DID
        VCAttribute vcAttributeWithDid = VCAttribute.builder()
                .id(vcId)
                .type("json_vc")
                .value(Map.of(
                        "id", vcId,
                        "type", List.of("VerifiableCredential", "SpecificCredentialType"),
                        "credentialSubject", Map.of("id", expectedDid)
                ))
                .build();

        List<VCAttribute> vcAttributes = List.of(vcAttributeWithDid);

        UserEntity userEntity = UserEntity.builder()
                .id("urn:entities:userId:1234")
                .type("userEntity")
                .dids(new EntityAttribute<>("Property", new ArrayList<>()))
                .vcs(new EntityAttribute<>("Property", vcAttributes))
                .build();

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
        when(objectMapper.convertValue(any(Map.class), eq(JsonNode.class)))
                .thenAnswer(invocation -> new ObjectMapper().valueToTree(invocation.getArgument(0)));

        StepVerifier.create(userDataServiceImpl.extractDidFromVerifiableCredential(userEntityString, vcId))
                .expectNext(expectedDid)
                .verifyComplete();
    }

    @Test
    void testDeleteVerifiableCredential() throws JsonProcessingException {
        String userEntityId = "userEntity";
        String vcIdToDelete = "vc1";
        String didToDelete = "did:example:123";
        String updatedUserEntityJson = "user entity with the deleted credential";

        UserEntity userEntity = new UserEntity(
                userEntityId,
                "userEntity",
                new EntityAttribute<>("Property", List.of(new DidAttribute("didType", didToDelete))),
                new EntityAttribute<>("Property", List.of(new VCAttribute(vcIdToDelete, "vc_json", Map.of("id", vcIdToDelete))))
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn(updatedUserEntityJson);

        StepVerifier.create(userDataServiceImpl.deleteVerifiableCredential(userEntityId, vcIdToDelete, didToDelete))
                .expectNextMatches(response -> response.equals(updatedUserEntityJson))
                .verifyComplete();
    }

    @Test
    void testSaveDid() throws JsonProcessingException {
        String userEntityString = "userEntity";
        String newDid = "did:key:123";
        String didMethod = "key";
        String updatedUserEntityJson = "user entity with the deleted did";

        List<DidAttribute> initialDids = List.of(new DidAttribute("key", "did:key:456"));
        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", initialDids),
                new EntityAttribute<>("Property", new ArrayList<>())
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn(updatedUserEntityJson);

        StepVerifier.create(userDataServiceImpl.saveDid(userEntityString, newDid, didMethod))
                .expectNextMatches(response -> response.equals(updatedUserEntityJson))
                .verifyComplete();
    }

    @Test
    void testDeleteDid() throws JsonProcessingException {
        String userEntityString = "userEntity";
        String selectedDid = "did:key:123";
        String updatedUserEntityJson = "user entity with updated did";

        List<DidAttribute> didsList = List.of(new DidAttribute("key", selectedDid));
        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", didsList),
                new EntityAttribute<>("Property", new ArrayList<>())
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn(updatedUserEntityJson);

        StepVerifier.create(userDataServiceImpl.deleteSelectedDidFromUserEntity(selectedDid,userEntityString))
                .expectNextMatches(response -> response.equals(updatedUserEntityJson))
                .verifyComplete();
    }

    @Test
    void testGetUserVCsInJson() throws Exception {
        String userEntityString = "userEntityJsonString";
        LinkedHashMap<String, Object> vcValue = new LinkedHashMap<>();
        vcValue.put("type", List.of("VerifiableCredential", "SpecificCredentialType"));
        vcValue.put(CREDENTIAL_SUBJECT, new LinkedHashMap<>(Map.of("id", "subjectId")));
        vcValue.put(EXPIRATION_DATE, "2024-04-07T09:57:59Z");

        List<VCAttribute> mockVcs = List.of(
                VCAttribute.builder()
                        .id("vc1")
                        .type(VC_JSON)
                        .value(vcValue)
                        .build(),
                VCAttribute.builder()
                        .id("vc1")
                        .type(JWT_VC)
                        .value("ey24343...")
                        .build()
        );

        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", List.of()),
                new EntityAttribute<>("Property", mockVcs)
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
        when(objectMapper.convertValue(any(LinkedHashMap.class), eq(JsonNode.class)))
                .thenReturn(new ObjectMapper().valueToTree(vcValue));

        StepVerifier.create(userDataServiceImpl.getUserVCsInJson(userEntityString))
                .assertNext(credentialsBasicInfoWithExpiredDate -> {
                    assertEquals(1, credentialsBasicInfoWithExpiredDate.size());
                    CredentialsBasicInfoWithExpirationDate credentialsInfo = credentialsBasicInfoWithExpiredDate.get(0);
                    assertEquals("vc1", credentialsInfo.id());
                    assertEquals(List.of("VerifiableCredential", "SpecificCredentialType"), credentialsInfo.vcType());
                    assertEquals(List.of("jwt_vc"), credentialsInfo.availableFormats());
                    assertEquals("subjectId", credentialsInfo.credentialSubject().get("id").asText());
                    assertEquals(ZonedDateTime.parse("2024-04-07T09:57:59Z"), credentialsInfo.expirationDate());
                })
                .verifyComplete();
    }

    @Test
    void testGetUserVCsInJsonDateTimeParseException() throws Exception {
        String userEntityString = "userEntityJsonString";
        String invalidDate = "2024434-04-07T09:57:59Z";
        LinkedHashMap<String, Object> vcValue = new LinkedHashMap<>();
        vcValue.put("type", List.of("VerifiableCredential", "SpecificCredentialType"));
        vcValue.put(CREDENTIAL_SUBJECT, new LinkedHashMap<>(Map.of("id", "subjectId")));
        vcValue.put(EXPIRATION_DATE, invalidDate);

        List<VCAttribute> mockVcs = List.of(
                VCAttribute.builder()
                        .id("vc1")
                        .type(VC_JSON)
                        .value(vcValue)
                        .build(),
                VCAttribute.builder()
                        .id("vc1")
                        .type(JWT_VC)
                        .value("ey24343...")
                        .build()
        );

        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", List.of()),
                new EntityAttribute<>("Property", mockVcs)
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
        when(objectMapper.convertValue(any(LinkedHashMap.class), eq(JsonNode.class)))
                .thenReturn(new ObjectMapper().valueToTree(vcValue));

        StepVerifier.create(userDataServiceImpl.getUserVCsInJson(userEntityString))
                .expectError()
                .verify();
    }

    @Test
    void testGetVerifiableCredentialByIdAndFormat() throws Exception {
        String userEntityString = "userEntityJsonString";
        String vcId = "vc1";
        String format = VC_JSON;
        String expectedVCValue = "{\"id\":\"vc1\",\"type\":\"VC_JSON\",\"credentialSubject\":{\"id\":\"subjectId\"}}";

        VCAttribute vcAttribute = new VCAttribute(vcId, format, expectedVCValue);

        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", List.of()),
                new EntityAttribute<>("Property", List.of(vcAttribute))
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialByIdAndFormat(userEntityString, vcId, format))
                .expectNext(expectedVCValue)
                .verifyComplete();
    }

    @Test
    void testGetVerifiableCredentialByIdAndFormat_NotFound() throws JsonProcessingException {
        String userEntityString = "userEntityJsonString";
        String vcId = "vcNonExistent";

        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", List.of()),
                new EntityAttribute<>("Property", List.of()) // Without VCs
        );

        when(objectMapper.readValue(any(String.class), eq(UserEntity.class))).thenReturn(userEntity);

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialByIdAndFormat(userEntityString, vcId, VC_JSON))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void testGetDidsByUserEntity() throws JsonProcessingException {
        String userEntityString = "userEntityJsonString";
        List<DidAttribute> didAttributes = List.of(
                new DidAttribute("exampleMethod1", "did:example:123"),
                new DidAttribute("exampleMethod2", "did:example:456")
        );

        UserEntity userEntity = new UserEntity(
                "user123",
                "userEntity",
                new EntityAttribute<>("Property", didAttributes),
                new EntityAttribute<>("Property", List.of())
        );

        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);

        StepVerifier.create(userDataServiceImpl.getDidsByUserEntity(userEntityString))
                .expectNextMatches(dids -> {
                    assertEquals(2, dids.size());
                    assertEquals("did:example:123", dids.get(0));
                    assertEquals("did:example:456", dids.get(1));
                    return true;
                })
                .verifyComplete();
    }

}
