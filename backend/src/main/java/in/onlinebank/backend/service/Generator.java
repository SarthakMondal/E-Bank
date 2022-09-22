package in.onlinebank.backend.service;

import org.apache.log4j.Logger;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class Generator {

    private Generator() {

    }
    private static Random random;  // SecureRandom is preferred to Random
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(Generator.class);
    static {
        try {
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("No Such Algorithm");
        }
    }

    public static String generateRandomString(int n){
        String alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        StringBuilder builder = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index = random.nextInt(61);

            builder.append(alphaNumericString
                    .charAt(index));
        }

        return builder.toString();
    }

    public static int generateRandomNumber(int n){

        if(n==4){
            return random.nextInt(9999);
        } else if (n==6) {
            return random.nextInt(999999);
        } else{
            return random.nextInt(999);
        }
    }
}
