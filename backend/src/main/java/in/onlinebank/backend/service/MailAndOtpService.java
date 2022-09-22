package in.onlinebank.backend.service;

import in.onlinebank.backend.model.DataModel;
import org.springframework.http.ResponseEntity;

public interface MailAndOtpService {
    public boolean sendEmail(DataModel dataModel);
    public ResponseEntity<String> sendOTP(long userId);
    public ResponseEntity<String> sendOTP(String atmCardNo);
    public ResponseEntity<String> validateOTP(DataModel dataModel);
    public boolean validateOTP(long userId, String otp);
}
