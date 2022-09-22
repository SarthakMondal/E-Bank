package in.onlinebank.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@ToString
@Table(name="card_and_banking_info")
@Component
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class CardAndBankingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "card_no")
    @NotNull(message = "This Field is Mandatory")
    @Size(min=16, max=16, message = "Debit Card No must be 16 digits")
    private String cardNo;

    @Column(name = "card_cvv")
    @Size(min=3, max=3, message = "CVV Bo must be 3 Digits")
    @NotNull(message = "This Field is Mandatory")
    private String cardCVV;

    @Column(name = "atm_pin")
    @Size(min=4, max=4, message = "Debit Card Pin must be 4 Digits")
    @NotNull(message = "This Field is Mandatory")
    private String atmPin;

    @Column(name = "atm_card_validity")
    @NotNull(message = "This Field is Mandatory")
    private Date atmCardValidity;

    @Column(name = "banking_id")
    @NotNull(message = "This Field is Mandatory")
    private String netBankingId;

    @Column(name = "banking_password")
    @NotNull(message = "This Field is Mandatory")
    private String netBankingPassword;

    @Column(name = "card_not_locked")
    @NotNull(message = "This Field is Mandatory")
    private boolean atmCardNotLocked = false;

    @Column(name = "card_attempts")
    @NotNull(message = "This Field is Mandatory")
    private int noOfAttemptsCard=0;

    @Column(name = "net_banking_attempts")
    @NotNull(message = "This Field is Mandatory")
    private int noOfAttemptsNetBanking=0;

    @Column(name = "bankingid_not_locked")
    @NotNull(message = "This Field is Mandatory")
    private boolean netBankingNotLocked = false;

    @OneToOne
    @JoinColumn(name = "account_id")
    @JsonBackReference(value = "banking-data")
    private AccountEntity bankAccount;

}
