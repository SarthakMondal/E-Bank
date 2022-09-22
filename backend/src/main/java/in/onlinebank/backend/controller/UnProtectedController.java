package in.onlinebank.backend.controller;

import in.onlinebank.backend.entity.TransactionEntity;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.service.AtmBankingService;
import in.onlinebank.backend.service.MailAndOtpService;
import in.onlinebank.backend.service.NetBabkingService;
import in.onlinebank.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/onlinebaking/backend/public")
public class UnProtectedController {

    @Autowired
    UserService userService;

    @Autowired
    MailAndOtpService mailAndOtpService;

    @Autowired
    AtmBankingService atmBankingService;

    @Autowired
    NetBabkingService netBabkingService;

    @PostMapping(path = "/customer/signup")
    public ResponseEntity<String> registrationCustomer(@RequestBody DataModel dataModel){
        return this.userService.signupUser(dataModel);
    }

    @PostMapping(path = "/customer/generateotp/{userId}")
    public ResponseEntity<String> generateOtpForPasswordResetAndLogin(@PathVariable String userId){
        return this.mailAndOtpService.sendOTP(Long.parseLong(userId));
    }

    @PostMapping(path = "/customer/validateotp")
    public ResponseEntity<String> validateOtpForPasswordResetAndLogin(@RequestBody DataModel dataModel){
        return this.mailAndOtpService.validateOTP(dataModel);
    }

    @PatchMapping(path = "/customer/resetpassword")
    public ResponseEntity<String> generateOtp(@RequestBody DataModel dataModel){
        return this.userService.passwordReset(dataModel);
    }

    @PostMapping(path = "/customer/login")
    public ResponseEntity<String> loginCustomer(@RequestBody DataModel dataModel){
        return this.userService.loginUser(dataModel);
    }

    @PostMapping(path = "/customer/generateotpfromatm")
    public ResponseEntity<String> generateOtpFromAtm(@RequestBody DataModel dataModel){
        return this.mailAndOtpService.sendOTP(dataModel.getCardNo());
    }

    @PatchMapping(path = "/customer/changeatmpin")
    public ResponseEntity<String> changeAtmPinAndUnlock(@RequestBody DataModel dataModel){
        return this.atmBankingService.changePinOrUnlockCard(dataModel);
    }

    @PatchMapping(path = "/customer/depositmoney")
    public ResponseEntity<String> depositMoney(@RequestBody DataModel dataModel){
        return this.atmBankingService.depositAmount(dataModel);
    }

    @PatchMapping(path = "/customer/withdrawmoney")
    public ResponseEntity<String> withdrawMoney(@RequestBody DataModel dataModel){
        return this.atmBankingService.withdrawAmount(dataModel);
    }

    @PostMapping(path = "/customer/checkmoney")
    public ResponseEntity<String> checkMoney(@RequestBody DataModel dataModel){
        return this.atmBankingService.checkBalance(dataModel);
    }

    @PostMapping(path = "/customer/ministatement")
    public ResponseEntity<List<TransactionEntity>> miniStatement(@RequestBody DataModel dataModel){
        return this.atmBankingService.getMiniStatement(dataModel);
    }

    @PatchMapping(path = "/customer/transfermoney")
    public ResponseEntity<String> transferMoney(@RequestBody DataModel dataModel){
        return this.netBabkingService.transferMoney(dataModel);
    }

    @PatchMapping(path = "/customer/onlinepayment")
    public ResponseEntity<String> onlineTransferViaCard(@RequestBody DataModel dataModel){
        return this.atmBankingService.onlinePayment(dataModel);
    }

}
