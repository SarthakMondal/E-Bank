package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.*;
import in.onlinebank.backend.exception.*;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.repository.AccountRepository;
import in.onlinebank.backend.repository.CardAndBankingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class NetBankingServiceImp implements NetBabkingService{

    @Autowired
    MailAndOtpService mailAndOtpService;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    CardAndBankingRepository cardAndBankingRepository;

    @Autowired
    MyUserDetailsService myUserDetailsService;

    @Override
    public ResponseEntity<String> transferMoney(DataModel dataModel) {
        ResponseEntity<String> response = null;

        try{
            String netBakingId = dataModel.getBankingId();
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByNetBankingId(netBakingId);
            String receiverAccountNo = dataModel.getAccountNo();
            String receiverIfscCode = dataModel.getReceiverIfscCode();
            String receiverBankName = dataModel.getReceiverBankName();

            if(cardAndBankingEntity.getId() == 0){
                throw new AccountNotFoundException("ID_NOT_ASSOCIATED");
            }else if(!cardAndBankingEntity.isAtmCardNotLocked()){
                throw new NetBankingException("NET_BANKING_DISABLED");
            } else if (!passwordEncoder.matches(dataModel.getPassword(), cardAndBankingEntity.getNetBankingPassword())) {
                cardAndBankingEntity.setNoOfAttemptsCard(cardAndBankingEntity.getNoOfAttemptsCard()+1);
                if(cardAndBankingEntity.getNoOfAttemptsCard() == 3){
                    cardAndBankingEntity.setAtmCardNotLocked(false);
                }
                cardAndBankingRepository.save(cardAndBankingEntity);
                throw new NetBankingException("PASSWORD_MISMATCH");

            } else if (!mailAndOtpService.validateOTP(cardAndBankingEntity.getBankAccount().getCustomer().getId(), dataModel.getOtpValue())) {
                throw new OtpMismatchException("OTP_MISMATCH");
            }else {
                cardAndBankingEntity.setAtmCardNotLocked(true);
                cardAndBankingRepository.save(cardAndBankingEntity);


                String senderAccountNo = cardAndBankingEntity.getBankAccount().getAccountNo();
                String senderName = cardAndBankingEntity.getBankAccount().getCustomer().getUserName();
                if(accountRepository.findByAccountNo(receiverAccountNo).getId()==0
                        || !accountRepository.findByAccountNo(receiverAccountNo).getCustomer().isActive()
                        || !accountRepository.findByAccountNo(receiverAccountNo).getCustomer().isVerified()){
                    throw new AccountNotFoundException("RECEIVER_NOT_FOUND");
                }
                String accountHolderName = accountRepository.findByAccountNo(receiverAccountNo).getCustomer().getUserName();

                long transferAmount = dataModel.getAmount();
                AccountEntity senderAccountEntity = accountRepository.findByAccountNo(senderAccountNo);
                AccountEntity receiverAccountEntity = accountRepository.findByAccountNo(receiverAccountNo);
                long senderPresentBalance = senderAccountEntity.getAmountDetails().getAmount();
                long receiverPresentBalance = receiverAccountEntity.getAmountDetails().getAmount();

                if(transferAmount > senderPresentBalance){
                    throw new InsufficientBalanceException("INSUFFICIENT_BALANCE");
                }
                senderPresentBalance-=transferAmount;
                receiverPresentBalance+=transferAmount;

                Date transactionDate = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
                formatter.setTimeZone(TimeZone.getTimeZone("Asia/kolkata"));
                formatter.format(transactionDate);

                TransactionEntity senderTransaction = new TransactionEntity();
                senderTransaction.setId(UUID.randomUUID());
                long senderPreviousAmount = senderPresentBalance+transferAmount;
                senderTransaction.setPreviousAmount(senderPreviousAmount);
                senderTransaction.setChangeAmount(transferAmount);
                senderTransaction.setCurrentAmount(senderPresentBalance);
                senderTransaction.setCreditOrDebit(CreditOrDebit.DEBIT);
                senderTransaction.setDatetime(transactionDate);
                senderTransaction.setStatus(TransactionStatus.SUCCESS);
                senderTransaction.setMethod("Account Transfer to: " + accountHolderName + " to Bank: " + receiverBankName + " Account No: " + receiverAccountNo + " IFSC code: " + receiverIfscCode);

                TransactionEntity receiverTransaction = new TransactionEntity();
                receiverTransaction.setId(UUID.randomUUID());
                long receiverPreviousAmount = receiverPresentBalance-transferAmount;
                receiverTransaction.setPreviousAmount(receiverPreviousAmount);
                receiverTransaction.setChangeAmount(transferAmount);
                receiverTransaction.setCurrentAmount(receiverPresentBalance);
                receiverTransaction.setCreditOrDebit(CreditOrDebit.CREDIT);
                receiverTransaction.setDatetime(transactionDate);
                receiverTransaction.setStatus(TransactionStatus.SUCCESS);
                receiverTransaction.setMethod("Account Transfer from " + senderName + " Account No: " + senderAccountNo + " Bank Name: " + BankDetails.BANKNAME + " IFSC code: " + BankDetails.IFSCCODE);

                List<TransactionEntity> senderTransactionList = senderAccountEntity.getAmountDetails().getTransactions();
                senderTransactionList.add(senderTransaction);
                List<TransactionEntity> receiverTransactionList = receiverAccountEntity.getAmountDetails().getTransactions();
                receiverTransactionList.add(receiverTransaction);

                AmountEntity senderAmount = senderAccountEntity.getAmountDetails();
                senderAmount.setAmount(senderPresentBalance);
                senderAmount.setTransactions(senderTransactionList);

                AmountEntity receiverAmount = receiverAccountEntity.getAmountDetails();
                receiverAmount.setAmount(receiverPresentBalance);
                receiverAmount.setTransactions(receiverTransactionList);

                senderAccountEntity.setAmountDetails(senderAmount);
                receiverAccountEntity.setAmountDetails(receiverAmount);

                accountRepository.save(senderAccountEntity);
                accountRepository.save(receiverAccountEntity);

                response = new ResponseEntity<>("AMOUNT_TRANSFERED", HttpStatus.OK);
            }


        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    @Override
    public ResponseEntity<String> blockNetBanking(DataModel dataModel) {
        ResponseEntity<String> response = null;
        try {
            long userId = myUserDetailsService.getLoggedInUserId();
            String accountNo = dataModel.getAccountNo();
            AccountEntity accountEntity = accountRepository.findByAccountNo(accountNo);

            if(accountEntity.getId()==0){
                throw new AccountNotFoundException("ACCOUNT_NOT_FOUND");
            }else if(accountEntity.getCustomer().getId()!=userId){
                throw new AccountNotFoundException("ACCOUNT_NOT_BELONGS_TO_YOU");
            }else{
                CardAndBankingEntity cardAndBankingEntity = accountEntity.getCardAndBanking();
                cardAndBankingEntity.setNetBankingNotLocked(false);

                cardAndBankingRepository.save(cardAndBankingEntity);
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }
}
