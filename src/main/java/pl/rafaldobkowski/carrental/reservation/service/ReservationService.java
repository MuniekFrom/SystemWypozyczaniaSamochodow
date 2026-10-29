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
import pl.rafaldobkowski.carrental.reservation.exception.ReservationNotFoundException;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.repository.ReservationRepository;
import pl.rafaldobkowski.carrental.user.exception.UserNotFoundException;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReservationService {

    private static final Duration TURNAROUND_BUFFER = Duration.ofHours(4);
    private static final int TIME_SLOT_MINUTES = 30;
    private static final long MINUTES_PER_DAY = 24L * 60L;

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.IN_PROGRESS
            );

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

        LocalDateTime blockingWindowStart =
                request.startAt().minus(TURNAROUND_BUFFER);

        LocalDateTime blockingWindowEnd =
                request.endAt().plus(TURNAROUND_BUFFER);

        boolean overlappingReservation =
                reservationRepository.existsOverlappingReservation(
                        car.getId(),
                        blockingWindowStart,
                        blockingWindowEnd,
                        BLOCKING_STATUSES
                );

        if (overlappingReservation) {
            throw new CarNotAvailableException(
                    "Car is not available in selected period"
            );
        }

        long numberOfDays = calculateBillableDays(
                request.startAt(),
                request.endAt()
        );

        BigDecimal totalPrice = car.getDailyPrice()
                .multiply(BigDecimal.valueOf(numberOfDays));

        Reservation reservation = new Reservation(
                user,
                car,
                request.startAt(),
                request.endAt(),
                totalPrice
        );

        Reservation savedReservation =
                reservationRepository.save(reservation);

        return mapToResponse(savedReservation);
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

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse confirmReservation(Long reservationId) {
        Reservation reservation = findReservationByIdOrThrow(reservationId);

        reservation.confirm();

        return mapToResponse(reservation);
    }

    @Transactional
    public ReservationResponse startReservation(Long reservationId) {
        Reservation reservation = findReservationByIdOrThrow(reservationId);

        reservation.startRental();

        return mapToResponse(reservation);
    }

    @Transactional
    public ReservationResponse completeReservation(Long reservationId) {
        Reservation reservation = findReservationByIdOrThrow(reservationId);

        reservation.complete();

        return mapToResponse(reservation);
    }

    private void validatePeriod(CreateReservationRequest request) {
        LocalDateTime startAt = request.startAt();
        LocalDateTime endAt = request.endAt();

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

    private long calculateBillableDays(
            LocalDateTime startAt,
            LocalDateTime endAt) {

        long rentalMinutes = ChronoUnit.MINUTES.between(startAt, endAt);

        return Math.max(
                1L,
                (rentalMinutes + MINUTES_PER_DAY - 1L) / MINUTES_PER_DAY
        );
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

    private Reservation findReservationByIdOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found"
                ));
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),

                reservation.getUser().getId(),
                reservation.getUser().getEmail(),

                reservation.getCar().getId(),
                reservation.getCar().getBrand(),
                reservation.getCar().getModel(),

                reservation.getStartAt(),
                reservation.getEndAt(),

                reservation.getTotalPrice(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}
