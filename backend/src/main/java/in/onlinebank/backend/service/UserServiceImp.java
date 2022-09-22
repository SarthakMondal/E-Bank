package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.*;
import in.onlinebank.backend.exception.UserNotFoundException;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.repository.UserRepository;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class UserServiceImp implements UserService{

    @Autowired
    UserRepository userRepository;

    @Autowired
    MyUserDetailsService myUserDetailsService;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Autowired
    MailAndOtpService mailAndOtpService;

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(UserServiceImp.class);
    public static final String OK = "COMPLETE";
    public static final String ERROR = "ERROR";


    @Override
    public ResponseEntity<String> signupUser(DataModel dataModel) {
        ResponseEntity<String> response = null;
        try {
            UserEntity userEntity = new UserEntity();
            AccountEntity accountEntity = new AccountEntity();
            DocumentEntity documentEntity = new DocumentEntity();
            CardAndBankingEntity cardAndBankingEntity = new CardAndBankingEntity();

            if (dataModel.getAccountType().equals("S")) {
                accountEntity.setAccountType(AccountType.SAVING);
            } else if(dataModel.getAccountType().equals("M")){
                accountEntity.setAccountType(AccountType.SALARY);
            } else{
                accountEntity.setAccountType(AccountType.CURRENT);
            }

            Date date = new Date();
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTime(date);

            calendar.add(Calendar.YEAR, 4);
            date = calendar.getTime();

            List<String> bankDetails = this.generateBankDetails(dataModel.getUserEmail());
            cardAndBankingEntity.setNetBankingId(bankDetails.get(1));
            cardAndBankingEntity.setNetBankingPassword(passwordEncoder.encode(bankDetails.get(2)));
            cardAndBankingEntity.setCardNo(bankDetails.get(3));
            cardAndBankingEntity.setAtmPin(bankDetails.get(4));
            cardAndBankingEntity.setCardCVV(bankDetails.get(5));
            cardAndBankingEntity.setAtmCardValidity(date);

            accountEntity.setAccountNo(bankDetails.get(0));
            accountEntity.setCardAndBanking(cardAndBankingEntity);
            accountEntity.setAmountDetails(new AmountEntity());

            List<AccountEntity> list = new ArrayList<>();
            list.add(accountEntity);

            documentEntity.setAadharNo(dataModel.getAadharNo());
            documentEntity.setPanNo(dataModel.getPanNo());
            documentEntity.setIdentityProof(Base64.getDecoder().decode(dataModel.getIdProof()));
            documentEntity.setPhoto(Base64.getDecoder().decode(dataModel.getPhoto()));

            userEntity.setUserName(dataModel.getUserName());
            userEntity.setUserEmail(dataModel.getUserEmail());
            userEntity.setUserAge(dataModel.getUserAge());
            if(dataModel.getUserGender().equals("M")){
                userEntity.setUserGender(Gender.MALE);
            } else if(dataModel.getUserGender().equals("F")){
                userEntity.setUserGender(Gender.FEMALE);
            } else{
                userEntity.setUserGender(Gender.OTHER);
            }
            userEntity.setUserMobile(dataModel.getUserMobile());
            userEntity.setUserAddress(dataModel.getUserAddress());
            userEntity.setPassword(passwordEncoder.encode(Generator.generateRandomString(8)));
            userEntity.setBackAccounts(list);
            userEntity.setDocumentDetails(documentEntity);

            documentEntity.setDocumentForCustomer(userEntity);
            accountEntity.setCustomer(userEntity);
            cardAndBankingEntity.setBankAccount(accountEntity);

            userRepository.save(userEntity);

            String mailBody = "Hello " + dataModel.getUserName() + ",\n"
                    + "Greetings from " + BankDetails.BANKNAME + "\n"
                    + "Please find your account details and follow the instructions below for uninterrupted Banking" + "\n"
                    + "user-id: " + userRepository.findByUserEmail(userEntity.getUserEmail()).getId() + "\n"
                    + "account-no: " + bankDetails.get(0) + "\n"
                    + "netbanking-id: " + bankDetails.get(1) + "\n"
                    + "atmcard-no: " + bankDetails.get(3) + "\n"
                    + "atmcard-valid-till: " + date.toString() + "\n"
                    + "Please Note: To login in the Banking website use your user-id and the website password. "
                    + "To use net-banking, account transfering use netbanking-id and net-banking password. To use atm service use atm-card and pin. "
                    + "To use atm card for online payment use card number and card cvv. Please reset password if you are loggin first time in"
                    + " the website and reset atm-pin via atm is also mandatory" + "\n"
                    + "To enter your account, net-banking, atm-transactions, online-shopping everytime you will receive an otp in your"
                    + " registered Email. Use it for 2-factor-authentication" + "\n"
                    + "******* DO NOT SHARE ATM-PIN, CARD-CVV, OTP WITH ANYONE AT ANY COST ******"+ "\n"
                    + "Thank you for Banking with Us." + "\n"
                    + "Regards, " + "\n"
                    + "Online Bank";
            String mailHeader = "Banking Instructions & Account Details";
            String receiver = dataModel.getUserEmail();

            DataModel dataModel1 = new DataModel();
            dataModel1.setMailHeader(mailHeader);
            dataModel1.setMailBody(mailBody);
            dataModel1.setMailAddress(receiver);

            mailAndOtpService.sendEmail(dataModel1);

            response = new ResponseEntity<>(OK, HttpStatus.OK);

        }
        catch (Exception e){
            userRepository.sequenceRollBack();
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<String> passwordReset(DataModel dataModel) {
        ResponseEntity<String> response = null;
        try{
            long userId = dataModel.getUserId();
            Optional<UserEntity> optional = userRepository.findById(userId);

            if(optional.isEmpty()){
                throw new UserNotFoundException("User Not Found with given Id");
            }

            UserEntity userEntity = optional.get();
            userEntity.setPassword(passwordEncoder.encode(dataModel.getPassword()));

            userRepository.save(userEntity);
            response = new ResponseEntity<>(OK, HttpStatus.OK);
        }
        catch (Exception e){
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<String> loginUser(DataModel dataModel) {
        ResponseEntity<String> response = null;

        String username = String.valueOf(dataModel.getUserId());
        String password = dataModel.getPassword();

        RestTemplate rest = new RestTemplate();
        String url = "http://localhost:8082/oauth/token";
        String authHeader = "Basic " + "T25saW5lQmFua2luZ19DbGllbnRJZDokMmEkMTAkcEhid2JjT2FFZVpaTDdIaGFHOWtPTy9tNzlrNlJWWGk0QTVPa2wxcTFBeEN6YWxnYkx1bG0=";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", password);

        HttpHeaders header = new HttpHeaders();
        header.setAccept(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        header.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        header.set("Authorization", authHeader);

        HttpEntity<MultiValueMap<String, String>> data = new HttpEntity<>(formData, header);

        try {
            ResponseEntity<String> loginInfo = rest.exchange(url, HttpMethod.POST, data, String.class);
            JSONObject temp = new JSONObject(loginInfo.getBody());

            response = new ResponseEntity<>(String.valueOf(temp.get("access_token")), HttpStatus.ACCEPTED);
        }

        catch (Exception e) {
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        }
        return response;
    }

    @Override
    public ResponseEntity<DataModel> viewProfile() {
        ResponseEntity<DataModel> response = null;
        try{
            long userId = myUserDetailsService.getLoggedInUserId();
            Optional<UserEntity> optional = userRepository.findById(userId);

            if(optional.isEmpty()){
                throw new UserNotFoundException("User Not Found with given Id");
            }

            UserEntity userEntity = optional.get();
            DataModel dataModel = new DataModel();
            dataModel.setUserId(userEntity.getId());
            dataModel.setUserName(userEntity.getUserName());
            dataModel.setUserAge(userEntity.getUserAge());
            dataModel.setUserEmail(userEntity.getUserEmail());
            dataModel.setUserAddress(userEntity.getUserAddress());

            String aadhar = userEntity.getDocumentDetails().getAadharNo().substring(0, 4)
                    + "*****" + userEntity.getDocumentDetails().getAadharNo().substring(8, userEntity.getDocumentDetails().getAadharNo().length()-1);


            String pan = userEntity.getDocumentDetails().getPanNo().substring(0, 4) + "******";

            dataModel.setAadharNo(aadhar);
            dataModel.setPanNo(pan);
            dataModel.setPhoto(Base64.getEncoder().encodeToString(userEntity.getDocumentDetails().getPhoto()));

            response = new ResponseEntity<>(dataModel, HttpStatus.OK);
        }
        catch (Exception e){
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(new DataModel(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }



    private List<String> generateBankDetails(String mailId){
        List<String> list = new ArrayList<>();
        String accountNo = "";
        String cardNo = "";
        String netBankingId = "";
        String netBankingPassword = "";
        String atmPin="";
        String cvv="";

        SimpleDateFormat dateFromat = new SimpleDateFormat("ddMMyyHHmmss");
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        dateFromat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        accountNo = BankDetails.BRANCHCODE + dateFromat.format(timestamp);
        netBankingId = mailId + "." + BankDetails.BRANCHCODE + "@oknetbanking";
        netBankingPassword = Generator.generateRandomString(8);
        cardNo = dateFromat.format(timestamp) + String.valueOf((Generator.generateRandomNumber(4)));
        atmPin = String.valueOf((Generator.generateRandomNumber(4)));
        cvv = String.valueOf((Generator.generateRandomNumber(3)));

        list.add(accountNo);
        list.add(netBankingId);
        list.add(netBankingPassword);
        list.add(cardNo);
        list.add(atmPin);
        list.add(cvv);

        return list;
    }
}
