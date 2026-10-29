package pl.rafaldobkowski.carrental.reservation.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.car.model.CarBodyType;
import pl.rafaldobkowski.carrental.car.model.CarCategory;
import pl.rafaldobkowski.carrental.car.model.CarFuelType;
import pl.rafaldobkowski.carrental.car.model.CarTransmissionType;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ReservationRepositoryIntegrationTest {

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.IN_PROGRESS
            );

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
        Reservation reservation = createReservation(
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0)
        );

        Reservation savedReservation =
                reservationRepository.saveAndFlush(reservation);

        assertNotNull(savedReservation.getId());

        LocalDateTime requestedStart =
                LocalDateTime.of(2026, 11, 11, 10, 0);
        LocalDateTime requestedEnd =
                LocalDateTime.of(2026, 11, 14, 10, 0);

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        requestedStart.minusHours(4),
                        requestedEnd.plusHours(4),
                        BLOCKING_STATUSES
                );

        assertTrue(overlap);
    }

    @Test
    void shouldDetectOverlapDuringFourHourTurnaroundBuffer() {
        reservationRepository.saveAndFlush(createReservation(
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 3, 13, 0)
        ));

        LocalDateTime requestedStart =
                LocalDateTime.of(2026, 12, 3, 16, 30);
        LocalDateTime requestedEnd =
                LocalDateTime.of(2026, 12, 4, 10, 0);

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        requestedStart.minusHours(4),
                        requestedEnd.plusHours(4),
                        BLOCKING_STATUSES
                );

        assertTrue(overlap);
    }

    @Test
    void shouldAllowReservationStartingExactlyAfterFourHourBuffer() {
        reservationRepository.saveAndFlush(createReservation(
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 3, 13, 0)
        ));

        LocalDateTime requestedStart =
                LocalDateTime.of(2026, 12, 3, 17, 0);
        LocalDateTime requestedEnd =
                LocalDateTime.of(2026, 12, 4, 10, 0);

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        requestedStart.minusHours(4),
                        requestedEnd.plusHours(4),
                        BLOCKING_STATUSES
                );

        assertFalse(overlap);
    }

    @Test
    void shouldDetectOverlapForInProgressReservation() {
        Reservation reservation = createReservation(
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0)
        );

        reservation.confirm();
        reservation.startRental();

        reservationRepository.saveAndFlush(reservation);

        LocalDateTime requestedStart =
                LocalDateTime.of(2026, 11, 11, 10, 0);
        LocalDateTime requestedEnd =
                LocalDateTime.of(2026, 11, 14, 10, 0);

        boolean overlap =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        requestedStart.minusHours(4),
                        requestedEnd.plusHours(4),
                        BLOCKING_STATUSES
                );

        assertTrue(overlap);
    }

    private Reservation createReservation(
            LocalDateTime startAt,
            LocalDateTime endAt) {

        return new Reservation(
                user,
                car,
                startAt,
                endAt,
                new BigDecimal("399.98")
        );
    }
}
