package in.onlinebank.backend.service;

import org.springframework.http.ResponseEntity;

public interface Generation {
    public ResponseEntity<String> generateId();
    public ResponseEntity<String> generateAccountDetails();
    public ResponseEntity<String> generateAtmDetails();
    public ResponseEntity<String> generateNetBankingDetails();
}
