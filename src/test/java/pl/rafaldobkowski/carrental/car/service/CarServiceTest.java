package pl.rafaldobkowski.carrental.car.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.rafaldobkowski.carrental.car.dto.CarResponse;
import pl.rafaldobkowski.carrental.car.dto.CreateCarRequest;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarRequest;
import pl.rafaldobkowski.carrental.car.exception.CarAlreadyExistsException;
import pl.rafaldobkowski.carrental.car.exception.CarNotFoundException;
import pl.rafaldobkowski.carrental.car.model.*;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarStatusRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    private Car createTestCar() {
        return new Car(
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
    }

    private CreateCarRequest createTestCreateCarRequest() {
        return new CreateCarRequest(
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
    }

    private UpdateCarRequest createTestUpdateCarRequest() {
        return new UpdateCarRequest(
                "Toyota",
                "Corolla",
                2024,
                "BI54321",
                "JTDBR32E720654321",
                CarFuelType.HYBRID,
                CarTransmissionType.AUTOMATIC,
                CarBodyType.SEDAN,
                5,
                CarCategory.STANDARD,
                "Black",
                new BigDecimal("249.99"),
                "Zaktualizowany samochód testowy"
        );
    }

    @Test
    void shouldThrowCarNotFoundExceptionWhenCarDoesNotExist() {
        Long carId = 99L;

        when(carRepository.findById(carId))
                .thenReturn(Optional.empty());

        CarNotFoundException exception = assertThrows(
                CarNotFoundException.class,
                () -> carService.getCarById(carId)
        );

        assertEquals(
                "Car with id 99 was not found.",
                exception.getMessage()
        );

        verify(carRepository).findById(carId);
    }

    @Test
    void shouldReturnCarWhenCarExists() {
        Long carId = 1L;
        Car car = createTestCar();

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        CarResponse response = carService.getCarById(carId);

        assertEquals("Toyota", response.brand());
        assertEquals("Corolla", response.model());
        assertEquals(CarStatus.ACTIVE, response.status());

        verify(carRepository).findById(carId);
    }

    @Test
    void shouldUpdateCarStatusWhenCarExists() {
        Long carId = 1L;
        Car car = createTestCar();

        UpdateCarStatusRequest request =
                new UpdateCarStatusRequest(CarStatus.IN_SERVICE);

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        CarResponse response =
                carService.updateCarStatus(carId, request);

        assertEquals(
                CarStatus.IN_SERVICE,
                response.status()
        );

        assertEquals(
                CarStatus.IN_SERVICE,
                car.getStatus()
        );

        verify(carRepository).findById(carId);
    }

    @Test
    void shouldThrowCarAlreadyExistsExceptionWhenVinAlreadyExists() {
        CreateCarRequest request = createTestCreateCarRequest();

        when(carRepository.existsByVin(request.vin()))
                .thenReturn(true);

        CarAlreadyExistsException exception = assertThrows(
                CarAlreadyExistsException.class,
                () -> carService.createCar(request)
        );

        assertEquals(
                "Car with this VIN already exists",
                exception.getMessage()
        );

        verify(carRepository).existsByVin(request.vin());

        verify(carRepository, never())
                .save(any(Car.class));
    }


    @Test
    void shouldThrowCarAlreadyExistsExceptionWhenRegistrationNumberAlreadyExists() {
        CreateCarRequest request = createTestCreateCarRequest();

        when(carRepository.existsByVin(request.vin()))
                .thenReturn(false);

        when(carRepository.existsByRegistrationNumber(
                request.registrationNumber()))
                .thenReturn(true);

        CarAlreadyExistsException exception = assertThrows(
                CarAlreadyExistsException.class,
                () -> carService.createCar(request)
        );

        assertEquals(
                "Car with this registration number already exists",
                exception.getMessage()
        );

        verify(carRepository).existsByRegistrationNumber(request.registrationNumber());

        verify(carRepository, never())
                .save(any(Car.class));

    }

    @Test
    void shouldCreateCarWhenVinAndRegistrationNumberAreUnique() {
        CreateCarRequest request = createTestCreateCarRequest();

        when(carRepository.existsByVin(request.vin()))
                .thenReturn(false);

        when(carRepository.existsByRegistrationNumber(
                request.registrationNumber()))
                .thenReturn(false);

        when(carRepository.save(any(Car.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0, Car.class)
                );

        CarResponse response = carService.createCar(request);

        assertEquals("Toyota", response.brand());
        assertEquals("Corolla", response.model());
        assertEquals("BI12345", response.registrationNumber());
        assertEquals(CarStatus.ACTIVE, response.status());

        verify(carRepository).existsByVin(request.vin());

        verify(carRepository)
                .existsByRegistrationNumber(request.registrationNumber());

        verify(carRepository).save(any(Car.class));
    }

    @Test
    void shouldUpdateCarWhenDataIsValid() {
        Long carId = 1L;
        Car car = createTestCar();
        UpdateCarRequest request = createTestUpdateCarRequest();

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        when(carRepository.existsByVinAndIdNot(request.vin(), carId))
                .thenReturn(false);

        when(carRepository.existsByRegistrationNumberAndIdNot(
                request.registrationNumber(), carId))
                .thenReturn(false);

        CarResponse response = carService.updateCar(carId, request);

        assertEquals("BI54321", response.registrationNumber());
        assertEquals("JTDBR32E720654321", response.vin());
        assertEquals(CarFuelType.HYBRID, response.fuelType());
        assertEquals("Black", response.color());
        assertEquals(
                new BigDecimal("249.99"),
                response.dailyPrice()
        );

        verify(carRepository).findById(carId);

        verify(carRepository)
                .existsByVinAndIdNot(request.vin(), carId);

        verify(carRepository)
                .existsByRegistrationNumberAndIdNot(
                        request.registrationNumber(), carId
                );
    }

}