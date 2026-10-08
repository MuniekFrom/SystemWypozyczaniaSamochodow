package pl.rafaldobkowski.carrental.auth.exception;

public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException(String message) {
        super(message);
    }
}