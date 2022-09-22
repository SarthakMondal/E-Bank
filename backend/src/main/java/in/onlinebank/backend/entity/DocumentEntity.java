package in.onlinebank.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="document_info")
@Component
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "aadhar_number")
    @NotNull(message = "This Field is Mandatory")
    @Size(min=12, max=12, message = "Aadhar Number must be 12 Digits")
    private String aadharNo;

    @Column(name = "pan_number")
    @NotNull(message = "This Field is Mandatory")
    @Size(min=10, max=10, message = "Pan Number must be 10 Digits")
    private String panNo;

    @Lob
    @NotNull(message = "This Field is Mandatory")
    @Column(name = "identity")
    private byte[] identityProof;

    @Lob
    @NotNull(message = "This Field is Mandatory")
    @Column(name = "photo")
    private byte[] photo;

    @ManyToOne
    @JoinColumn(name = "document_for_customer")
    @JsonBackReference(value = "customer-document")
    private UserEntity documentForCustomer;
}
