package in.onlinebank.backend.service;

import in.onlinebank.backend.model.DataModel;
import org.springframework.http.ResponseEntity;

public interface NetBabkingService {
    public ResponseEntity<String> transferMoney(DataModel dataModel);
    public ResponseEntity<String> blockNetBanking(DataModel dataModel);
}
