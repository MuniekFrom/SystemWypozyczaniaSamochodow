package pl.rafaldobkowski.carrental.car.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.car.model.CarBodyType;
import pl.rafaldobkowski.carrental.car.model.CarCategory;
import pl.rafaldobkowski.carrental.car.model.CarDriveType;
import pl.rafaldobkowski.carrental.car.model.CarFuelType;
import pl.rafaldobkowski.carrental.car.model.CarStatus;
import pl.rafaldobkowski.carrental.car.model.CarTransmissionType;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.repository.ReservationRepository;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class CarRepositoryIntegrationTest {

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
    private CarRepository carRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveCarInDatabase() {
        Car car = createTestCar();

        Car savedCar = carRepository.saveAndFlush(car);

        assertNotNull(savedCar.getId());
        assertTrue(carRepository.existsByVin(car.getVin()));
        assertTrue(carRepository.existsByRegistrationNumber(
                car.getRegistrationNumber()
        ));
    }

    @Test
    void shouldRejectDuplicateVin() {
        String duplicatedVin = "JTDBR32E720123456";

        Car firstCar = createTestCar("BI12345", duplicatedVin);
        Car secondCar = createTestCar("BI54321", duplicatedVin);

        carRepository.saveAndFlush(firstCar);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> carRepository.saveAndFlush(secondCar)
        );
    }

    @Test
    void shouldRejectDuplicateRegistrationNumber() {
        String duplicatedRegistrationNumber = "BI12345";

        Car firstCar = createTestCar(
                duplicatedRegistrationNumber,
                "JTDBR32E720123456"
        );
        Car secondCar = createTestCar(
                duplicatedRegistrationNumber,
                "JTDBR32E720123321"
        );

        carRepository.saveAndFlush(firstCar);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> carRepository.saveAndFlush(secondCar)
        );
    }

    @Test
    void shouldReturnOnlyCarsAvailableInSelectedPeriod() {
        User user = createAndSaveUser(
                "anna.availability@example.com",
                "+48123456789"
        );

        Car availableCar = createTestCar(
                "BI11111",
                "JTDBR32E720111111"
        );
        Car reservedCar = createTestCar(
                "BI22222",
                "JTDBR32E720222222"
        );

        carRepository.saveAllAndFlush(
                List.of(availableCar, reservedCar)
        );

        reservationRepository.saveAndFlush(new Reservation(
                user,
                reservedCar,
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("599.97")
        ));

        List<Car> availableCars = carRepository.findAvailableCars(
                LocalDateTime.of(2026, 11, 11, 6, 0),
                LocalDateTime.of(2026, 11, 14, 14, 0),
                CarStatus.ACTIVE,
                BLOCKING_STATUSES
        );

        assertEquals(1, availableCars.size());
        assertEquals(
                availableCar.getId(),
                availableCars.getFirst().getId()
        );
    }

    @Test
    void shouldNotReturnCarBeforeFourHourTurnaroundHasPassed() {
        User user = createAndSaveUser(
                "anna.buffer@example.com",
                "+48987654321"
        );
        Car car = createTestCar(
                "BI33333",
                "JTDBR32E720333333"
        );
        carRepository.saveAndFlush(car);

        reservationRepository.saveAndFlush(new Reservation(
                user,
                car,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 3, 13, 0),
                new BigDecimal("599.97")
        ));

        LocalDateTime requestedStart =
                LocalDateTime.of(2026, 12, 3, 16, 30);
        LocalDateTime requestedEnd =
                LocalDateTime.of(2026, 12, 4, 12, 0);

        List<Car> availableCars = carRepository.findAvailableCars(
                requestedStart.minusHours(4),
                requestedEnd.plusHours(4),
                CarStatus.ACTIVE,
                BLOCKING_STATUSES
        );

        assertTrue(availableCars.isEmpty());
    }

    @Test
    void shouldReturnCarExactlyFourHoursAfterReturn() {
        User user = createAndSaveUser(
                "anna.boundary@example.com",
                "+48777777777"
        );
        Car car = createTestCar(
                "BI66666",
                "JTDBR32E720666666"
        );
        carRepository.saveAndFlush(car);

        reservationRepository.saveAndFlush(new Reservation(
                user,
                car,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 3, 13, 0),
                new BigDecimal("599.97")
        ));

        LocalDateTime requestedStart =
                LocalDateTime.of(2026, 12, 3, 17, 0);
        LocalDateTime requestedEnd =
                LocalDateTime.of(2026, 12, 4, 12, 0);

        List<Car> availableCars = carRepository.findAvailableCars(
                requestedStart.minusHours(4),
                requestedEnd.plusHours(4),
                CarStatus.ACTIVE,
                BLOCKING_STATUSES
        );

        assertEquals(1, availableCars.size());
        assertEquals(car.getId(), availableCars.getFirst().getId());
    }

    @Test
    void shouldNotReturnCarThatIsInService() {
        Car car = createTestCar(
                "BI44444",
                "JTDBR32E720444444"
        );
        car.changeStatus(CarStatus.IN_SERVICE);
        carRepository.saveAndFlush(car);

        List<Car> availableCars = carRepository.findAvailableCars(
                LocalDateTime.of(2026, 11, 20, 6, 0),
                LocalDateTime.of(2026, 11, 22, 14, 0),
                CarStatus.ACTIVE,
                BLOCKING_STATUSES
        );

        assertTrue(availableCars.isEmpty());
    }

    @Test
    void shouldSaveAndReadCarTechnicalData() {
        Car car = createTestCar(
                "BI55555",
                "JTDBR32E720555555"
        );

        car.updateTechnicalData(
                1798,
                140,
                CarDriveType.FWD,
                new BigDecimal("9.20")
        );

        Long carId = carRepository
                .saveAndFlush(car)
                .getId();

        entityManager.clear();

        Car savedCar = carRepository
                .findById(carId)
                .orElseThrow();

        assertEquals(1798, savedCar.getEngineCapacityCm3());
        assertEquals(140, savedCar.getPowerHp());
        assertEquals(CarDriveType.FWD, savedCar.getDriveType());
        assertEquals(
                new BigDecimal("9.20"),
                savedCar.getAccelerationTo100Seconds()
        );
    }

    private User createAndSaveUser(String email, String phoneNumber) {
        return userRepository.saveAndFlush(new User(
                "Anna",
                "Nowak",
                email,
                "test-password-hash",
                phoneNumber
        ));
    }

    private Car createTestCar() {
        return createTestCar(
                "BI12345",
                "JTDBR32E720123456"
        );
    }

    private Car createTestCar(
            String registrationNumber,
            String vin) {

        return new Car(
                "Toyota",
                "Corolla",
                2024,
                registrationNumber,
                vin,
                CarFuelType.GASOLINE,
                CarTransmissionType.AUTOMATIC,
                CarBodyType.SEDAN,
                5,
                CarCategory.STANDARD,
                "Gray",
                new BigDecimal("199.99"),
                "Samochód testowy"
        );
    }
}
