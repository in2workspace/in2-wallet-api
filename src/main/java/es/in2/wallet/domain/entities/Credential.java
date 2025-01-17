package es.in2.wallet.domain.entities;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("wallet.credential")
public class Credential {
    @Id
    @Column("id")
    private UUID id;

    @Column("credential_id")
    private UUID credentialId;

    @Column("user_id")
    private UUID userId;

    @Column("credential_type")
    private List<String> credentialType;

    @Column("credential_status")
    private Integer credentialStatus;

    @Column("credential_format")
    private Integer credentialFormat;

    @Column("credential_data")
    private String credentialData;

    @Column("json_vc")
    private String jsonVc;

    @Column("created_at")
    private Timestamp createdAt;

    @Column("updated_at")
    private Timestamp updatedAt;
}