package in.onlinebank.backend.service;
import in.onlinebank.backend.entity.OtpEntity;
import in.onlinebank.backend.entity.UserEntity;
import in.onlinebank.backend.exception.UserNotFoundException;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.repository.CardAndBankingRepository;
import in.onlinebank.backend.repository.UserRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

@Service
public class MailAndOtpServiceImp implements MailAndOtpService {

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(MailAndOtpServiceImp.class);
    private static final String EMAIL = "techwizard0004@gmail.com";
    private static final String ERROR = "ERROR";


    @Autowired
    UserRepository userRepository;

    @Autowired
    CardAndBankingRepository cardAndBankingRepository;

    @Override
    public boolean sendEmail(DataModel dataModel) {
        try{
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL, "wxdgxozvheqjdqzk");
                }
            });

            String receiverAddress = dataModel.getMailAddress();
            String subject = dataModel.getMailHeader();
            String mailBody = dataModel.getMailBody();

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(EMAIL, false));

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL));
            msg.setSubject(subject);
            msg.setContent(mailBody, "text/plain");

            Transport.send(msg);
            String log = "actual mail address: " + receiverAddress + " for this project mails are sent from and sent to: " + EMAIL;
            LOGGER.info(log);
            return true;

        }catch (Exception e){
            LOGGER.warn(e.getMessage());
            return false;
        }
    }

    @Override
    public ResponseEntity<String> sendOTP(long userId) {

        ResponseEntity<String> response = null;
        try{
            Optional<UserEntity> optional = userRepository.findById(userId);
            UserEntity userEntity;

            if(optional.isEmpty()){
                throw new UserNotFoundException("User Not Found with given Id");
            }

            OtpEntity otpEntity = new OtpEntity();
            String otpValue = String.valueOf(Generator.generateRandomNumber(6));

            otpEntity.setOtpValue(otpValue);
            otpEntity.setGeneratedAt(new Date());

            userEntity = optional.get();
            userEntity.setUserOtp(otpEntity);
            userRepository.save(userEntity);

            DataModel dataModel = new DataModel();
            dataModel.setMailAddress(userEntity.getUserEmail());
            dataModel.setMailHeader("OTP from ONLINE BANK");
            dataModel.setMailBody("OTP: " + otpValue + ", valid for 3 Minutes");

            if(this.sendEmail(dataModel)){
                response = new ResponseEntity<>("OK", HttpStatus.OK);
            }else{
                response = new ResponseEntity<>(ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<String> sendOTP(String atmCardNo) {
        long userId = cardAndBankingRepository.findByCardNo(atmCardNo).getBankAccount().getCustomer().getId();
        return this.sendOTP(userId);
    }

    @Override
    public ResponseEntity<String> validateOTP(DataModel dataModel) {
        ResponseEntity<String> response = null;
        try{
            long userId = dataModel.getUserId();
            Optional<UserEntity> optional = userRepository.findById(userId);

            if(optional.isEmpty()){
                throw new UserNotFoundException("User Not Found with given Id");
            }

            OtpEntity otpEntity = optional.get().getUserOtp();

            long otpGeneratedAt = otpEntity.getGeneratedAt().getTime();
            long currentTime =new Date().getTime();
            long validityTime = 3 * 60 * 1000L;
            String generatedVal = otpEntity.getOtpValue();
            String requestedVal = dataModel.getOtpValue();

            if((currentTime <= otpGeneratedAt + validityTime) && (requestedVal.equals(generatedVal))){
                response = new ResponseEntity<>("OTP_MATCHED", HttpStatus.ACCEPTED);
            }
            else{
                response = new ResponseEntity<>("OTP_MISMATCH", HttpStatus.NOT_ACCEPTABLE);
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public boolean validateOTP(long userId, String otp) {
        DataModel dataModel = new DataModel();
        dataModel.setUserId(userId);
        dataModel.setOtpValue(otp);

        return this.validateOTP(dataModel).getStatusCode().value() == 202;
    }
}
