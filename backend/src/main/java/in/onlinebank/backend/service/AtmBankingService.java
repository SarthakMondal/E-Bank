package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.TransactionEntity;
import in.onlinebank.backend.model.DataModel;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AtmBankingService {
    public ResponseEntity<String> checkBalance(DataModel dataModel);
    public ResponseEntity<String> changePinOrUnlockCard(DataModel dataModel);
    public ResponseEntity<String> depositAmount(DataModel dataModel);
    public ResponseEntity<String> withdrawAmount(DataModel dataModel);
    public ResponseEntity<String> lockAtmCard(DataModel dataModel);
    public ResponseEntity<String> onlinePayment(DataModel dataModel);

    public ResponseEntity<List<TransactionEntity>> getMiniStatement(DataModel dataModel);
}
