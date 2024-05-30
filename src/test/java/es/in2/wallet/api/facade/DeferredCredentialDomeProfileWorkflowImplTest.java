package es.in2.wallet.api.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.workflow.issuance.impl.DeferredCredentialDomeProfileWorkflowImpl;
import es.in2.wallet.domain.exception.CredentialNotAvailableException;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.EntityAttribute;
import es.in2.wallet.domain.model.TransactionDataAttribute;
import es.in2.wallet.domain.model.TransactionEntity;
import es.in2.wallet.domain.service.CredentialService;
import es.in2.wallet.domain.service.DataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static es.in2.wallet.domain.util.ApplicationConstants.JWT_VC;
import static es.in2.wallet.domain.util.ApplicationConstants.PROPERTY_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeferredCredentialDomeProfileWorkflowImplTest {

    @Mock
    private BrokerService brokerService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CredentialService credentialService;
    @Mock
    private DataService dataService;

    @InjectMocks
    private DeferredCredentialDomeProfileWorkflowImpl service;

    @Test
    void requestSignedLEARCredentialService_Success() throws JsonProcessingException {
        String processId = "processId";
        String userId = "userId";
        String credentialId = "credentialId";
        String credentialJson = "credential";
        String transactionJson = "transaction";
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .transactionDataAttribute(
                        EntityAttribute.<TransactionDataAttribute>builder()
                                .type(PROPERTY_TYPE)
                                .value(TransactionDataAttribute.builder()
                                        .transactionId("123")
                                        .accessToken("ey1234")
                                        .deferredEndpoint("https://example.com/deferred")
                                        .build()).build()
                ).build();

        CredentialResponse credentialResponse = CredentialResponse.builder().credential("credential").format(JWT_VC).build();
        List<TransactionEntity> transactions = List.of(transactionEntity);

        when(brokerService.getTransactionThatIsLinkedToACredential(processId, credentialId))
                .thenReturn(Mono.just(transactionJson));
        when(objectMapper.readValue(eq(transactionJson), any(TypeReference.class)))
                .thenReturn(transactions);
        when(credentialService.getCredentialDomeDeferredCase(
                transactionEntity.transactionDataAttribute().value().transactionId(),
                transactionEntity.transactionDataAttribute().value().accessToken(),
                transactionEntity.transactionDataAttribute().value().deferredEndpoint()
                ))
                .thenReturn(Mono.just(credentialResponse));

        when(brokerService.getCredentialByIdAndUserId(processId,credentialId, userId))
                .thenReturn(Mono.just(credentialJson));

        when(dataService.updateVCEntityWithSignedFormat(credentialJson,credentialResponse))
                .thenReturn(Mono.just("UpdatedCredentialEntity"));

        when(brokerService.updateEntity(processId, credentialId, "UpdatedCredentialEntity"))
                .thenReturn(Mono.empty());
        when(brokerService.deleteTransactionByTransactionId(processId,transactionEntity.id()))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.requestDeferredCredential(processId, userId, credentialId))
                .verifyComplete();
    }

    @Test
    void requestSignedLEARCredentialService_WithPendingTransaction() throws JsonProcessingException {
        String processId = "processId";
        String userId = "userId";
        String credentialId = "credentialId";
        String transactionJson = "transaction";
        String updatedTransactionJson = "updatedTransaction";

        TransactionEntity transactionEntity = TransactionEntity.builder()
                .id("trans123")
                .transactionDataAttribute(
                        EntityAttribute.<TransactionDataAttribute>builder()
                                .type(PROPERTY_TYPE)
                                .value(TransactionDataAttribute.builder()
                                        .transactionId("trans456")
                                        .accessToken("access789")
                                        .deferredEndpoint("https://example.com/callback")
                                        .build())
                                .build())
                .build();

        CredentialResponse credentialResponse = CredentialResponse.builder()
                .transactionId("newTransId")
                .build();

        List<TransactionEntity> transactions = List.of(transactionEntity);

        when(brokerService.getTransactionThatIsLinkedToACredential(processId, credentialId))
                .thenReturn(Mono.just(transactionJson));
        when(objectMapper.readValue(eq(transactionJson), any(TypeReference.class)))
                .thenReturn(transactions);
        ObjectWriter mockWriter = mock(ObjectWriter.class);
        when(objectMapper.writerWithDefaultPrettyPrinter()).thenReturn(mockWriter);
        when(mockWriter.writeValueAsString(transactionEntity)).thenReturn("transaction entity");
        when(credentialService.getCredentialDomeDeferredCase(
                transactionEntity.transactionDataAttribute().value().transactionId(),
                transactionEntity.transactionDataAttribute().value().accessToken(),
                transactionEntity.transactionDataAttribute().value().deferredEndpoint()
        ))
                .thenReturn(Mono.just(credentialResponse));

        when(dataService.updateTransactionWithNewTransactionId("transaction entity", "newTransId"))
                .thenReturn(Mono.just(updatedTransactionJson));
        when(brokerService.updateEntity(processId, transactionEntity.id(), updatedTransactionJson))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.requestDeferredCredential(processId, userId, credentialId))
                .expectError(CredentialNotAvailableException.class)
                .verify();
    }

    @Test
    void requestSignedLEARCredentialService_Failure_DueToErrorInDeserialization() throws JsonProcessingException {
        String processId = "processId";
        String userId = "userId";
        String credentialId = "credentialId";
        String transactionJson = "[{\"id\":\"trans123\", \"transactionDataAttribute\": {\"value\": {\"transactionId\": \"trans456\", \"accessToken\": \"token789\", \"deferredEndpoint\": \"https://callback.example.com\"}}}]";

        when(brokerService.getTransactionThatIsLinkedToACredential(processId, credentialId))
                .thenReturn(Mono.just(transactionJson));
        when(objectMapper.readValue(eq(transactionJson), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("Deserialization error") {});

        StepVerifier.create(service.requestDeferredCredential(processId, userId, credentialId))
                .expectError(FailedDeserializingException.class)
                .verify();
    }

}

