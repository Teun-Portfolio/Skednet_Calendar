package utils;

import java.util.Arrays;

public class Logger {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";

    /**
     * prints info message
     */
    public static void i(String function, String message) {
        System.out.printf("I - %s - %s\n", function, message);
    }

    /**
     * prints warning message
     */
    public static void w(String function, String message) {
        System.out.printf("%sW - %s - %s%s\n", YELLOW, function, message, RESET);
    }

    /**
     * prints error message
     */
    public static void e(String function, String message) {
        System.out.printf("%sE - %s - %s%s\n", RED, function, message, RESET);
    }

    public static void e(String function, Exception e) {
        e(function, e.getMessage());
    }

    public static void e(String function, String message, Exception e) {
        e(function, String.format("%sE - %s - %s\n%s%s\n", RED, function, message, Arrays.toString(e.getStackTrace()), RESET));
    }
}
