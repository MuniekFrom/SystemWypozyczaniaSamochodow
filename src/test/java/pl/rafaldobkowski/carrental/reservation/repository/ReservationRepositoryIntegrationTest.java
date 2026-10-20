package pl.rafaldobkowski.carrental.reservation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import pl.rafaldobkowski.carrental.car.model.*;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ReservationRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    private User user;

    private Car car;

    @BeforeEach
    void setUp() {
        user = new User(
                "Anna",
                "Nowak",
                "anna.reservation@example.com",
                "test-password-hash",
                "+48123456789"
        );

        car = new Car(
                "Toyota",
                "Corolla",
                2024,
                "BI12345",
                "JTDBR32E720123456",
                CarFuelType.GASOLINE,
                CarTransmissionType.AUTOMATIC,
                CarBodyType.SEDAN,
                5,
                CarCategory.STANDARD,
                "Gray",
                new BigDecimal("199.99"),
                "Samochód testowy"
        );

        userRepository.saveAndFlush(user);
        carRepository.saveAndFlush(car);
    }

    @Test
    void shouldDetectOverlappingReservation() {

        Reservation reservation = new Reservation(
                user,
                car,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new BigDecimal("399.98")
        );

        Reservation savedReservation =
                reservationRepository.saveAndFlush(reservation);

        assertNotNull(savedReservation.getId());

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 14),
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
                );

        assertTrue(overlap);
    }


    @Test
    void shouldNotDetectOverlapWhenNewReservationStartsOnEndDate() {
        Reservation reservation = new Reservation(
                user,
                car,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new BigDecimal("399.98")
        );

        reservationRepository.saveAndFlush(reservation);

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        LocalDate.of(2026, 11, 12),
                        LocalDate.of(2026, 11, 14),
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
                );

        assertFalse(overlap);
    }

    @Test
    void shouldDetectOverlapForInProgressReservation() {
        Reservation reservation = new Reservation(
                user,
                car,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new BigDecimal("399.98")
        );

        reservation.confirm();
        reservation.startRental();

        reservationRepository.saveAndFlush(reservation);

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 14),
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
                );

        assertTrue(overlap);
    }
}