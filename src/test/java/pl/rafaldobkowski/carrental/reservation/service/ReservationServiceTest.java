package pl.rafaldobkowski.carrental.reservation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.rafaldobkowski.carrental.auth.exception.AccountBlockedException;
import pl.rafaldobkowski.carrental.car.exception.CarNotFoundException;
import pl.rafaldobkowski.carrental.car.model.*;
import pl.rafaldobkowski.carrental.car.repository.CarRepository;
import pl.rafaldobkowski.carrental.reservation.dto.CreateReservationRequest;
import pl.rafaldobkowski.carrental.reservation.dto.ReservationResponse;
import pl.rafaldobkowski.carrental.reservation.exception.CarNotAvailableException;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationPeriodException;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationStatusException;
import pl.rafaldobkowski.carrental.reservation.exception.ReservationNotFoundException;
import pl.rafaldobkowski.carrental.reservation.model.Reservation;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.repository.ReservationRepository;
import pl.rafaldobkowski.carrental.user.exception.UserNotFoundException;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldRejectReservationWhenEndDateIsNotAfterStartDate() {
        LocalDateTime startAt = futureSlot(5);
        LocalDateTime endAt = startAt.minusDays(1);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        startAt,
                        endAt
                );

        InvalidReservationPeriodException exception =
                assertThrows(
                        InvalidReservationPeriodException.class,
                        () -> reservationService.createReservation(
                                1L,
                                request
                        )
                );

        assertEquals(
                "End date and time must be after start date and time",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                carRepository,
                reservationRepository
        );
    }

    @Test
    void shouldRejectReservationWhenStartDateIsInThePast() {
        LocalDateTime startAt = pastSlot(1);
        LocalDateTime endAt = futureSlot(2);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        startAt,
                        endAt
                );

        InvalidReservationPeriodException exception =
                assertThrows(
                        InvalidReservationPeriodException.class,
                        () -> reservationService.createReservation(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Start date and time cannot be in the past",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                carRepository,
                reservationRepository
        );
    }

    @Test
    void shouldRejectReservationOutsideThirtyMinuteSlot() {
        LocalDateTime startAt = futureSlot(1).plusMinutes(15);
        LocalDateTime endAt = futureSlot(2);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        startAt,
                        endAt
                );

        InvalidReservationPeriodException exception =
                assertThrows(
                        InvalidReservationPeriodException.class,
                        () -> reservationService.createReservation(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Reservation times must use 30-minute intervals",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                carRepository,
                reservationRepository
        );
    }

    @Test
    void shouldRejectReservationWhenUserDoesNotExist() {
        Long userId = 99L;

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        futureSlot(1),
                        futureSlot(3)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class,
                        () -> reservationService.createReservation(
                                userId,
                                request
                        )
                );

        assertEquals(
                "User with id 99 was not found",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);

        verifyNoInteractions(
                carRepository,
                reservationRepository
        );
    }

    @Test
    void shouldRejectReservationWhenUserIsBlocked() {
        Long userId = 1L;

        User user = createTestUser();

        user.block();

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        futureSlot(1),
                        futureSlot(3)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        AccountBlockedException exception =
                assertThrows(
                        AccountBlockedException.class,
                        () -> reservationService.createReservation(
                                userId,
                                request
                        )
                );

        assertEquals(
                "User account is blocked",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);

        verifyNoInteractions(
                carRepository,
                reservationRepository
        );
    }

    @Test
    void shouldRejectReservationWhenCarDoesNotExist() {
        Long userId = 1L;
        Long carId = 99L;

        User user = createTestUser();

        CreateReservationRequest request =
                new CreateReservationRequest(
                        carId,
                        futureSlot(1),
                        futureSlot(3)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(carRepository.findById(carId))
                .thenReturn(Optional.empty());

        CarNotFoundException exception =
                assertThrows(
                        CarNotFoundException.class,
                        () -> reservationService.createReservation(
                                userId,
                                request
                        )
                );

        assertEquals(
                "Car with id 99 was not found.",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);
        verify(carRepository).findById(carId);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectReservationWhenCarIsNotActive() {
        Long userId = 1L;
        Long carId = 2L;

        User user = createTestUser();

        Car car = createTestCar();

        car.changeStatus(CarStatus.IN_SERVICE);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        carId,
                        futureSlot(1),
                        futureSlot(3)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        CarNotAvailableException exception =
                assertThrows(
                        CarNotAvailableException.class,
                        () -> reservationService.createReservation(
                                userId,
                                request
                        )
                );

        assertEquals(
                "Car is not available for reservation",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);
        verify(carRepository).findById(carId);

        verifyNoInteractions(reservationRepository);
    }

    @Test
    void shouldRejectReservationWhenPeriodOverlaps() {
        Long userId = 1L;
        Long carId = 2L;

        LocalDateTime startAt = futureSlot(2);
        LocalDateTime endAt = futureSlot(5);

        User user = createTestUser();

        Car car = mock(Car.class);

        when(car.getId()).thenReturn(carId);
        when(car.getStatus()).thenReturn(CarStatus.ACTIVE);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        carId,
                        startAt,
                        endAt
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        when(reservationRepository.existsOverlappingReservation(
                carId,
                startAt.minusHours(4),
                endAt.plusHours(4),
                List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.IN_PROGRESS
                )
        )).thenReturn(true);

        CarNotAvailableException exception =
                assertThrows(
                        CarNotAvailableException.class,
                        () -> reservationService.createReservation(
                                userId,
                                request
                        )
                );

        assertEquals(
                "Car is not available in selected period",
                exception.getMessage()
        );

        verify(reservationRepository)
                .existsOverlappingReservation(
                        carId,
                        startAt.minusHours(4),
                        endAt.plusHours(4),
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.IN_PROGRESS
                        )
                );

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }


    @Test
    void shouldCreateReservationAndRoundPriceUpToStartedDay() {
        Long userId = 1L;
        Long carId = 2L;

        LocalDateTime startAt = futureSlot(2);
        LocalDateTime endAt = startAt.plusDays(2).plusHours(3);

        User user = mock(User.class);
        Car car = mock(Car.class);

        when(user.getId()).thenReturn(userId);
        when(user.getEmail())
                .thenReturn("anna.nowak@example.com");
        when(user.isBlocked()).thenReturn(false);

        when(car.getId()).thenReturn(carId);
        when(car.getStatus()).thenReturn(CarStatus.ACTIVE);
        when(car.getDailyPrice())
                .thenReturn(new BigDecimal("199.99"));
        when(car.getBrand()).thenReturn("Toyota");
        when(car.getModel()).thenReturn("Corolla");

        CreateReservationRequest request =
                new CreateReservationRequest(
                        carId,
                        startAt,
                        endAt
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        when(reservationRepository.existsOverlappingReservation(
                carId,
                startAt.minusHours(4),
                endAt.plusHours(4),
                List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.IN_PROGRESS
                )
        )).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0, Reservation.class)
                );

        ReservationResponse response =
                reservationService.createReservation(
                        userId,
                        request
                );

        assertEquals(userId, response.userId());
        assertEquals(carId, response.carId());
        assertEquals("anna.nowak@example.com",
                response.userEmail());
        assertEquals("Toyota", response.carBrand());
        assertEquals("Corolla", response.carModel());
        assertEquals(startAt, response.startAt());
        assertEquals(endAt, response.endAt());
        assertEquals(
                new BigDecimal("599.97"),
                response.totalPrice()
        );
        assertEquals(
                ReservationStatus.PENDING,
                response.status()
        );
        assertNotNull(response.createdAt());

        ArgumentCaptor<Reservation> reservationCaptor =
                ArgumentCaptor.forClass(Reservation.class);

        verify(reservationRepository)
                .save(reservationCaptor.capture());

        Reservation savedReservation =
                reservationCaptor.getValue();

        assertSame(user, savedReservation.getUser());
        assertSame(car, savedReservation.getCar());
        assertEquals(
                new BigDecimal("599.97"),
                savedReservation.getTotalPrice()
        );
        assertEquals(
                ReservationStatus.PENDING,
                savedReservation.getStatus()
        );
    }


    @Test
    void shouldReturnEmptyListWhenUserHasNoReservations() {
        Long userId = 1L;

        User user = createTestUser();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(reservationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());

        List<ReservationResponse> responses =
                reservationService.getUserReservations(userId);

        assertTrue(responses.isEmpty());

        verify(userRepository).findById(userId);

        verify(reservationRepository)
                .findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldReturnUserReservations() {
        Long userId = 1L;

        User user = createTestUser();

        Car car = createTestCar();

        LocalDateTime startAt = futureSlot(1);
        LocalDateTime endAt = startAt.plusDays(2);

        Reservation reservation = new Reservation(
                user,
                car,
                startAt,
                endAt,
                new BigDecimal("399.98")
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(reservationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(reservation));

        List<ReservationResponse> responses =
                reservationService.getUserReservations(userId);

        assertEquals(1, responses.size());

        ReservationResponse response = responses.getFirst();

        assertEquals("anna.nowak@example.com", response.userEmail());
        assertEquals("Toyota", response.carBrand());
        assertEquals("Corolla", response.carModel());
        assertEquals(startAt, response.startAt());
        assertEquals(endAt, response.endAt());
        assertEquals(new BigDecimal("399.98"), response.totalPrice());
        assertEquals(ReservationStatus.PENDING, response.status());

        verify(userRepository).findById(userId);

        verify(reservationRepository)
                .findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldCancelUserReservation() {
        Long userId = 1L;
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        when(reservationRepository.findByIdAndUserId(
                reservationId,
                userId
        )).thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.cancelReservation(
                        userId,
                        reservationId
                );

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        assertEquals(
                ReservationStatus.CANCELLED,
                response.status()
        );

        verify(reservationRepository)
                .findByIdAndUserId(reservationId, userId);
    }

    @Test
    void shouldThrowReservationNotFoundWhenCancellingUnavailableReservation() {
        Long userId = 1L;
        Long reservationId = 99L;

        when(reservationRepository.findByIdAndUserId(
                reservationId,
                userId
        )).thenReturn(Optional.empty());

        ReservationNotFoundException exception = assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.cancelReservation(
                        userId,
                        reservationId
                )
        );

        assertEquals(
                "Reservation not found",
                exception.getMessage()
        );

        verify(reservationRepository)
                .findByIdAndUserId(reservationId, userId);
    }

    @Test
    void shouldRejectCancellingAlreadyCancelledReservation() {
        Long userId = 1L;
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        reservation.cancel();

        when(reservationRepository.findByIdAndUserId(
                reservationId,
                userId
        )).thenReturn(Optional.of(reservation));

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                () -> reservationService.cancelReservation(
                        userId,
                        reservationId
                )
        );

        assertEquals(
                "Reservation cannot be cancelled in its current status",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository)
                .findByIdAndUserId(reservationId, userId);
    }


    @Test
    void shouldReturnAllReservations() {
        User user = createTestUser();
        Car car = createTestCar();

        LocalDateTime startAt = futureSlot(1);
        LocalDateTime endAt = startAt.plusDays(2);

        Reservation reservation = new Reservation(
                user,
                car,
                startAt,
                endAt,
                new BigDecimal("399.98")
        );

        when(reservationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(reservation));

        List<ReservationResponse> responses =
                reservationService.getAllReservations();

        assertEquals(1, responses.size());

        ReservationResponse response = responses.getFirst();

        assertEquals("anna.nowak@example.com", response.userEmail());
        assertEquals("Toyota", response.carBrand());
        assertEquals("Corolla", response.carModel());
        assertEquals(startAt, response.startAt());
        assertEquals(endAt, response.endAt());
        assertEquals(new BigDecimal("399.98"), response.totalPrice());
        assertEquals(ReservationStatus.PENDING, response.status());

        verify(reservationRepository)
                .findAllByOrderByCreatedAtDesc();
    }


    @Test
    void shouldConfirmReservation() {
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.confirmReservation(reservationId);

        assertEquals(
                ReservationStatus.CONFIRMED,
                reservation.getStatus()
        );

        assertEquals(
                ReservationStatus.CONFIRMED,
                response.status()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldThrowReservationNotFoundWhenConfirmingMissingReservation() {
        Long reservationId = 99L;

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.empty());

        ReservationNotFoundException exception = assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.confirmReservation(reservationId)
        );

        assertEquals(
                "Reservation not found",
                exception.getMessage()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldRejectConfirmingCancelledReservation() {
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        reservation.cancel();

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                () -> reservationService.confirmReservation(reservationId)
        );

        assertEquals(
                "Only pending reservations can be confirmed",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldThrowReservationNotFoundWhenCompletingMissingReservation() {
        Long reservationId = 99L;

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.empty());

        ReservationNotFoundException exception = assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.completeReservation(reservationId)
        );

        assertEquals(
                "Reservation not found",
                exception.getMessage()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldRejectCompletingPendingReservation() {
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                () -> reservationService.completeReservation(reservationId)
        );

        assertEquals(
                "Only in-progress reservations can be completed",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.PENDING,
                reservation.getStatus()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldCompleteInProgressReservation() {
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        reservation.confirm();
        reservation.startRental();

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.completeReservation(reservationId);

        assertEquals(
                ReservationStatus.COMPLETED,
                reservation.getStatus()
        );

        assertEquals(
                ReservationStatus.COMPLETED,
                response.status()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldStartConfirmedReservation() {
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                futureSlot(1),
                futureSlot(3),
                new BigDecimal("399.98")
        );

        reservation.confirm();

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        ReservationResponse response =
                reservationService.startReservation(reservationId);

        assertEquals(
                ReservationStatus.IN_PROGRESS,
                reservation.getStatus()
        );

        assertEquals(
                ReservationStatus.IN_PROGRESS,
                response.status()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldThrowReservationNotFoundWhenStartingMissingReservation() {
        Long reservationId = 99L;

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.empty());

        ReservationNotFoundException exception = assertThrows(
                ReservationNotFoundException.class,
                () -> reservationService.startReservation(reservationId)
        );

        assertEquals(
                "Reservation not found",
                exception.getMessage()
        );

        verify(reservationRepository)
                .findById(reservationId);
    }

    @Test
    void shouldRejectStartingPendingReservation() {
        Long reservationId = 10L;

        User user = createTestUser();
        Car car = createTestCar();

        Reservation reservation = new Reservation(
                user,
                car,
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 10, 0),
                new BigDecimal("399.98")
        );

        when(reservationRepository.findById(reservationId))
                .thenReturn(Optional.of(reservation));

        InvalidReservationStatusException exception = assertThrows(
                InvalidReservationStatusException.class,
                () -> reservationService.startReservation(reservationId)
        );

        assertEquals(
                "Only confirmed reservations can be started",
                exception.getMessage()
        );

        assertEquals(
                ReservationStatus.PENDING,
                reservation.getStatus()
        );

        verify(reservationRepository).findById(reservationId);
    }

    private LocalDateTime futureSlot(long daysFromNow) {
        return LocalDateTime.now()
                .plusDays(daysFromNow)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private LocalDateTime pastSlot(long daysBeforeNow) {
        return LocalDateTime.now()
                .minusDays(daysBeforeNow)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private User createTestUser(){
        return new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );
    }

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

}
