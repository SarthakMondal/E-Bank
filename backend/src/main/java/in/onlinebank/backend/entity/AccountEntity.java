package in.onlinebank.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="bankdetails_info")
@Component
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account_no", nullable = false)
    @NotNull(message = "This Field is Mandatory")
    private String accountNo="";

    @Column(name = "account_type")
    @NotNull(message = "This Field is Mandatory")
    private AccountType accountType = AccountType.CURRENT;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonBackReference(value = "customer-accounts")
    private UserEntity customer;

    @OneToOne(fetch = FetchType.LAZY, cascade=CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "banking_details")
    @JsonManagedReference(value = "banking-data")
    private CardAndBankingEntity cardAndBanking;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_cash_and_transactions")
    private AmountEntity amountDetails;

}
