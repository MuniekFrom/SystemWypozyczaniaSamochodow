package pl.rafaldobkowski.carrental.reservation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rafaldobkowski.carrental.auth.exception.AccountBlockedException;
import pl.rafaldobkowski.carrental.car.exception.CarNotFoundException;
import pl.rafaldobkowski.carrental.car.model.Car;
import pl.rafaldobkowski.carrental.car.model.CarStatus;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;
import pl.rafaldobkowski.carrental.reservation.dto.CreateReservationRequest;
import pl.rafaldobkowski.carrental.reservation.dto.ReservationResponse;
import pl.rafaldobkowski.carrental.reservation.exception.CarNotAvailableException;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationPeriodException;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.repository.ReservationRepository;
import pl.rafaldobkowski.carrental.user.exception.UserNotFoundException;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;
import pl.rafaldobkowski.carrental.reservation.exception.ReservationNotFoundException;

import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            CarRepository carRepository,
            UserRepository userRepository) {

        this.reservationRepository = reservationRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED
            );

    private void validatePeriod(CreateReservationRequest request) {
        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

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
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + userId + " was not found"
                ));
    }

    private Car findCarByIdOrThrow(Long carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new CarNotFoundException(
                        "Car with id " + carId + " was not found."
                ));
    }

    @Transactional
    public ReservationResponse createReservation(
            Long userId,
            CreateReservationRequest request) {

        validatePeriod(request);

        User user = findUserByIdOrThrow(userId);

        if (user.isBlocked()) {
            throw new AccountBlockedException(
                    "User account is blocked"
            );
        }

        Car car = findCarByIdOrThrow(request.carId());

        if (car.getStatus() != CarStatus.ACTIVE) {
            throw new CarNotAvailableException(
                    "Car is not available for reservation"
            );
        }

        boolean overlappingReservation =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        request.startDate(),
                        request.endDate(),
                        BLOCKING_STATUSES
                );

        if (overlappingReservation) {
            throw new CarNotAvailableException(
                    "Car is not available in selected period"
            );
        }

        long numberOfDays = ChronoUnit.DAYS.between(
                request.startDate(),
                request.endDate()
        );

        BigDecimal totalPrice = car.getDailyPrice()
                .multiply(BigDecimal.valueOf(numberOfDays));

        Reservation reservation = new Reservation(
                user,
                car,
                request.startDate(),
                request.endDate(),
                totalPrice
        );

        Reservation savedReservation =
                reservationRepository.save(reservation);

        return mapToResponse(savedReservation);
    }

    private ReservationResponse mapToResponse(
            Reservation reservation) {

        return new ReservationResponse(
                reservation.getId(),

                reservation.getUser().getId(),
                reservation.getUser().getEmail(),

                reservation.getCar().getId(),
                reservation.getCar().getBrand(),
                reservation.getCar().getModel(),

                reservation.getStartDate(),
                reservation.getEndDate(),

                reservation.getTotalPrice(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(Long userId) {
        findUserByIdOrThrow(userId);

        return reservationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse cancelReservation(
            Long userId,
            Long reservationId
    ) {
        Reservation reservation = reservationRepository
                .findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found"
                ));

        reservation.cancel();

        return mapToResponse(reservation);
    }

}