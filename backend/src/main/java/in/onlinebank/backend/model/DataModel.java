package in.onlinebank.backend.model;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataModel {
    private long userId;
    private String userName;
    private String userEmail;
    private String userMobile;
    private int userAge;
    private String userAddress;
    private String userGender;
    private String password;
    private String aadharNo;
    private String accountType;
    private String accountNo;
    private String panNo;
    private String idProof;
    private String photo;
    private String mailAddress;
    private String mailHeader;
    private String mailBody;
    private String otpValue;
    private long accountId;
    private long amount;
    private String cardNo;
    private String cardPin;
    private String cardCvv;
    private List<AccountDataModel> accountDataModels;
    private String bankDetails;
    private String bankingId;
    private String receiverBankName;
    private String receiverIfscCode;
}
