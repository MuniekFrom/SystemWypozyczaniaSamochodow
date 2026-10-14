package pl.rafaldobkowski.carrental.reservation.exception;

import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

public class InvalidReservationStatusException extends RuntimeException {

    public InvalidReservationStatusException(String message) {
        super(message);
    }

}