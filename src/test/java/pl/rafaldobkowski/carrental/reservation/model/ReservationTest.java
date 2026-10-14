package pl.rafaldobkowski.carrental.reservation.model;

import org.junit.jupiter.api.Test;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationStatusException;
import pl.rafaldobkowski.carrental.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ReservationTest {

    @Test
    void shouldCancelPendingReservation() {
        Reservation reservation = createTestReservation();

        assertEquals(
                ReservationStatus.PENDING,
                reservation.getStatus()
        );

        reservation.cancel();

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledReservation() {
        Reservation reservation = createTestReservation();

        reservation.cancel();

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                reservation::cancel
        );

        assertEquals(
                "Reservation cannot be cancelled in its current status",
                exception.getMessage()
        );
    }

    private Reservation createTestReservation() {
        User user = mock(User.class);
        Car car = mock(Car.class);

        return new Reservation(
                user,
                car,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                new BigDecimal("399.98")
        );
    }
}