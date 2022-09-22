package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.*;
import in.onlinebank.backend.exception.AccountNotFoundException;
import in.onlinebank.backend.exception.OtpMismatchException;
import in.onlinebank.backend.exception.UserNotFoundException;
import in.onlinebank.backend.model.AccountDataModel;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.repository.AccountRepository;
import in.onlinebank.backend.repository.UserRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BankDetailsServiceImp implements BankDetailsService{
    @Autowired
    UserRepository userRepository;

    @Autowired
    MyUserDetailsService myUserDetailsService;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Autowired
    MailAndOtpService mailAndOtpService;

    @Autowired
    AccountRepository accountRepository;

    private static final String USERNOTFOUND = "User Not Found with given Id";

    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(BankDetailsServiceImp.class);

    @Override
    public ResponseEntity<DataModel> viewBankDetails() {
        ResponseEntity<DataModel> response = null;
        try{

            DataModel dataModel = new DataModel();
            dataModel.setBankDetails(BankDetails.BANKNAME + " " + BankDetails.BRANCHNAME + " " + BankDetails.IFSCCODE + " " + BankDetails.BRANCHCODE);
            response = new ResponseEntity<>(dataModel, HttpStatus.OK);
        }
        catch (Exception e){
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(new DataModel(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<DataModel> viewAccountDetails() {
        ResponseEntity<DataModel> response = null;
        try{
            long userId = myUserDetailsService.getLoggedInUserId();
            Optional<UserEntity> optional = userRepository.findById(userId);

            if(optional.isEmpty()){
                throw new UserNotFoundException(USERNOTFOUND);
            }

            UserEntity userEntity = optional.get();
            DataModel dataModel = new DataModel();

            List<AccountEntity> entityList = userEntity.getBackAccounts();
            List<AccountDataModel> dataList = new ArrayList<>();

            entityList.forEach(accountEntity -> {
                AccountDataModel model = new AccountDataModel();
                model.setAccountNo(accountEntity.getAccountNo());
                model.setCardValidity(accountEntity.getCardAndBanking().getAtmCardValidity());
                String card = accountEntity.getCardAndBanking().getCardNo();
                model.setCardNo(card.substring(0, 4) + "-" + card.substring(4, 8) + "-" + card.substring(8, 12) + "-" + card.substring(12, 16));
                model.setNetBankingId(accountEntity.getCardAndBanking().getNetBankingId());
                model.setAccountType(accountEntity.getAccountType());
                model.setAccountBalance(accountEntity.getAmountDetails().getAmount());
                dataList.add(model);
            });

            dataModel.setAccountDataModels(dataList);

            response = new ResponseEntity<>(dataModel, HttpStatus.OK);
        }
        catch (Exception e){
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(new DataModel(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<String> changeNetBankingPassword(DataModel dataModel) {
        ResponseEntity<String> response = null;
        try {
            long userId = myUserDetailsService.getLoggedInUserId();
            Optional<UserEntity> optional = userRepository.findById(userId);

            if (optional.isEmpty()) {
                throw new UserNotFoundException(USERNOTFOUND);
            }

            String netBankingPassword = passwordEncoder.encode(dataModel.getPassword());
            Optional<AccountEntity> optional1 = accountRepository.findById(dataModel.getAccountId());
            if (optional1.isEmpty()) {
                throw new AccountNotFoundException("Account Not Found with given Id");
            }

            Set<Long> accountIds = optional.get().getBackAccounts().stream().map(AccountEntity::getId).collect(Collectors.toSet());
            if(!accountIds.contains(dataModel.getAccountId())){
                throw new AccountNotFoundException("Account Does not Belong to You");
            }
            AccountEntity accountEntity = optional1.get();
            CardAndBankingEntity bankingEntity = accountEntity.getCardAndBanking();

            if(!mailAndOtpService.validateOTP(accountEntity.getCustomer().getId(), dataModel.getOtpValue())){
                throw new OtpMismatchException("OTP_MISMATCH");
            }

            bankingEntity.setNetBankingPassword(netBankingPassword);
            accountEntity.setCardAndBanking(bankingEntity);

            accountRepository.save(accountEntity);
            response = new ResponseEntity<>("OK", HttpStatus.OK);
        }
        catch (Exception e){
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<List<TransactionEntity>> getMyTransactions(DataModel dataModel) {
        ResponseEntity<List<TransactionEntity>> response = null;
        try {
            long userId = myUserDetailsService.getLoggedInUserId();
            Optional<UserEntity> optional = userRepository.findById(userId);

            if (optional.isEmpty()) {
                throw new UserNotFoundException(USERNOTFOUND);
            }
            Optional<AccountEntity> optional1 = accountRepository.findById(dataModel.getAccountId());
            if (optional1.isEmpty()) {
                throw new AccountNotFoundException("Account Not Found with given Id");
            }

            Set<Long> accountIds = optional.get().getBackAccounts().stream().map(AccountEntity::getId).collect(Collectors.toSet());
            if(!accountIds.contains(dataModel.getAccountId())){
                throw new AccountNotFoundException("Account Does not Belong to You");
            }

            List<TransactionEntity> entityList = optional1.get().getAmountDetails().getTransactions()
                    .stream().sorted(Comparator.comparing(TransactionEntity::getDatetime).reversed()).collect(Collectors.toList());
            response = new ResponseEntity<>(entityList, HttpStatus.OK);

        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
            response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }
}
