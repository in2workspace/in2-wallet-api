package es.in2.wallet.domain.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("wallet.deferred_credential_metadata")
public class DeferredCredentialMetadata {
    @Id
    @Column("id")
    private UUID id;

    @Column("transaction_id")
    private UUID transactionId;

    @Column("credential_id")
    private UUID credentialId;

    @Column("access_token")
    private String accessToken;

    @Column("deferred_endpoint")
    private String deferredEndpoint;

    @Column("created_at")
    private Timestamp createdAt;

    @Column("updated_at")
    private Timestamp updatedAt;
}
