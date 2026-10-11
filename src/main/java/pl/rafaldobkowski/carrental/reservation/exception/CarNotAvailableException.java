package pl.rafaldobkowski.carrental.reservation.exception;

public class CarNotAvailableException extends RuntimeException {

    public CarNotAvailableException(String message) {
        super(message);
    }
}