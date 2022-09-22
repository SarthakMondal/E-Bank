package in.onlinebank.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="transaction_info")
@JsonIgnoreProperties({"hibernateLazyInitializer"})
@ToString
public class TransactionEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "previous_amount")
    private long previousAmount;

    @Column(name = "current_amount")
    private long currentAmount;

    @Column(name = "changed_amount")
    private long changeAmount;

    @Column(name = "transaction_method")
    private String method;

    @Column(name = "transaction_status")
    private TransactionStatus status;

    @Column(name = "credit_or_debit")
    private CreditOrDebit creditOrDebit;

    @Column(name = "transaction_date")
    private Date datetime;

    @Column(name = "atm_mechine_ip")
    private String systemIp;

}
