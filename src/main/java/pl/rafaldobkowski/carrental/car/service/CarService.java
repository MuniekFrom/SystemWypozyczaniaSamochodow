package pl.rafaldobkowski.carrental.car.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rafaldobkowski.carrental.car.dto.CarResponse;
import pl.rafaldobkowski.carrental.car.dto.CreateCarRequest;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarRequest;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarStatusRequest;
import pl.rafaldobkowski.carrental.car.exception.CarAlreadyExistsException;
import pl.rafaldobkowski.carrental.car.exception.CarNotFoundException;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.car.model.CarStatus;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationPeriodException;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.time.LocalDate;
import java.util.List;

@Service
public class CarService {

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.IN_PROGRESS
            );

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    private CarResponse mapToResponse(Car car) {
        return new CarResponse(
                car.getId(),
                car.getBrand(),
                car.getModel(),
                car.getProductionYear(),
                car.getRegistrationNumber(),
                car.getVin(),
                car.getFuelType(),
                car.getTransmissionType(),
                car.getBodyType(),
                car.getNumberOfSeats(),
                car.getCategory(),
                car.getColor(),
                car.getDailyPrice(),
                car.getStatus(),
                car.getDescription(),
                car.getEngineCapacityCm3(),
                car.getPowerHp(),
                car.getDriveType(),
                car.getAccelerationTo100Seconds()
        );
    }

    @Transactional
    public CarResponse createCar(CreateCarRequest request) {

        if (carRepository.existsByVin(request.vin())) {
            throw new CarAlreadyExistsException(
                    "Car with this VIN already exists"
            );
        }

        if (carRepository.existsByRegistrationNumber(
                request.registrationNumber()
        )) {
            throw new CarAlreadyExistsException(
                    "Car with this registration number already exists"
            );
        }

        Car car = new Car(
                request.brand(),
                request.model(),
                request.productionYear(),
                request.registrationNumber(),
                request.vin(),
                request.fuelType(),
                request.transmissionType(),
                request.bodyType(),
                request.numberOfSeats(),
                request.category(),
                request.color(),
                request.dailyPrice(),
                request.description()
        );

        car.updateTechnicalData(
                request.engineCapacityCm3(),
                request.powerHp(),
                request.driveType(),
                request.accelerationTo100Seconds()
        );

        Car savedCar = carRepository.save(car);

        return mapToResponse(savedCar);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> getAllCars() {
        return carRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarResponse getCarById(Long id) {
        Car car = findCarByIdOrThrow(id);

        return mapToResponse(car);
    }

    @Transactional
    public CarResponse updateCar(
            Long id,
            UpdateCarRequest request
    ) {
        Car car = findCarByIdOrThrow(id);

        if (carRepository.existsByVinAndIdNot(
                request.vin(),
                id
        )) {
            throw new CarAlreadyExistsException(
                    "Car with this VIN already exists"
            );
        }

        if (carRepository.existsByRegistrationNumberAndIdNot(
                request.registrationNumber(),
                id
        )) {
            throw new CarAlreadyExistsException(
                    "Car with this registration number already exists"
            );
        }

        car.updateDetails(
                request.brand(),
                request.model(),
                request.productionYear(),
                request.registrationNumber(),
                request.vin(),
                request.fuelType(),
                request.transmissionType(),
                request.bodyType(),
                request.numberOfSeats(),
                request.category(),
                request.color(),
                request.dailyPrice(),
                request.description()
        );

        car.updateTechnicalData(
                request.engineCapacityCm3(),
                request.powerHp(),
                request.driveType(),
                request.accelerationTo100Seconds()
        );

        return mapToResponse(car);
    }

    @Transactional
    public CarResponse updateCarStatus(
            Long id,
            UpdateCarStatusRequest request
    ) {
        Car car = findCarByIdOrThrow(id);

        car.changeStatus(request.status());

        return mapToResponse(car);
    }

    @Transactional(readOnly = true)
    public List<CarResponse> getAvailableCars(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidReservationPeriodException(
                    "Start date cannot be in the past"
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new InvalidReservationPeriodException(
                    "End date must be after start date"
            );
        }

        return carRepository.findAvailableCars(
                        startDate,
                        endDate,
                        CarStatus.ACTIVE,
                        BLOCKING_STATUSES
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private Car findCarByIdOrThrow(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() ->
                        new CarNotFoundException(
                                "Car with id "
                                        + id
                                        + " was not found."
                        )
                );
    }
}