package in.onlinebank.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="user_info")
@Component
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_id_generator")
    @SequenceGenerator(name = "customer_id_generator", sequenceName = "customer_id", initialValue = 1000000, allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_name")
    @NotNull(message = "This Field is Mandatory")
    private String userName;

    @Column(name = "user_email")
    @NotNull(message = "This Field is Mandatory")
    @Email(message = "Email-Id is not in Proper Format")
    private String userEmail;

    @Column(name = "user_mobile")
    @NotNull(message = "This Field is Mandatory")
    @Size(max = 10, min=10)
    private String userMobile;

    @Column(name = "user_age")
    private int userAge;

    @Column(name = "user_address")
    @NotNull(message = "This Field is Mandatory")
    private String userAddress;

    @Column(name = "user_gender")
    @NotNull(message = "This Field is Mandatory")
    private Gender userGender;

    @Column(name = "user_password")
    @NotNull(message = "This Field is Mandatory")
    private String password;

    @Column(name = "user_active")
    @NotNull(message = "This Field is Mandatory")
    private boolean active = false;

    @Column(name = "user_verified")
    @NotNull(message = "This Field is Mandatory")
    private boolean verified = false;

    @Column(name = "user_role")
    @NotNull(message = "This Field is Mandatory")
    private String role = "ROLE_CUSTOMER";

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "customer_documents")
    @JsonManagedReference(value = "customer-document")
    private DocumentEntity documentDetails;

    @Column(name = "user_accounts")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "customer-accounts")
    private List<AccountEntity> backAccounts;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_otp")
    private OtpEntity userOtp;

}
