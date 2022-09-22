package in.onlinebank.backend.exception;

public class InsufficientBalanceException extends Exception{
    public InsufficientBalanceException(String s){
        super(s);
    }
}
