package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.domain.exception.ParseErrorException;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.impl.DataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static es.in2.wallet.domain.util.ApplicationConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BrokerService brokerService;

    @InjectMocks
    private DataServiceImpl userDataServiceImpl;

    @Test
    void testRegisterUserInContextBroker() throws JsonProcessingException {
        String id = "123";

        WalletUser expectedWalletUser = WalletUser.builder().id(USER_ENTITY_PREFIX + id).type(WALLET_USER_TYPE).build();

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(expectedWalletUser)).thenReturn("user entity");

        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.createUserEntity(id))
                .expectNext("user entity")
                .verifyComplete();
    }
    @Test
    void testRegisterUserInContextBrokerErrorWhileDeserializingEntity() throws JsonProcessingException {
        String id = "123";

        WalletUser expectedWalletUser = WalletUser.builder().id(USER_ENTITY_PREFIX + id).type(WALLET_USER_TYPE).build();

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(expectedWalletUser)).thenThrow(new JsonProcessingException("Error") {});

        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.createUserEntity(id))
                .expectError(ParseErrorException.class)
                .verify();
    }
    @Test
    void testSaveVCWithJwtFormatNewEntity() throws JsonProcessingException {
        String processId = "123";
        // Sample JWT token for a verifiable credential
        String vcJwt = "eyJraWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4jelEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6a2V5OnpEbmFlZnk3amhwY0ZCanp0TXJFSktFVHdFU0NoUXd4cEpuVUpLb3ZzWUQ1ZkpabXAiLCJuYmYiOjE2OTgxMzQ4NTUsImlzcyI6ImRpZDprZXk6elEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsImV4cCI6MTcwMDcyNjg1NSwiaWF0IjoxNjk4MTM0ODU1LCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWwiXSwiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL2RvbWUtbWFya2V0cGxhY2UuZXUvLzIwMjIvY3JlZGVudGlhbHMvbGVhcmNyZWRlbnRpYWwvdjEiXSwiaWQiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkiLCJpc3N1ZXIiOnsiaWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4ifSwiaXNzdWFuY2VEYXRlIjoiMjAyMy0xMC0yNFQwODowNzozNVoiLCJpc3N1ZWQiOiIyMDIzLTEwLTI0VDA4OjA3OjM1WiIsInZhbGlkRnJvbSI6IjIwMjMtMTAtMjRUMDg6MDc6MzVaIiwiZXhwaXJhdGlvbkRhdGUiOiIyMDIzLTExLTIzVDA4OjA3OjM1WiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmtleTp6RG5hZWZ5N2pocGNGQmp6dE1yRUpLRVR3RVNDaFF3eHBKblVKS292c1lENWZKWm1wIiwidGl0bGUiOiJNci4iLCJmaXJzdF9uYW1lIjoiSm9obiIsImxhc3RfbmFtZSI6IkRvZSIsImdlbmRlciI6Ik0iLCJwb3N0YWxfYWRkcmVzcyI6IiIsImVtYWlsIjoiam9obmRvZUBnb29kYWlyLmNvbSIsInRlbGVwaG9uZSI6IiIsImZheCI6IiIsIm1vYmlsZV9waG9uZSI6IiszNDc4NzQyNjYyMyIsImxlZ2FsUmVwcmVzZW50YXRpdmUiOnsiY24iOiI1NjU2NTY1NlYgSmVzdXMgUnVpeiIsInNlcmlhbE51bWJlciI6IjU2NTY1NjU2ViIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRFUy0xMjM0NTY3OCIsIm8iOiJHb29kQWlyIiwiYyI6IkVTIn0sInJvbGVzQW5kRHV0aWVzIjpbeyJ0eXBlIjoiTEVBUkNyZWRlbnRpYWwiLCJpZCI6Imh0dHBzOi8vZG9tZS1tYXJrZXRwbGFjZS5ldS8vbGVhci92MS82NDg0OTk0bjRyOWU5OTA0OTQifV0sImtleSI6InZhbHVlIn19LCJqdGkiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkifQ.2_YNY515CaohirD4AHDBMvzDagEn-p8uAsaiMT0H4ltK2uVfG8IWWqV_OOR6lFlXMzUhJd7nKsaWkhnAQY8kyA";
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

        CredentialResponse credentials= CredentialResponse.builder().credential(vcJwt).format(JWT_VC).build();

        when(brokerService.getEntityById(eq(processId),anyString())).thenReturn(Mono.just(Optional.empty()));
        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn("user entity with updated credential");

        ObjectMapper objectMapper2 = new ObjectMapper();
        JsonNode jsonNode = objectMapper2.readTree(jwtPayload);

        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);


        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.saveVC(processId,"entity not updated", credentials))
                .expectNext("user entity with updated credential")
                .verifyComplete();
    }

    @Test
    void testSaveVCWithUnsupportedFormat() {
        String processId = "123";
        // Sample JWT token for a verifiable credential
        String vcOnWrongFormat = "dskdsakd";

        CredentialResponse credentials= CredentialResponse.builder().credential(vcOnWrongFormat).format("html_vc").build();

        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.saveVC(processId,"entity", credentials))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testSaveVCWithJwtFormatWhenEntityCredentialAlreadyExist() throws JsonProcessingException {
        String processId = "123";
        String vcCbor = "dsadsadasd";
        // Sample JWT token for a verifiable credential
        String vcJwt = "eyJraWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4jelEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6a2V5OnpEbmFlZnk3amhwY0ZCanp0TXJFSktFVHdFU0NoUXd4cEpuVUpLb3ZzWUQ1ZkpabXAiLCJuYmYiOjE2OTgxMzQ4NTUsImlzcyI6ImRpZDprZXk6elEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsImV4cCI6MTcwMDcyNjg1NSwiaWF0IjoxNjk4MTM0ODU1LCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWwiXSwiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL2RvbWUtbWFya2V0cGxhY2UuZXUvLzIwMjIvY3JlZGVudGlhbHMvbGVhcmNyZWRlbnRpYWwvdjEiXSwiaWQiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkiLCJpc3N1ZXIiOnsiaWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4ifSwiaXNzdWFuY2VEYXRlIjoiMjAyMy0xMC0yNFQwODowNzozNVoiLCJpc3N1ZWQiOiIyMDIzLTEwLTI0VDA4OjA3OjM1WiIsInZhbGlkRnJvbSI6IjIwMjMtMTAtMjRUMDg6MDc6MzVaIiwiZXhwaXJhdGlvbkRhdGUiOiIyMDIzLTExLTIzVDA4OjA3OjM1WiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmtleTp6RG5hZWZ5N2pocGNGQmp6dE1yRUpLRVR3RVNDaFF3eHBKblVKS292c1lENWZKWm1wIiwidGl0bGUiOiJNci4iLCJmaXJzdF9uYW1lIjoiSm9obiIsImxhc3RfbmFtZSI6IkRvZSIsImdlbmRlciI6Ik0iLCJwb3N0YWxfYWRkcmVzcyI6IiIsImVtYWlsIjoiam9obmRvZUBnb29kYWlyLmNvbSIsInRlbGVwaG9uZSI6IiIsImZheCI6IiIsIm1vYmlsZV9waG9uZSI6IiszNDc4NzQyNjYyMyIsImxlZ2FsUmVwcmVzZW50YXRpdmUiOnsiY24iOiI1NjU2NTY1NlYgSmVzdXMgUnVpeiIsInNlcmlhbE51bWJlciI6IjU2NTY1NjU2ViIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRFUy0xMjM0NTY3OCIsIm8iOiJHb29kQWlyIiwiYyI6IkVTIn0sInJvbGVzQW5kRHV0aWVzIjpbeyJ0eXBlIjoiTEVBUkNyZWRlbnRpYWwiLCJpZCI6Imh0dHBzOi8vZG9tZS1tYXJrZXRwbGFjZS5ldS8vbGVhci92MS82NDg0OTk0bjRyOWU5OTA0OTQifV0sImtleSI6InZhbHVlIn19LCJqdGkiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkifQ.2_YNY515CaohirD4AHDBMvzDagEn-p8uAsaiMT0H4ltK2uVfG8IWWqV_OOR6lFlXMzUhJd7nKsaWkhnAQY8kyA";
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

        CredentialResponse credentials= CredentialResponse.builder().credential(vcJwt).format(JWT_VC).build();

        when(brokerService.getEntityById(eq(processId),anyString())).thenReturn(Mono.just(Optional.of("credential entity")));

        ObjectMapper objectMapper2 = new ObjectMapper();
        JsonNode jsonNode = objectMapper2.readTree(jwtPayload);

        when(objectMapper.readTree(anyString())).thenReturn(jsonNode);

        CredentialEntity existentCredentialEntity = CredentialEntity.builder()
                .id("123")
                .type(CREDENTIAL_TYPE)
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jwtPayload))
                .cwtCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,vcCbor))
                .credentialTypeAttribute(new CredentialTypeAttribute(PROPERTY_TYPE,List.of("VerifiableCredential",
                        "LEARCredential")))
                .credentialStatusAttribute(new CredentialStatusAttribute(PROPERTY_TYPE,CredentialStatus.VALID))
                .relationshipAttribute(new RelationshipAttribute(RELATIONSHIP_TYPE,"walletUser:1234"))
                .build();
        when(objectMapper.readValue("credential entity", CredentialEntity.class)).thenReturn(existentCredentialEntity);

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any())).thenReturn("user entity with updated credential");

        // Executing the method under test
        StepVerifier.create(userDataServiceImpl.saveVC(processId,"entity not updated", credentials))
                .expectNext("user entity with updated credential")
                .verifyComplete();
    }

    @Test
    void testExtractDidFromVerifiableCredential() throws JsonProcessingException {
        String credential = "credentialEntity";
        String vcId = "vc1";
        String expectedDid = "did:example:123";
        String jsonCredential = """
                        {
                            "type": [
                              "VerifiableCredential",
                              "ExampleCredential"
                            ],
                            "@context": [
                              "https://www.w3.org/2018/credentials/v1",
                              "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                            "issuer": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                              "id": "did:example:123",
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
                              ]
                            }
                          }
                """;

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id(vcId)
                .type("Credential")
                .credentialTypeAttribute(CredentialTypeAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .value(List.of("VerifiableCredential","ExampleCredential"))
                        .build())
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jsonCredential))
                .build();

        JsonNode jsonNode = new ObjectMapper().readTree(jsonCredential);
        when(objectMapper.readValue(credential, CredentialEntity.class)).thenReturn(credentialEntity);
        when(objectMapper.convertValue(credentialEntity.jsonCredentialAttribute().value(), JsonNode.class)).thenReturn(jsonNode);


        StepVerifier.create(userDataServiceImpl.extractDidFromVerifiableCredential(credential))
                .expectNext(expectedDid)
                .verifyComplete();
    }

    @Test
    void testExtractDidFromVerifiableCredentialLEARCredentialEmployeeType() throws JsonProcessingException {
        String credential = "credentialEntity";
        String vcId = "vc1";
        String expectedDid = "did:example:123";
        String jsonCredential = """
                        {
                            "type": [
                              "VerifiableCredential",
                              "LEARCredentialEmployee"
                            ],
                            "@context": [
                              "https://www.w3.org/2018/credentials/v1",
                              "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                            "issuer": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                              "mandate" : {
                                "mandatee" : {
                                    "id" : "did:example:123"
                                }
                              }
                              },
                              "rolesAndDuties": [
                                {
                                  "type": "LEARCredential",
                                  "id": "https://dome-marketplace.eu//lear/v1/6484994n4r9e990494"
                                }
                              ]
                            }
                          }
                """;

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id(vcId)
                .type("Credential")
                .credentialTypeAttribute(CredentialTypeAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .value(List.of("VerifiableCredential","LEARCredentialEmployee"))
                        .build())
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jsonCredential))
                .build();

        JsonNode jsonNode = new ObjectMapper().readTree(jsonCredential);
        when(objectMapper.readValue(credential, CredentialEntity.class)).thenReturn(credentialEntity);
        when(objectMapper.convertValue(credentialEntity.jsonCredentialAttribute().value(), JsonNode.class)).thenReturn(jsonNode);


        StepVerifier.create(userDataServiceImpl.extractDidFromVerifiableCredential(credential))
                .expectNext(expectedDid)
                .verifyComplete();
    }


    @Test
    void testGetUserVCsInJson() throws Exception {
        String credentialsJson = "credentialsJson";
        String userEntityId = "urn:walletUser:f517af8f-954e-47c2-a778-dd312154dc2d";

        String jwtCredential = "eysdasda";
        String jsonCredential = """
                        {
                            "type": [
                                "VerifiableCredential",
                                "LEARCredentialEmployee"
                            ],
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                            "issuer": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                                "id": "did:example:123"
                            },
                            "rolesAndDuties": [
                                {
                                    "type": "LEARCredential",
                                    "id": "https://dome-marketplace.eu//lear/v1/6484994n4r9e990494"
                                }
                            ]
                        }
                """;

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id("vc1")
                .type("Credential")
                .credentialTypeAttribute(CredentialTypeAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .value(List.of("VerifiableCredential","LEARCredentialEmployee"))
                        .build())
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jsonCredential))
                .credentialStatusAttribute(CredentialStatusAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .credentialStatus(CredentialStatus.VALID).build())
                .jwtCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jwtCredential))
                .relationshipAttribute(new RelationshipAttribute(RELATIONSHIP_TYPE,userEntityId))
                .build();

        List<CredentialEntity> credentials = List.of(credentialEntity);

        JsonNode jsonNode = new ObjectMapper().readTree(jsonCredential);
        when(objectMapper.readValue(eq(credentialsJson), any(TypeReference.class))).thenReturn(credentials);
        when(objectMapper.convertValue(credentialEntity.jsonCredentialAttribute().value(), JsonNode.class)).thenReturn(jsonNode);


        StepVerifier.create(userDataServiceImpl.getUserVCsInJson(credentialsJson))
                .assertNext(credentialsBasicInfoWithExpiredDate -> {
                    assertEquals(1, credentialsBasicInfoWithExpiredDate.size());
                    CredentialsBasicInfo credentialsInfo = credentialsBasicInfoWithExpiredDate.get(0);
                    assertEquals("vc1", credentialsInfo.id());
                    assertEquals(List.of("VerifiableCredential", "LEARCredentialEmployee"), credentialsInfo.vcType());
                    assertEquals(List.of(VC_JSON,JWT_VC), credentialsInfo.availableFormats());
                    assertEquals(CredentialStatus.VALID, credentialsInfo.credentialStatus());
                    assertEquals("did:example:123", credentialsInfo.credentialSubject().get("id").asText());
                    assertEquals(ZonedDateTime.parse("2023-11-23T08:07:35Z"), credentialsInfo.expirationDate());
                    assertEquals(userEntityId, credentialEntity.relationshipAttribute().object());
                })
                .verifyComplete();
    }
    @Test
    void testGetUserVCsInJsonProcessingError() throws Exception {
        String credentialsJson = "credentialsJson";
        when(objectMapper.readValue(eq(credentialsJson), any(TypeReference.class))).thenThrow(new JsonProcessingException("Error") {});


        StepVerifier.create(userDataServiceImpl.getUserVCsInJson(credentialsJson))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testGetVerifiableCredentialOnSupportedFormat() throws JsonProcessingException {
        String credentialJson = "credentialsJson";

        String jwtCredential = "eysdasda";
        String cwtCredential = "fsafasf";
        String jsonCredential = """
                        {
                            "type": [
                                "VerifiableCredential",
                                "LEARCredentialEmployee"
                            ],
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "urn:uuid:8700bee5-4621-4720-94de-e866fb7197e9",
                            "issuer": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                                "id": "did:example:123"
                            }
                        }
                """;

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id("vc1")
                .type("Credential")
                .credentialTypeAttribute(CredentialTypeAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .value(List.of("VerifiableCredential","LEARCredentialEmployee"))
                        .build())
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jsonCredential))
                .cwtCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,cwtCredential))
                .credentialStatusAttribute(CredentialStatusAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .credentialStatus(CredentialStatus.VALID).build())
                .jwtCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jwtCredential))
                .build();

        when(objectMapper.readValue(credentialJson, CredentialEntity.class)).thenReturn(credentialEntity);

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialOnRequestedFormat(credentialJson, JWT_VC))
                .expectNext(jwtCredential)
                .verifyComplete();

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialOnRequestedFormat(credentialJson, VC_CWT))
                .expectNext(cwtCredential)
                .verifyComplete();

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialOnRequestedFormat(credentialJson, VC_JSON))
                .expectNext(jsonCredential)
                .verifyComplete();
    }

    @Test
    void testGetVerifiableCredentialOnUnsupportedFormat() {
        String credentialJson = "{\"some\":\"data\"}";

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialOnRequestedFormat(credentialJson, "unsupported_format"))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Unsupported credential format requested: unsupported_format"))
                .verify();
    }

    @Test
    void testGetVerifiableCredentialOnFormatNotFound() throws JsonProcessingException {
        String credentialJson = "{\"jsonCredentialAttribute\": {\"value\": null}}";
        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id("vc1")
                .type("Credential")
                .credentialTypeAttribute(CredentialTypeAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .value(List.of("VerifiableCredential","LEARCredentialEmployee"))
                        .build())
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,null))
                .credentialStatusAttribute(CredentialStatusAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .credentialStatus(CredentialStatus.VALID).build())
                .build();

        when(objectMapper.readValue(credentialJson, CredentialEntity.class)).thenReturn(credentialEntity);

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialOnRequestedFormat(credentialJson, VC_JSON))
                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                        throwable.getMessage().contains("Credential format not found or is null: json"))
                .verify();
    }

    @Test
    void testGetVerifiableCredentialJsonProcessingException() throws JsonProcessingException {
        String credentialJson = "{\"invalid\":\"json\"}";

        when(objectMapper.readValue(credentialJson, CredentialEntity.class))
                .thenThrow(new JsonProcessingException("Parsing error") {});

        StepVerifier.create(userDataServiceImpl.getVerifiableCredentialOnRequestedFormat(credentialJson, VC_JSON))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Error deserializing CredentialEntity from JSON"))
                .verify();
    }
    @Test
    void testSaveTransaction() throws JsonProcessingException {
        String credentialId = "cred123";
        String transactionId = "trans456";
        String accessToken = "access789";
        String deferredEndpoint = "https://example.com/callback";

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any(TransactionEntity.class))).thenReturn("transaction entity");

        StepVerifier.create(userDataServiceImpl.saveTransaction(credentialId, transactionId, accessToken, deferredEndpoint))
                .expectNext("transaction entity")
                .verifyComplete();

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(mockWriter).writeValueAsString(captor.capture());
        TransactionEntity capturedEntity = captor.getValue();
        assertNotNull(capturedEntity.id());
        assertTrue(capturedEntity.id().startsWith(TRANSACTION_ENTITY_PREFIX));
        assertEquals(TRANSACTION_TYPE, capturedEntity.type());
        assertEquals(transactionId, capturedEntity.transactionDataAttribute().value().transactionId());
        assertEquals(accessToken, capturedEntity.transactionDataAttribute().value().accessToken());
        assertEquals(deferredEndpoint, capturedEntity.transactionDataAttribute().value().deferredEndpoint());
        assertEquals(CREDENTIAL_ENTITY_PREFIX + credentialId, capturedEntity.relationshipAttribute().object());
    }

    @Test
    void testUpdateVCEntityWithSignedJWTFormat() throws JsonProcessingException {
        String credentialEntityJson = "{\"id\":\"cred123\", \"type\":\"Credential\"}";
        String signedJwt = "signedJwtExample";
        CredentialResponse signedCredential = CredentialResponse.builder().credential(signedJwt).format(JWT_VC).build();

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id("cred123")
                .type("Credential")
                .build();

        when(objectMapper.readValue(credentialEntityJson, CredentialEntity.class)).thenReturn(credentialEntity);
        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any(CredentialEntity.class))).thenReturn("updatedEntityJson");

        StepVerifier.create(userDataServiceImpl.updateVCEntityWithSignedFormat(credentialEntityJson, signedCredential))
                .expectNext("updatedEntityJson")
                .verifyComplete();

        verify(objectMapper).readValue(credentialEntityJson, CredentialEntity.class);
    }

    @Test
    void testUpdateVCEntityWithSignedCWTFormat() throws JsonProcessingException {
        String credentialEntityJson = "{\"id\":\"cred123\", \"type\":\"Credential\"}";
        String signedCwt = "signedCwtExample";
        CredentialResponse signedCredential = CredentialResponse.builder().credential(signedCwt).format(VC_CWT).build();

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id("cred123")
                .type("Credential")
                .build();

        when(objectMapper.readValue(credentialEntityJson, CredentialEntity.class)).thenReturn(credentialEntity);
        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(any(CredentialEntity.class))).thenReturn("updatedEntityJson");

        StepVerifier.create(userDataServiceImpl.updateVCEntityWithSignedFormat(credentialEntityJson, signedCredential))
                .expectNext("updatedEntityJson")
                .verifyComplete();

        verify(objectMapper).readValue(credentialEntityJson, CredentialEntity.class);
    }

    @Test
    void testUpdateTransactionWithNewTransactionId() throws JsonProcessingException {
        String transactionEntityJson = "{\"id\":\"trans123\", \"type\":\"Transaction\", \"transactionDataAttribute\":{\"type\":\"Property\", \"value\":{\"transactionId\":\"oldTransId\", \"accessToken\":\"access123\", \"deferredEndpoint\":\"https://example.com/callback\"}}, \"relationshipAttribute\":{\"type\":\"Relationship\", \"object\":\"cred123\"}}";
        String newTransactionId = "newTransId";
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .id("trans123")
                .type("Transaction")
                .transactionDataAttribute(EntityAttribute.<TransactionDataAttribute>builder()
                        .type("Property")
                        .value(TransactionDataAttribute.builder()
                                .transactionId("oldTransId")
                                .accessToken("access123")
                                .deferredEndpoint("https://example.com/callback")
                                .build())
                        .build())
                .relationshipAttribute(RelationshipAttribute.builder()
                        .type("Relationship")
                        .object("cred123")
                        .build())
                .build();

        TransactionEntity updatedTransactionEntity = TransactionEntity.builder()
                .id("trans123")
                .type("Transaction")
                .transactionDataAttribute(EntityAttribute.<TransactionDataAttribute>builder()
                        .type("Property")
                        .value(TransactionDataAttribute.builder()
                                .transactionId(newTransactionId)
                                .accessToken("access123")
                                .deferredEndpoint("https://example.com/callback")
                                .build())
                        .build())
                .relationshipAttribute(RelationshipAttribute.builder()
                        .type("Relationship")
                        .object("cred123")
                        .build())
                .build();

        when(objectMapper.readValue(transactionEntityJson, TransactionEntity.class)).thenReturn(transactionEntity);
        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(updatedTransactionEntity)).thenReturn("updatedTransactionJson");

        StepVerifier.create(userDataServiceImpl.updateTransactionWithNewTransactionId(transactionEntityJson, newTransactionId))
                .expectNext("updatedTransactionJson")
                .verifyComplete();

        verify(objectMapper).readValue(transactionEntityJson, TransactionEntity.class);
    }

    @Test
    void testUpdateTransactionWithNewTransactionIdJsonProcessingException() throws JsonProcessingException {
        String transactionEntityJson = "{\"id\":\"trans123\"}";
        String newTransactionId = "newTransId";

        when(objectMapper.readValue(transactionEntityJson, TransactionEntity.class)).thenThrow(new JsonProcessingException("Error processing JSON") {});

        StepVerifier.create(userDataServiceImpl.updateTransactionWithNewTransactionId(transactionEntityJson, newTransactionId))
                .expectError(ParseErrorException.class)
                .verify();

        verify(objectMapper).readValue(transactionEntityJson, TransactionEntity.class);
    }

    @Test
    void testSaveDOMEUnsignedCredential() throws JsonProcessingException {
        String userId = "123";
        String jsonCredential = """
                        {
                            "type": [
                                "VerifiableCredential",
                                "LEARCredentialEmployee"
                            ],
                            "@context": [
                                "https://www.w3.org/2018/credentials/v1",
                                "https://dome-marketplace.eu//2022/credentials/learcredential/v1"
                            ],
                            "id": "vc1",
                            "issuer": "did:key:zQ3shtcEQP3yuxbkZ1SjN51T8fQmRyXnjrXm8E84WLXKDQbRn",
                            "issuanceDate": "2023-10-24T08:07:35Z",
                            "issued": "2023-10-24T08:07:35Z",
                            "validFrom": "2023-10-24T08:07:35Z",
                            "expirationDate": "2023-11-23T08:07:35Z",
                            "credentialSubject": {
                                "id": "did:example:123"
                            }
                        }
                """;

        JsonNode jsonNode = new ObjectMapper().readTree(jsonCredential);

        CredentialEntity credentialEntity = CredentialEntity.builder()
                .id(CREDENTIAL_ENTITY_PREFIX + "vc1")
                .type(CREDENTIAL_TYPE)
                .credentialTypeAttribute(CredentialTypeAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .value(List.of("VerifiableCredential","LEARCredentialEmployee"))
                        .build())
                .jsonCredentialAttribute(new CredentialAttribute(PROPERTY_TYPE,jsonNode))
                .credentialStatusAttribute(CredentialStatusAttribute.builder()
                        .type(PROPERTY_TYPE)
                        .credentialStatus(CredentialStatus.ISSUED).build())
                .relationshipAttribute(new RelationshipAttribute(RELATIONSHIP_TYPE, USER_ENTITY_PREFIX + userId))
                .build();

        when(objectMapper.readTree(jsonCredential)).thenReturn(jsonNode);

        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(credentialEntity)).thenReturn("CredentialEntity");

        StepVerifier.create(userDataServiceImpl.saveDOMEUnsignedCredential(userId, jsonCredential))
                .expectNext("CredentialEntity")
                .verifyComplete();

    }
}
