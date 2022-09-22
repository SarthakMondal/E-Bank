package in.onlinebank.backend.controller;
import in.onlinebank.backend.entity.TransactionEntity;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.service.AtmBankingService;
import in.onlinebank.backend.service.BankDetailsService;
import in.onlinebank.backend.service.NetBabkingService;
import in.onlinebank.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/onlinebaking/backend/protected/")
public class ProtectedController {
    @Autowired
    UserService userService;

    @Autowired
    BankDetailsService bankDetailsService;

    @Autowired
    AtmBankingService atmBankingService;

    @Autowired
    NetBabkingService netBabkingService;

    @GetMapping(path = "/customer/profile")
    public ResponseEntity<DataModel> userProfile(){
        return this.userService.viewProfile();
    }

    @GetMapping(path = "/customer/bankdetails")
    public ResponseEntity<DataModel> viewBankDetails(){
        return this.bankDetailsService.viewBankDetails();
    }

    @GetMapping(path = "/customer/accountdetails")
    public ResponseEntity<DataModel> viewAccountDetails(){
        return this.bankDetailsService.viewAccountDetails();
    }

    @PatchMapping(path = "/customer/changebankingpassword")
    public ResponseEntity<String> changeBankingPassword(@RequestBody DataModel dataModel){
        return this.bankDetailsService.changeNetBankingPassword(dataModel);
    }

    @GetMapping(path = "/customer/transactions")
    public ResponseEntity<List<TransactionEntity>> viewAccountTransactions(@RequestBody DataModel dataModel){
        return this.bankDetailsService.getMyTransactions(dataModel);
    }

    @PatchMapping(path = "/customer/lockatmcard")
    public ResponseEntity<String> lockAtmCard(@RequestBody DataModel dataModel){
        return this.atmBankingService.lockAtmCard(dataModel);
    }

    @PatchMapping(path = "/customer/locknetbanking")
    public ResponseEntity<String> lockNetBanking(@RequestBody DataModel dataModel){
        return this.netBabkingService.blockNetBanking(dataModel);
    }

}
