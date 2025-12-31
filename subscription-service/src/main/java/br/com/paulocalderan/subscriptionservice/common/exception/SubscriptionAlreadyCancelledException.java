package br.com.paulocalderan.subscriptionservice.common.exception;

public class SubscriptionAlreadyCancelledException extends RuntimeException {
    public SubscriptionAlreadyCancelledException(String message) {
        super(message);
    }

    public SubscriptionAlreadyCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}

