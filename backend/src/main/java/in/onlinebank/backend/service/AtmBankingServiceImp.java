package in.onlinebank.backend.service;

import in.onlinebank.backend.entity.*;
import in.onlinebank.backend.exception.AccountNotFoundException;
import in.onlinebank.backend.exception.InsufficientBalanceException;
import in.onlinebank.backend.exception.NetBankingException;
import in.onlinebank.backend.model.DataModel;
import in.onlinebank.backend.repository.AccountRepository;
import in.onlinebank.backend.repository.CardAndBankingRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AtmBankingServiceImp implements AtmBankingService{

    @Autowired
    CardAndBankingRepository cardAndBankingRepository;

    @Autowired
    MailAndOtpService mailAndOtpService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    MyUserDetailsService myUserDetailsService;

    private static final String EXPIRED = "CARD_VALIDITY_EXPIRED";
    private static final String NOTALLOWED = "NOT_ALLOWED";
    private static final String LOCKED = "CARD_LOCKED";
    private static final String FORMAT = "dd-MM-yyyy HH:mm:ss";
    private static final String MISMATCH = "OTP_MISMATCH";
    private static final String TIMEZONE = "Asia/kolkata";
    private static final String FORMAT2 = "E, dd MMM yyyy HH:mm:ss z";

    private static final String MESSAGE = "Notification From ONLINE BANK";

    @Override
    public ResponseEntity<String> changePinOrUnlockCard(DataModel dataModel) {
        ResponseEntity<String> response = null;

        try{
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByCardNo(dataModel.getCardNo());
            String accountNoEntered = dataModel.getAccountNo();
            String accountNoActual = cardAndBankingEntity.getBankAccount().getAccountNo();

            long userId = cardAndBankingEntity.getBankAccount().getCustomer().getId();
            String otpValue = dataModel.getOtpValue();

            if(accountNoActual.equals(accountNoEntered) && mailAndOtpService.validateOTP(userId, otpValue)){
                cardAndBankingEntity.setAtmPin(dataModel.getCardPin());
                cardAndBankingEntity.setAtmCardNotLocked(true);

                cardAndBankingRepository.save(cardAndBankingEntity);

                DataModel dataModel1 = new DataModel();
                dataModel1.setMailAddress(cardAndBankingEntity.getBankAccount().getCustomer().getUserEmail());
                dataModel1.setMailBody("Pin Changed, Card Status: UN-LOCKED");
                dataModel1.setMailHeader(MESSAGE);

                mailAndOtpService.sendEmail(dataModel);
                response = new ResponseEntity<>("STATUS_MODIFIED", HttpStatus.ACCEPTED);
            }else{
                response = new ResponseEntity<>(NOTALLOWED, HttpStatus.NOT_ACCEPTABLE);
            }

        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> checkBalance(DataModel dataModel) {
        ResponseEntity<String> response = null;

        try {
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByCardNo(dataModel.getCardNo());
            AccountEntity accountEntity = cardAndBankingEntity.getBankAccount();
            Date today = new Date();
            Date validity = cardAndBankingEntity.getAtmCardValidity();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
            simpleDateFormat.format(today);
            simpleDateFormat.format(validity);

            if(!dataModel.getCardPin().equals(cardAndBankingEntity.getAtmPin())){
                response = new ResponseEntity<>(NOTALLOWED, HttpStatus.NOT_ACCEPTABLE);
                cardAndBankingEntity.setNoOfAttemptsCard(cardAndBankingEntity.getNoOfAttemptsCard()+1);
                cardAndBankingRepository.save(cardAndBankingEntity);

                if(cardAndBankingEntity.getNoOfAttemptsCard() ==3 ){
                    cardAndBankingEntity.setAtmCardNotLocked(false);
                    accountEntity.setCardAndBanking(cardAndBankingEntity);
                    accountRepository.save(accountEntity);
                }
            } else if (!cardAndBankingEntity.isAtmCardNotLocked()) {
                response = new ResponseEntity<>(LOCKED, HttpStatus.NOT_ACCEPTABLE);
            } else if (validity.before(today)) {
                response = new ResponseEntity<>(EXPIRED, HttpStatus.NOT_ACCEPTABLE);
            } else if(!mailAndOtpService.validateOTP(accountEntity.getCustomer().getId(), dataModel.getOtpValue())){
                response = new ResponseEntity<>(MISMATCH, HttpStatus.NOT_ACCEPTABLE);
            }
            else {
                AmountEntity amountEntity = accountEntity.getAmountDetails();
                String amount = String.valueOf(amountEntity.getAmount());
                response = new ResponseEntity<>(amount, HttpStatus.OK);

                cardAndBankingEntity.setNoOfAttemptsCard(0);
                accountEntity.setCardAndBanking(cardAndBankingEntity);
                accountRepository.save(accountEntity);
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> depositAmount(DataModel dataModel) {
        ResponseEntity<String> response = null;

        try{
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByCardNo(dataModel.getCardNo());
            AccountEntity accountEntity = cardAndBankingEntity.getBankAccount();
            Date today = new Date();
            Date validity = cardAndBankingEntity.getAtmCardValidity();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
            simpleDateFormat.format(today);
            simpleDateFormat.format(validity);

            if(!dataModel.getCardPin().equals(cardAndBankingEntity.getAtmPin())){
                response = new ResponseEntity<>(NOTALLOWED, HttpStatus.NOT_ACCEPTABLE);
                cardAndBankingEntity.setNoOfAttemptsCard(cardAndBankingEntity.getNoOfAttemptsCard()+1);

                if(cardAndBankingEntity.getNoOfAttemptsCard() ==3 ){
                    cardAndBankingEntity.setAtmCardNotLocked(false);
                    accountEntity.setCardAndBanking(cardAndBankingEntity);
                    accountRepository.save(accountEntity);
                }
                cardAndBankingRepository.save(cardAndBankingEntity);
            } else if (!cardAndBankingEntity.isAtmCardNotLocked()) {
                response = new ResponseEntity<>(LOCKED, HttpStatus.NOT_ACCEPTABLE);
            } else if (validity.before(today)) {
                response = new ResponseEntity<>(EXPIRED, HttpStatus.NOT_ACCEPTABLE);
            } else if(!mailAndOtpService.validateOTP(accountEntity.getCustomer().getId(), dataModel.getOtpValue())){
                response = new ResponseEntity<>(MISMATCH, HttpStatus.NOT_ACCEPTABLE);
            }
            else{
                AmountEntity amountEntity = accountEntity.getAmountDetails();
                List<TransactionEntity> transactionList = amountEntity.getTransactions();

                long depositdOnTheDay = transactionList.stream().filter(tEntity ->
                        DateUtils.isSameDay(new Date(), tEntity.getDatetime()) && tEntity.getCreditOrDebit() ==
                                CreditOrDebit.CREDIT).mapToLong(TransactionEntity::getChangeAmount).sum();

                if(depositdOnTheDay + dataModel.getAmount() > 30000){
                    response = new ResponseEntity<>("DEPOSIT_LIMIT_EXCEEDED", HttpStatus.NOT_ACCEPTABLE);
                }
                else{
                    long amount = dataModel.getAmount();
                    long prevAmount = accountEntity.getAmountDetails().getAmount();
                    long newAmount = dataModel.getAmount() + accountEntity.getAmountDetails().getAmount();

                    Date transactionDate = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat(FORMAT2);
                    formatter.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
                    formatter.format(transactionDate);
                    UUID uuid = UUID.randomUUID();
                    InetAddress ip = InetAddress.getLocalHost();

                    TransactionEntity transaction = new TransactionEntity();
                    transaction.setId(uuid);
                    transaction.setPreviousAmount(prevAmount);
                    transaction.setChangeAmount(amount);
                    transaction.setCurrentAmount(newAmount);
                    transaction.setCreditOrDebit(CreditOrDebit.CREDIT);
                    transaction.setDatetime(transactionDate);
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transaction.setMethod("Deposited From Atm of Branch WORLD");
                    transaction.setSystemIp(ip.toString());

                    transactionList.add(transaction);
                    amountEntity.setAmount(newAmount);
                    amountEntity.setTransactions(transactionList);

                    accountEntity.setAmountDetails(amountEntity);
                    cardAndBankingEntity.setNoOfAttemptsCard(0);
                    accountEntity.setCardAndBanking(cardAndBankingEntity);

                    accountRepository.save(accountEntity);

                    DataModel dataModel1 = new DataModel();
                    dataModel1.setMailAddress(cardAndBankingEntity.getBankAccount().getCustomer().getUserEmail());
                    dataModel1.setMailBody("Amount Credited through ATM deposit Balance: " + amount + " Account Balance: " + newAmount);
                    dataModel1.setMailHeader(MESSAGE);

                    mailAndOtpService.sendEmail(dataModel1);
                    response = new ResponseEntity<>("AMOUNT_DEPOSITED", HttpStatus.OK);

                }
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> withdrawAmount(DataModel dataModel) {
        ResponseEntity<String> response = null;

        try{
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByCardNo(dataModel.getCardNo());
            AccountEntity accountEntity = cardAndBankingEntity.getBankAccount();
            Date today = new Date();
            Date validity = cardAndBankingEntity.getAtmCardValidity();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
            simpleDateFormat.format(today);
            simpleDateFormat.format(validity);

            if(!dataModel.getCardPin().equals(cardAndBankingEntity.getAtmPin())){
                response = new ResponseEntity<>(NOTALLOWED, HttpStatus.NOT_ACCEPTABLE);
                cardAndBankingEntity.setNoOfAttemptsCard(cardAndBankingEntity.getNoOfAttemptsCard()+1);
                cardAndBankingRepository.save(cardAndBankingEntity);

                if(cardAndBankingEntity.getNoOfAttemptsCard() ==3 ){
                    cardAndBankingEntity.setAtmCardNotLocked(false);
                    accountEntity.setCardAndBanking(cardAndBankingEntity);
                    accountRepository.save(accountEntity);
                }
            } else if (!cardAndBankingEntity.isAtmCardNotLocked()) {
                response = new ResponseEntity<>(LOCKED, HttpStatus.NOT_ACCEPTABLE);
            }else if (validity.before(today)) {
                response = new ResponseEntity<>(EXPIRED, HttpStatus.NOT_ACCEPTABLE);
            }else if(!mailAndOtpService.validateOTP(accountEntity.getCustomer().getId(), dataModel.getOtpValue())){
                response = new ResponseEntity<>(MISMATCH, HttpStatus.NOT_ACCEPTABLE);
            }
            else{
                AmountEntity amountEntity = accountEntity.getAmountDetails();
                List<TransactionEntity> transactionList = amountEntity.getTransactions();

                long withdrawnAmountOnTheDay = transactionList.stream().filter(tEntity ->
                        DateUtils.isSameDay(new Date(), tEntity.getDatetime()) && tEntity.getCreditOrDebit() ==
                                CreditOrDebit.DEBIT).mapToLong(TransactionEntity::getChangeAmount).sum();

                long amount = dataModel.getAmount();
                long prevAmount = accountEntity.getAmountDetails().getAmount();
                long newAmount =  accountEntity.getAmountDetails().getAmount()-dataModel.getAmount();

                if(withdrawnAmountOnTheDay + amount > 20000){
                    response = new ResponseEntity<>("WITHDRAW_LIMIT_EXCEEDED", HttpStatus.NOT_ACCEPTABLE);
                }
                else if(amountEntity.getAmount() < amount){
                    response = new ResponseEntity<>("INSUFFICIENT_BALANCE", HttpStatus.NOT_ACCEPTABLE);
                }
                else{

                    Date transactionDate = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat(FORMAT2);
                    formatter.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
                    formatter.format(transactionDate);
                    UUID uuid = UUID.randomUUID();
                    InetAddress ip = InetAddress.getLocalHost();

                    TransactionEntity transaction = new TransactionEntity();
                    transaction.setId(uuid);
                    transaction.setPreviousAmount(prevAmount);
                    transaction.setChangeAmount(amount);
                    transaction.setCurrentAmount(newAmount);
                    transaction.setCreditOrDebit(CreditOrDebit.DEBIT);
                    transaction.setDatetime(transactionDate);
                    transaction.setStatus(TransactionStatus.SUCCESS);
                    transaction.setMethod("Withdrawn from Online Bank");
                    transaction.setSystemIp(ip.toString());

                    transactionList.add(transaction);
                    amountEntity.setAmount(newAmount);
                    amountEntity.setTransactions(transactionList);

                    accountEntity.setAmountDetails(amountEntity);
                    cardAndBankingEntity.setNoOfAttemptsCard(0);
                    accountEntity.setCardAndBanking(cardAndBankingEntity);
                    accountRepository.save(accountEntity);
                    DataModel dataModel1 = new DataModel();
                    dataModel1.setMailAddress(cardAndBankingEntity.getBankAccount().getCustomer().getUserEmail());
                    dataModel1.setMailBody("Amount Debited through ATM Withdraw Balance: " + amount + " Account Balance: " + newAmount);
                    dataModel1.setMailHeader(MESSAGE);

                    mailAndOtpService.sendEmail(dataModel1);
                    response = new ResponseEntity<>("AMOUNT_WITHDRAWN", HttpStatus.OK);

                }
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> lockAtmCard(DataModel dataModel) {
        ResponseEntity<String> response = null;
        try{
            long userId = myUserDetailsService.getLoggedInUserId();
            String accountNo = dataModel.getAccountNo();
            AccountEntity accountEntity = accountRepository.findByAccountNo(accountNo);

            if(accountEntity.getId()==0){
                throw new AccountNotFoundException("ACCOUNT_NOT_FOUND");
            }else if(accountEntity.getCustomer().getId()!=userId){
                throw new AccountNotFoundException("ACCOUNT_NOT_BELONGS_TO_YOU");
            }else{
                CardAndBankingEntity cardAndBankingEntity = accountEntity.getCardAndBanking();
                cardAndBankingEntity.setAtmCardNotLocked(false);

                cardAndBankingRepository.save(cardAndBankingEntity);
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ResponseEntity<String> onlinePayment(DataModel dataModel) {
        ResponseEntity<String> response = null;

        try{
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByCardNo(dataModel.getCardNo());
            AccountEntity accountEntitySender = cardAndBankingEntity.getBankAccount();
            Date today = new Date();
            Date validity = cardAndBankingEntity.getAtmCardValidity();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
            simpleDateFormat.format(today);
            simpleDateFormat.format(validity);

            if(!dataModel.getCardCvv().equals(cardAndBankingEntity.getCardCVV())){
                response = new ResponseEntity<>(NOTALLOWED, HttpStatus.NOT_ACCEPTABLE);
                cardAndBankingEntity.setNoOfAttemptsCard(cardAndBankingEntity.getNoOfAttemptsCard()+1);
                cardAndBankingRepository.save(cardAndBankingEntity);

                if(cardAndBankingEntity.getNoOfAttemptsCard() ==3 ){
                    cardAndBankingEntity.setAtmCardNotLocked(false);
                    accountEntitySender.setCardAndBanking(cardAndBankingEntity);
                    accountRepository.save(accountEntitySender);
                }
            } else if (!cardAndBankingEntity.isAtmCardNotLocked()) {
                response = new ResponseEntity<>(LOCKED, HttpStatus.NOT_ACCEPTABLE);
            }else if (validity.before(today)) {
                response = new ResponseEntity<>(EXPIRED, HttpStatus.NOT_ACCEPTABLE);
            }else if(!mailAndOtpService.validateOTP(accountEntitySender.getCustomer().getId(), dataModel.getOtpValue())){
                response = new ResponseEntity<>(MISMATCH, HttpStatus.NOT_ACCEPTABLE);
            }
            else{
                String receiverAccountNo = dataModel.getAccountNo();
                AccountEntity accountEntityReceiver = accountRepository.findByAccountNo(receiverAccountNo);

                if(accountEntityReceiver.getId() ==0){
                    throw new AccountNotFoundException("ACCOUNT_NOT_FOUND");
                }else{
                    long senderPreviousAmount = accountEntitySender.getAmountDetails().getAmount();
                    long receiverPreviousAmount = accountEntityReceiver.getAmountDetails().getAmount();
                    long debitAmount = dataModel.getAmount();

                    if(debitAmount > senderPreviousAmount){
                        throw new InsufficientBalanceException("INSUFFICIENT_BALANCE");
                    } else if (debitAmount > 15000) {
                        throw new NetBankingException("LIMIT_EXCEEDED");
                    }else{
                        long senderPresentAmount = senderPreviousAmount-debitAmount;
                        long receiverPresentAmount = receiverPreviousAmount+debitAmount;

                        Date transactionDate = new Date();
                        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT2);
                        formatter.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
                        formatter.format(transactionDate);

                        TransactionEntity senderTransaction = new TransactionEntity();
                        senderTransaction.setId(UUID.randomUUID());
                        senderTransaction.setPreviousAmount(senderPreviousAmount);
                        senderTransaction.setChangeAmount(debitAmount);
                        senderTransaction.setCurrentAmount(senderPresentAmount);
                        senderTransaction.setCreditOrDebit(CreditOrDebit.DEBIT);
                        senderTransaction.setDatetime(transactionDate);
                        senderTransaction.setStatus(TransactionStatus.SUCCESS);
                        senderTransaction.setMethod("Account Transfer to: " + accountEntityReceiver.getCustomer().getUserName() + " through Online Card Payment");

                        TransactionEntity receiverTransaction = new TransactionEntity();
                        receiverTransaction.setId(UUID.randomUUID());
                        receiverTransaction.setPreviousAmount(receiverPreviousAmount);
                        receiverTransaction.setChangeAmount(debitAmount);
                        receiverTransaction.setCurrentAmount(receiverPresentAmount);
                        receiverTransaction.setCreditOrDebit(CreditOrDebit.CREDIT);
                        receiverTransaction.setDatetime(transactionDate);
                        receiverTransaction.setStatus(TransactionStatus.SUCCESS);
                        receiverTransaction.setMethod("Account Transfer from " + accountEntitySender.getCustomer().getUserName() + "Through Online Card Payment" +  "Bank Name: " + BankDetails.BANKNAME + " IFSC code: " + BankDetails.IFSCCODE);

                        List<TransactionEntity> senderTransactionList = accountEntitySender.getAmountDetails().getTransactions();
                        senderTransactionList.add(senderTransaction);
                        List<TransactionEntity> receiverTransactionList = accountEntityReceiver.getAmountDetails().getTransactions();
                        receiverTransactionList.add(receiverTransaction);

                        AmountEntity senderAmount = accountEntitySender.getAmountDetails();
                        senderAmount.setAmount(senderPresentAmount);
                        senderAmount.setTransactions(senderTransactionList);

                        AmountEntity receiverAmount = accountEntityReceiver.getAmountDetails();
                        receiverAmount.setAmount(receiverPresentAmount);
                        receiverAmount.setTransactions(receiverTransactionList);

                        accountRepository.save(accountEntitySender);
                        accountRepository.save(accountEntityReceiver);

                        response = new ResponseEntity<>("TRANSACTION_COMPLETE", HttpStatus.OK);
                    }
                }
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    @Override
    public ResponseEntity<List<TransactionEntity>> getMiniStatement(DataModel dataModel) {
        ResponseEntity<List<TransactionEntity>> response = null;

        try{
            CardAndBankingEntity cardAndBankingEntity = cardAndBankingRepository.findByCardNo(dataModel.getCardNo());
            AccountEntity accountEntity = cardAndBankingEntity.getBankAccount();
            Date today = new Date();
            Date validity = cardAndBankingEntity.getAtmCardValidity();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT);
            simpleDateFormat.format(today);
            simpleDateFormat.format(validity);

            if(!dataModel.getCardPin().equals(cardAndBankingEntity.getAtmPin())){
                response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_ACCEPTABLE);
                cardAndBankingEntity.setNoOfAttemptsCard(cardAndBankingEntity.getNoOfAttemptsCard()+1);
                cardAndBankingRepository.save(cardAndBankingEntity);

                if(cardAndBankingEntity.getNoOfAttemptsCard() ==3 ){
                    cardAndBankingEntity.setAtmCardNotLocked(false);
                    accountEntity.setCardAndBanking(cardAndBankingEntity);
                    accountRepository.save(accountEntity);
                }
            } else if (!cardAndBankingEntity.isAtmCardNotLocked()) {
                response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_ACCEPTABLE);
            }else if (validity.before(today)) {
                response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_ACCEPTABLE);
            }else if(!mailAndOtpService.validateOTP(accountEntity.getCustomer().getId(), dataModel.getOtpValue())){
                response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_ACCEPTABLE);
            }
            else{
                AmountEntity amountEntity = accountEntity.getAmountDetails();
                List<TransactionEntity> transactionList = amountEntity.getTransactions().stream()
                        .sorted(Comparator.comparing(TransactionEntity::getDatetime).reversed())
                        .limit(10).collect(Collectors.toList());


                response = new ResponseEntity<>(transactionList, HttpStatus.OK);
            }
        }
        catch (Exception e){
            response = new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }
}
