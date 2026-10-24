package pl.rafaldobkowski.carrental.car.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import pl.rafaldobkowski.carrental.car.model.*;

import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.repository.ReservationRepository;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import jakarta.persistence.EntityManager;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class CarRepositoryIntegrationTest {

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

    private Car createTestCar() {
        return createTestCar(
                "BI12345",
                "JTDBR32E720123456"
        );
    }

    @Test
    void shouldSaveCarInDatabase() {
        Car car = createTestCar();

        Car savedCar = carRepository.saveAndFlush(car);

        assertNotNull(savedCar.getId());
        assertTrue(carRepository.existsByVin(car.getVin()));
        assertTrue(
                carRepository.existsByRegistrationNumber(
                        car.getRegistrationNumber()
                )
        );
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

        Car firstCar = createTestCar(duplicatedRegistrationNumber, "JTDBR32E720123456");
        Car secondCar = createTestCar(duplicatedRegistrationNumber, "JTDBR32E720123321");

        carRepository.saveAndFlush(firstCar);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> carRepository.saveAndFlush(secondCar)
        );
    }

    @Test
    void shouldReturnOnlyCarsAvailableInSelectedPeriod() {
        User user = new User(
                "Anna",
                "Nowak",
                "anna.availability@example.com",
                "test-password-hash",
                "+48123456789"
        );

        userRepository.saveAndFlush(user);

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

        Reservation reservation = new Reservation(
                user,
                reservedCar,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new BigDecimal("399.98")
        );

        reservationRepository.saveAndFlush(reservation);

        List<Car> availableCars =
                carRepository.findAvailableCars(
                        LocalDate.of(2026, 11, 11),
                        LocalDate.of(2026, 11, 14),
                        CarStatus.ACTIVE,
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
                );

        assertEquals(1, availableCars.size());

        assertEquals(
                availableCar.getId(),
                availableCars.getFirst().getId()
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

    @Test
    void shouldReturnCarWhenSearchStartsOnReservationEndDate() {
        User user = new User(
                "Anna",
                "Nowak",
                "anna.boundary@example.com",
                "test-password-hash",
                "+48987654321"
        );

        userRepository.saveAndFlush(user);

        Car car = createTestCar(
                "BI33333",
                "JTDBR32E720333333"
        );

        carRepository.saveAndFlush(car);

        Reservation reservation = new Reservation(
                user,
                car,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new BigDecimal("399.98")
        );

        reservationRepository.saveAndFlush(reservation);

        List<Car> availableCars =
                carRepository.findAvailableCars(
                        LocalDate.of(2026, 11, 12),
                        LocalDate.of(2026, 11, 14),
                        CarStatus.ACTIVE,
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
                );

        assertEquals(1, availableCars.size());

        assertEquals(
                car.getId(),
                availableCars.getFirst().getId()
        );
    }

    @Test
    void shouldNotReturnCarThatIsInService() {
        Car car = createTestCar(
                "BI44444",
                "JTDBR32E720444444"
        );

        car.changeStatus(CarStatus.IN_SERVICE);

        carRepository.saveAndFlush(car);

        List<Car> availableCars =
                carRepository.findAvailableCars(
                        LocalDate.of(2026, 11, 20),
                        LocalDate.of(2026, 11, 22),
                        CarStatus.ACTIVE,
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
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

}