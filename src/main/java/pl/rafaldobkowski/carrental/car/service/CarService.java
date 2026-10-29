package pl.rafaldobkowski.carrental.car.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rafaldobkowski.carrental.car.dto.CarAvailabilitySlotResponse;
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
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.repository.ReservationRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class CarService {

    private static final Duration TURNAROUND_BUFFER = Duration.ofHours(4);
    private static final int TIME_SLOT_MINUTES = 30;
    private static final int TIME_SLOTS_PER_DAY = 48;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.IN_PROGRESS
            );

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    public CarService(
            CarRepository carRepository,
            ReservationRepository reservationRepository) {

        this.carRepository = carRepository;
        this.reservationRepository = reservationRepository;
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
        return mapToResponse(findCarByIdOrThrow(id));
    }

    @Transactional
    public CarResponse updateCar(
            Long id,
            UpdateCarRequest request
    ) {
        Car car = findCarByIdOrThrow(id);

        if (carRepository.existsByVinAndIdNot(request.vin(), id)) {
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
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        validatePeriod(startAt, endAt);

        LocalDateTime blockingWindowStart =
                startAt.minus(TURNAROUND_BUFFER);

        LocalDateTime blockingWindowEnd =
                endAt.plus(TURNAROUND_BUFFER);

        return carRepository.findAvailableCars(
                        blockingWindowStart,
                        blockingWindowEnd,
                        CarStatus.ACTIVE,
                        BLOCKING_STATUSES
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarAvailabilitySlotResponse> getAvailabilitySlots(
            Long carId,
            LocalDate date
    ) {
        Car car = findCarByIdOrThrow(carId);

        if (date.isBefore(LocalDate.now())) {
            throw new InvalidReservationPeriodException(
                    "Availability date cannot be in the past"
            );
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Reservation> blockingReservations =
                reservationRepository.findBlockingReservationsForCar(
                        carId,
                        dayStart.minus(TURNAROUND_BUFFER),
                        dayEnd,
                        BLOCKING_STATUSES
                );

        LocalDateTime now = LocalDateTime.now();

        return IntStream.range(0, TIME_SLOTS_PER_DAY)
                .mapToObj(index -> {
                    LocalDateTime slotAt = dayStart.plusMinutes(
                            (long) index * TIME_SLOT_MINUTES
                    );

                    boolean available = car.getStatus() == CarStatus.ACTIVE
                            && !slotAt.isBefore(now)
                            && blockingReservations.stream()
                            .noneMatch(reservation ->
                                    isBlockedPickupSlot(slotAt, reservation)
                            );

                    return new CarAvailabilitySlotResponse(
                            slotAt.toLocalTime().format(TIME_FORMATTER),
                            available
                    );
                })
                .toList();
    }

    private boolean isBlockedPickupSlot(
            LocalDateTime slotAt,
            Reservation reservation) {

        LocalDateTime blockedUntil =
                reservation.getEndAt().plus(TURNAROUND_BUFFER);

        return !slotAt.isBefore(reservation.getStartAt())
                && slotAt.isBefore(blockedUntil);
    }

    private void validatePeriod(
            LocalDateTime startAt,
            LocalDateTime endAt) {

        if (startAt.isBefore(LocalDateTime.now())) {
            throw new InvalidReservationPeriodException(
                    "Start date and time cannot be in the past"
            );
        }

        if (!endAt.isAfter(startAt)) {
            throw new InvalidReservationPeriodException(
                    "End date and time must be after start date and time"
            );
        }

        if (!isValidTimeSlot(startAt) || !isValidTimeSlot(endAt)) {
            throw new InvalidReservationPeriodException(
                    "Reservation times must use 30-minute intervals"
            );
        }
    }

    private boolean isValidTimeSlot(LocalDateTime dateTime) {
        return dateTime.getMinute() % TIME_SLOT_MINUTES == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0;
    }

    private Car findCarByIdOrThrow(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() ->
                        new CarNotFoundException(
                                "Car with id " + id + " was not found."
                        )
                );
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
}
