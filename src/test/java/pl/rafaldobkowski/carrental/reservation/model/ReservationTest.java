package pl.rafaldobkowski.carrental.reservation.model;

import org.junit.jupiter.api.Test;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationStatusException;
import pl.rafaldobkowski.carrental.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Test
    void shouldConfirmPendingReservation() {
        Reservation reservation = createTestReservation();

        reservation.confirm();

        assertEquals(
                ReservationStatus.CONFIRMED,
                reservation.getStatus()
        );
    }

    @Test
    void shouldRejectConfirmingCancelledReservation() {
        Reservation reservation = createTestReservation();

        reservation.cancel();

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                reservation::confirm
        );

        assertEquals(
                "Only pending reservations can be confirmed",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );
    }

    @Test
    void shouldStartConfirmedReservation() {
        Reservation reservation = createTestReservation();

        reservation.confirm();
        reservation.startRental();

        assertEquals(
                ReservationStatus.IN_PROGRESS,
                reservation.getStatus()
        );
    }

    @Test
    void shouldRejectStartingPendingReservation() {
        Reservation reservation = createTestReservation();

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                reservation::startRental
        );

        assertEquals(
                "Only confirmed reservations can be started",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.PENDING,
                reservation.getStatus()
        );
    }

    @Test
    void shouldRejectCompletingConfirmedReservation() {
        Reservation reservation = createTestReservation();

        reservation.confirm();

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                reservation::complete
        );

        assertEquals(
                "Only in-progress reservations can be completed",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.CONFIRMED,
                reservation.getStatus()
        );
    }

    @Test
    void shouldCompleteInProgressReservation() {
        Reservation reservation = createTestReservation();

        reservation.confirm();
        reservation.startRental();
        reservation.complete();

        assertEquals(
                ReservationStatus.COMPLETED,
                reservation.getStatus()
        );
    }

    @Test
    void shouldRejectCompletingPendingReservation() {
        Reservation reservation = createTestReservation();

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                reservation::complete
        );

        assertEquals(
                "Only in-progress reservations can be completed",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.PENDING,
                reservation.getStatus()
        );
    }

    private Reservation createTestReservation() {
        User user = mock(User.class);
        Car car = mock(Car.class);

        LocalDateTime startAt = LocalDateTime.now()
                .plusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        return new Reservation(
                user,
                car,
                startAt,
                startAt.plusDays(2),
                new BigDecimal("399.98")
        );
    }
}
