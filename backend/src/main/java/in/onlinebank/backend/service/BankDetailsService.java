package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.TransactionEntity;
import in.onlinebank.backend.model.DataModel;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface BankDetailsService {
    public ResponseEntity<DataModel> viewBankDetails();
    public ResponseEntity<DataModel> viewAccountDetails();
    public ResponseEntity<String> changeNetBankingPassword(DataModel dataModel);
    public ResponseEntity<List<TransactionEntity>> getMyTransactions(DataModel dataModel);
}
