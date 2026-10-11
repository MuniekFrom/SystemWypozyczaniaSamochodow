package pl.rafaldobkowski.carrental.reservation.exception;

public class InvalidReservationPeriodException
        extends RuntimeException {

    public InvalidReservationPeriodException(String message) {
        super(message);
    }
}