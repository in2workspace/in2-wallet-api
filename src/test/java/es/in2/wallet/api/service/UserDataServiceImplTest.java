//package es.in2.wallet.api.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectWriter;
//import es.in2.wallet.api.model.*;
//import es.in2.wallet.api.service.impl.UserDataServiceImpl;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.test.StepVerifier;
//
//import java.util.*;
//
//import static es.in2.wallet.api.util.MessageUtils.PROPERTY_TYPE;
//import static es.in2.wallet.api.util.MessageUtils.VC_JSON;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class UserDataServiceImplTest {
//
//    @Mock
//    private ObjectMapper objectMapper;
//
//    @InjectMocks
//    private UserDataServiceImpl userDataServiceImpl;
//
//    @Test
//    void testRegisterUserInContextBroker() throws JsonProcessingException {
//        String id = "123";
//
//        UserEntity expectedUserEntity = new UserEntity(
//                "urn:entities:userId:" + id,
//                "userEntity",
//                new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>()),
//                new EntityAttribute<>(PROPERTY_TYPE, new ArrayList<>())
//        );
//
//        ObjectWriter mockWriter = mock(ObjectWriter.class);
//        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
//        when(mockWriter.writeValueAsString(expectedUserEntity)).thenReturn("user entity");
//
//        // Executing the method under test
//        StepVerifier.create(userDataServiceImpl.createUserEntity(id))
//                .expectNext("user entity")
//                .verifyComplete();
//    }
//    @Test
//    void testSaveVC() throws JsonProcessingException {
//        // Sample JWT token for a verifiable credential
//        String vcJwt = "eyJraWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4jelEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6a2V5OnpEbmFlZnk3amhwY0ZCanp0TXJFSktFVHdFU0NoUXd4cEpuVUpLb3ZzWUQ1ZkpabXAiLCJuYmYiOjE2OTgxMzQ4NTUsImlzcyI6ImRpZDprZXk6elEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsImV4cCI6MTcwMDcyNjg1NSwiaWF0IjoxNjk4MTM0ODU1LCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWwiXSwiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL2RvbWUtbWFya2V0cGxhY2UuZXUvLzIwMjIvY3JlZGVudGlhbHMvbGVhcmNyZWRlbnRpYWwvdjEiXSwiaWQiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkiLCJpc3N1ZXIiOnsiaWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4ifSwiaXNzdWFuY2VEYXRlIjoiMjAyMy0xMC0yNFQwODowNzozNVoiLCJpc3N1ZWQiOiIyMDIzLTEwLTI0VDA4OjA3OjM1WiIsInZhbGlkRnJvbSI6IjIwMjMtMTAtMjRUMDg6MDc6MzVaIiwiZXhwaXJhdGlvbkRhdGUiOiIyMDIzLTExLTIzVDA4OjA3OjM1WiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmtleTp6RG5hZWZ5N2pocGNGQmp6dE1yRUpLRVR3RVNDaFF3eHBKblVKS292c1lENWZKWm1wIiwidGl0bGUiOiJNci4iLCJmaXJzdF9uYW1lIjoiSm9obiIsImxhc3RfbmFtZSI6IkRvZSIsImdlbmRlciI6Ik0iLCJwb3N0YWxfYWRkcmVzcyI6IiIsImVtYWlsIjoiam9obmRvZUBnb29kYWlyLmNvbSIsInRlbGVwaG9uZSI6IiIsImZheCI6IiIsIm1vYmlsZV9waG9uZSI6IiszNDc4NzQyNjYyMyIsImxlZ2FsUmVwcmVzZW50YXRpdmUiOnsiY24iOiI1NjU2NTY1NlYgSmVzdXMgUnVpeiIsInNlcmlhbE51bWJlciI6IjU2NTY1NjU2ViIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRFUy0xMjM0NTY3OCIsIm8iOiJHb29kQWlyIiwiYyI6IkVTIn0sInJvbGVzQW5kRHV0aWVzIjpbeyJ0eXBlIjoiTEVBUkNyZWRlbnRpYWwiLCJpZCI6Imh0dHBzOi8vZG9tZS1tYXJrZXRwbGFjZS5ldS8vbGVhci92MS82NDg0OTk0bjRyOWU5OTA0OTQifV0sImtleSI6InZhbHVlIn19LCJqdGkiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkifQ.2_YNY515CaohirD4AHDBMvzDagEn-p8uAsaiMT0H4ltK2uVfG8IWWqV_OOR6lFlXMzUhJd7nKsaWkhnAQY8kyA";
//        String jwtPayload = """
//                        {
//                          "sub": "did:key:zDnaefy7jhpcFBjztMrEJKETwESChQwxpJnUJKovsYD5fJZmp",
//                          "nbf": 1698134855,
//                          "iss": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
//                          "exp": 1700726855,
//                          "iat": 1698134855,
//                          "vc": {
//                            "type": [
//                              "VerifiableCredential",
//                              "LEARCredential"
//                            ],
//                            "@context": [
//                              "https://www.w3.org/2018/credentials/v1",
//                              "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
//                            ],
//                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
//                            "issuer": {
//                              "id": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn"
//                            },
//                            "issuanceDate": "2023-10-24T08:07:35Z",
//                            "issued": "2023-10-24T08:07:35Z",
//                            "validFrom": "2023-10-24T08:07:35Z",
//                            "expirationDate": "2023-11-23T08:07:35Z",
//                            "credentialSubject": {
//                              "id": "did:key:zDnaefy7jhpcFBjztMrEJKETwESChQwxpJnUJKovsYD5fJZmp",
//                              "title": "Mr.",
//                              "first_name": "John",
//                              "last_name": "Doe",
//                              "gender": "M",
//                              "postal_address": "",
//                              "email": "johndoe@goodair.com",
//                              "telephone": "",
//                              "fax": "",
//                              "mobile_phone": "+34787426623",
//                              "legalRepresentative": {
//                                "cn": "56565656V Jesus Ruiz",
//                                "serialNumber": "56565656V",
//                                "organizationIdentifier": "VATES-12345678",
//                                "o": "GoodAir",
//                                "c": "ES"
//                              },
//                              "rolesAndDuties": [
//                                {
//                                  "type": "LEARCredential",
//                                  "id": "https://dome-marketplace.eu//lear/v1/6484994n4r9e990494"
//                                }
//                              ],
//                              "key": "value"
//                            }
//                          },
//                          "jti": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9"
//                        }
//                """;
//
//        // Sample JSON response entity
//        UserEntity mockUserEntity = UserEntity.builder()
//                .id("urn:entities:userId:1234")
//                .type("userEntity")
//                .dids(EntityAttribute.<List<DidAttribute>>builder()
//                        .type("Property")
//                        .value(List.of())
//                        .build())
//                .vcs(EntityAttribute.<List<VCAttribute>>builder()
//                        .type("Property")
//                        .value(List.of())
//                        .build())
//                .build();
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(mockUserEntity);
//        ObjectWriter mockWriter = mock(ObjectWriter.class);
//        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
//        when(mockWriter.writeValueAsString(any())).thenReturn("user entity with updated credential");
//
//        ObjectMapper objectMapper2 = new ObjectMapper();
//        JsonNode jsonNode = objectMapper2.readTree(jwtPayload);
//
//        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);
//
//
//        // Executing the method under test
//        StepVerifier.create(userDataServiceImpl.saveVC("entity not updated", vcJwt))
//                .expectNext("user entity with updated credential")
//                .verifyComplete();
//    }
//
//
//    @Test
//    void testGetSelectableVCsByVcTypeList() throws JsonProcessingException {
//        List<String> vcTypeList = List.of("SpecificCredentialType", "LEARCredential");
//
//        // User entity JSON string as provided in your test setup
//        String userEntityString = "userEntity";
//
//        // Mock VCs
//        List<VCAttribute> mockVcs = List.of(
//                VCAttribute.builder()
//                        .id("vc1")
//                        .type("vc_json")
//                        .value(Map.of(
//                                "id", "vc1",
//                                "type", List.of("VerifiableCredential", "SpecificCredentialType"),
//                                "credentialSubject", Map.of("name", "John Doe")
//                        ))
//                        .build(),
//                VCAttribute.builder()
//                        .id("vc2")
//                        .type("vc_json")
//                        .value(Map.of(
//                                "id", "vc2",
//                                "type", List.of("VerifiableCredential", "LEARCredential"),
//                                "credentialSubject", Map.of("name", "John Doe", "age", "25")
//                        ))
//                        .build()
//        );
//
//        UserEntity userEntity = UserEntity.builder()
//                .id("urn:entities:userId:1234")
//                .type("userEntity")
//                .dids(new EntityAttribute<>("Property", new ArrayList<>()))
//                .vcs(new EntityAttribute<>("Property", mockVcs))
//                .build();
//
//        // Deserializing userEntityJson into UserEntity
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//
//        when(objectMapper.convertValue(any(Map.class), eq(JsonNode.class)))
//                .thenAnswer(invocation -> {
//                    Map<?, ?> vcDataValue = invocation.getArgument(0);
//                    return new ObjectMapper().valueToTree(vcDataValue);
//                });
//
//
//        StepVerifier.create(userDataServiceImpl.getSelectableVCsByVcTypeList(vcTypeList, userEntityString))
//                .assertNext(vcBasicDataDTOList -> {
//                    assertEquals(2, vcBasicDataDTOList.size());
//
//                    CredentialsBasicInfo vc1Info = vcBasicDataDTOList.get(0);
//                    assertEquals("vc1", vc1Info.id());
//                    assertTrue(vc1Info.vcType().contains("SpecificCredentialType"));
//
//                    CredentialsBasicInfo vc2Info = vcBasicDataDTOList.get(1);
//                    assertEquals("vc2", vc2Info.id());
//                    assertTrue(vc2Info.vcType().contains("LEARCredential"));
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void testExtractDidFromVerifiableCredential() throws JsonProcessingException {
//        String userEntityString = "userEntity";
//        String vcId = "vc1";
//        String expectedDid = "did:example:123";
//
//        // Mock VC con DID
//        VCAttribute vcAttributeWithDid = VCAttribute.builder()
//                .id(vcId)
//                .type("vc_json")
//                .value(Map.of(
//                        "id", vcId,
//                        "type", List.of("VerifiableCredential", "SpecificCredentialType"),
//                        "credentialSubject", Map.of("id", expectedDid)
//                ))
//                .build();
//
//        List<VCAttribute> vcAttributes = List.of(vcAttributeWithDid);
//
//        UserEntity userEntity = UserEntity.builder()
//                .id("urn:entities:userId:1234")
//                .type("userEntity")
//                .dids(new EntityAttribute<>("Property", new ArrayList<>()))
//                .vcs(new EntityAttribute<>("Property", vcAttributes))
//                .build();
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//        when(objectMapper.convertValue(any(Map.class), eq(JsonNode.class)))
//                .thenAnswer(invocation -> new ObjectMapper().valueToTree(invocation.getArgument(0)));
//
//        StepVerifier.create(userDataServiceImpl.extractDidFromVerifiableCredential(userEntityString, vcId))
//                .expectNext(expectedDid)
//                .verifyComplete();
//    }
//
//    @Test
//    void testDeleteVerifiableCredential() throws JsonProcessingException {
//        String userEntityId = "userEntity";
//        String vcIdToDelete = "vc1";
//        String didToDelete = "did:example:123";
//        String updatedUserEntityJson = "user entity with the deleted credential";
//
//        UserEntity userEntity = new UserEntity(
//                userEntityId,
//                "userEntity",
//                new EntityAttribute<>("Property", List.of(new DidAttribute("didType", didToDelete))),
//                new EntityAttribute<>("Property", List.of(new VCAttribute(vcIdToDelete, "vc_json", Map.of("id", vcIdToDelete))))
//        );
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//
//        ObjectWriter mockWriter = mock(ObjectWriter.class);
//        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
//        when(mockWriter.writeValueAsString(any())).thenReturn(updatedUserEntityJson);
//
//        StepVerifier.create(userDataServiceImpl.deleteVerifiableCredential(userEntityId, vcIdToDelete, didToDelete))
//                .expectNextMatches(response -> response.equals(updatedUserEntityJson))
//                .verifyComplete();
//    }
//
//    @Test
//    void testSaveDid() throws JsonProcessingException {
//        String userEntityString = "userEntity";
//        String newDid = "did:key:123";
//        String didMethod = "key";
//        String updatedUserEntityJson = "user entity with the deleted did";
//
//        List<DidAttribute> initialDids = List.of(new DidAttribute("key", "did:key:456"));
//        UserEntity userEntity = new UserEntity(
//                "user123",
//                "userEntity",
//                new EntityAttribute<>("Property", initialDids),
//                new EntityAttribute<>("Property", new ArrayList<>())
//        );
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//
//        ObjectWriter mockWriter = mock(ObjectWriter.class);
//        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
//        when(mockWriter.writeValueAsString(any())).thenReturn(updatedUserEntityJson);
//
//        StepVerifier.create(userDataServiceImpl.saveDid(userEntityString, newDid, didMethod))
//                .expectNextMatches(response -> response.equals(updatedUserEntityJson))
//                .verifyComplete();
//    }
//
//    @Test
//    void testDeleteDid() throws JsonProcessingException {
//        String userEntityString = "userEntity";
//        String selectedDid = "did:key:123";
//        String updatedUserEntityJson = "user entity with updated did";
//
//        List<DidAttribute> didsList = List.of(new DidAttribute("key", selectedDid));
//        UserEntity userEntity = new UserEntity(
//                "user123",
//                "userEntity",
//                new EntityAttribute<>("Property", didsList),
//                new EntityAttribute<>("Property", new ArrayList<>())
//        );
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//
//        ObjectWriter mockWriter = mock(ObjectWriter.class);
//        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
//        when(mockWriter.writeValueAsString(any())).thenReturn(updatedUserEntityJson);
//
//        StepVerifier.create(userDataServiceImpl.deleteSelectedDidFromUserEntity(selectedDid,userEntityString))
//                .expectNextMatches(response -> response.equals(updatedUserEntityJson))
//                .verifyComplete();
//    }
//
//    @Test
//    void testGetUserVCsInJson() throws Exception {
//        String userEntityString = "userEntityJsonString";
//        LinkedHashMap<String, Object> vcValue = new LinkedHashMap<>();
//        vcValue.put("type", List.of("VerifiableCredential", "SpecificCredentialType"));
//        vcValue.put("credentialSubject", new LinkedHashMap<>(Map.of("id", "subjectId")));
//
//        VCAttribute vcAttribute = new VCAttribute("vcId", VC_JSON, vcValue);
//
//        UserEntity userEntity = new UserEntity(
//                "user123",
//                "userEntity",
//                new EntityAttribute<>("Property", List.of()),
//                new EntityAttribute<>("Property", List.of(vcAttribute))
//        );
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//        when(objectMapper.convertValue(any(LinkedHashMap.class), eq(JsonNode.class)))
//                .thenReturn(new ObjectMapper().valueToTree(vcValue));
//
//        StepVerifier.create(userDataServiceImpl.getUserVCsInJson(userEntityString))
//                .assertNext(credentialsBasicInfos -> {
//                    assertEquals(1, credentialsBasicInfos.size());
//                    CredentialsBasicInfo credentialsInfo = credentialsBasicInfos.get(0);
//                    assertEquals("vcId", credentialsInfo.id());
//                    assertEquals(List.of("VerifiableCredential", "SpecificCredentialType"), credentialsInfo.vcType());
//                    assertEquals("subjectId", credentialsInfo.credentialSubject().get("id").asText());
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void testGetVerifiableCredentialByIdAndFormat() throws Exception {
//        String userEntityString = "userEntityJsonString";
//        String vcId = "vc1";
//        String format = VC_JSON;
//        String expectedVCValue = "{\"id\":\"vc1\",\"type\":\"VC_JSON\",\"credentialSubject\":{\"id\":\"subjectId\"}}";
//
//        VCAttribute vcAttribute = new VCAttribute(vcId, format, expectedVCValue);
//
//        UserEntity userEntity = new UserEntity(
//                "user123",
//                "userEntity",
//                new EntityAttribute<>("Property", List.of()),
//                new EntityAttribute<>("Property", List.of(vcAttribute))
//        );
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//
//        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialByIdAndFormat(userEntityString, vcId, format))
//                .expectNext(expectedVCValue)
//                .verifyComplete();
//    }
//
//    @Test
//    void testGetVerifiableCredentialByIdAndFormat_NotFound() throws JsonProcessingException {
//        String userEntityString = "userEntityJsonString";
//        String vcId = "vcNonExistent";
//
//        UserEntity userEntity = new UserEntity(
//                "user123",
//                "userEntity",
//                new EntityAttribute<>("Property", List.of()),
//                new EntityAttribute<>("Property", List.of()) // Without VCs
//        );
//
//        when(objectMapper.readValue(any(String.class), eq(UserEntity.class))).thenReturn(userEntity);
//
//        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialByIdAndFormat(userEntityString, vcId, VC_JSON))
//                .expectError(NoSuchElementException.class)
//                .verify();
//    }
//
//    @Test
//    void testGetDidsByUserEntity() throws JsonProcessingException {
//        String userEntityString = "userEntityJsonString";
//        List<DidAttribute> didAttributes = List.of(
//                new DidAttribute("exampleMethod1", "did:example:123"),
//                new DidAttribute("exampleMethod2", "did:example:456")
//        );
//
//        UserEntity userEntity = new UserEntity(
//                "user123",
//                "userEntity",
//                new EntityAttribute<>("Property", didAttributes),
//                new EntityAttribute<>("Property", List.of())
//        );
//
//        when(objectMapper.readValue(anyString(), eq(UserEntity.class))).thenReturn(userEntity);
//
//        StepVerifier.create(userDataServiceImpl.getDidsByUserEntity(userEntityString))
//                .expectNextMatches(dids -> {
//                    assertEquals(2, dids.size());
//                    assertEquals("did:example:123", dids.get(0));
//                    assertEquals("did:example:456", dids.get(1));
//                    return true;
//                })
//                .verifyComplete();
//    }
//
//}
