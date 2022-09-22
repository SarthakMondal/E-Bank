package in.onlinebank.backend.service;
import in.onlinebank.backend.model.DataModel;
import org.springframework.http.ResponseEntity;

public interface UserService {
    public ResponseEntity<String> signupUser(DataModel dataModel);
    public ResponseEntity<String> passwordReset(DataModel dataModel);
    public ResponseEntity<String> loginUser(DataModel dataModel);
    public ResponseEntity<DataModel> viewProfile();

}
