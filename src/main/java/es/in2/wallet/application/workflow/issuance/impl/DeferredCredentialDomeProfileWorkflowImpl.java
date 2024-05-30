package es.in2.wallet.application.workflow.issuance.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.application.workflow.issuance.DeferredCredentialDomeProfileWorkflow;
import es.in2.wallet.domain.exception.CredentialNotAvailableException;
import es.in2.wallet.domain.exception.FailedDeserializingException;
import es.in2.wallet.domain.model.TransactionEntity;
import es.in2.wallet.domain.service.CredentialService;
import es.in2.wallet.domain.service.DataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeferredCredentialDomeProfileWorkflowImpl implements DeferredCredentialDomeProfileWorkflow {
    private final BrokerService brokerService;
    private final ObjectMapper objectMapper;
    private final CredentialService credentialService;
    private final DataService dataService;
    @Override
    public Mono<Void> requestDeferredCredential(String processId, String userId, String credentialId) {
        return brokerService.getTransactionThatIsLinkedToACredential(processId,credentialId)
                .flatMap(transactionEntity -> {
                            try {
                                //Although the response is a list of transactions, we obtain the first position since we know that a credential
                                // cannot be linked to more than one transaction. Therefore, we know that even though a list is returned,
                                // it will always contain one element.
                                List<TransactionEntity> transactions = objectMapper.readValue(transactionEntity, new TypeReference<>() {});
                                TransactionEntity transaction = transactions.get(0);
                                return credentialService.getCredentialDomeDeferredCase(
                                        transaction.transactionDataAttribute().value()
                                                .transactionId(),
                                        transaction.transactionDataAttribute().value()
                                                .accessToken(),
                                        transaction.transactionDataAttribute().value()
                                                .deferredEndpoint())
                                        .flatMap(credentialResponse -> {

                                            if (credentialResponse.transactionId() == null){
                                                return brokerService.getCredentialByIdAndUserId(processId,credentialId,userId)
                                                        .flatMap(credentialEntity -> dataService.updateVCEntityWithSignedFormat(credentialEntity,credentialResponse))
                                                        .flatMap(updatedEntity -> brokerService.updateEntity(processId,credentialId,updatedEntity))
                                                        .then(brokerService.deleteTransactionByTransactionId(processId,transaction.id()));
                                            }
                                            else {
                                                try {
                                                    return dataService.updateTransactionWithNewTransactionId(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transaction),credentialResponse.transactionId())
                                                            .flatMap(updatedEntity -> brokerService.updateEntity(processId,transaction.id(),updatedEntity))
                                                            .then(Mono.error(new CredentialNotAvailableException("The signed credential it's not available yet")));
                                                }
                                                catch (JsonProcessingException e) {
                                                    return Mono.error(new RuntimeException("Error processing : " + transactionEntity));
                                                }
                                            }
                                        });
                            }
                            catch (Exception e) {
                                log.error("Error while processing Transaction", e);
                                return Mono.error(new FailedDeserializingException("Error processing Transaction: " + transactionEntity));
                            }
                });
    }
}
