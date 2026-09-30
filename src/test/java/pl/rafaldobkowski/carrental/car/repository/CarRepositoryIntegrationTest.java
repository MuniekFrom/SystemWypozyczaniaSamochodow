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

import java.math.BigDecimal;

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

    // TODO: sprawdzić nazwę konkretnego ograniczenia bazy danych

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
}