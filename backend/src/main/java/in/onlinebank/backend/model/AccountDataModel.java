package in.onlinebank.backend.model;

import in.onlinebank.backend.entity.AccountType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AccountDataModel {
    private String accountNo;
    private String cardNo;
    private String cardType = "VISA";
    private Date cardValidity;
    private AccountType accountType;
    private String netBankingId;
    private long accountBalance;
}
