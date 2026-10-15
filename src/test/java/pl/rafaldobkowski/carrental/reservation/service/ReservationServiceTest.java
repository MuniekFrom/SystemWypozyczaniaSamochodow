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
import java.time.LocalDate;
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
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = startDate.minusDays(1);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        startDate,
                        endDate
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
                "End date must be after start date",
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
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(2);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        1L,
                        startDate,
                        endDate
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
                "Start date cannot be in the past",
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
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(3)
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
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(3)
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
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(3)
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
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(3)
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

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);

        User user = createTestUser();

        Car car = mock(Car.class);

        when(car.getId()).thenReturn(carId);
        when(car.getStatus()).thenReturn(CarStatus.ACTIVE);

        CreateReservationRequest request =
                new CreateReservationRequest(
                        carId,
                        startDate,
                        endDate
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        when(reservationRepository.existsOverlappingReservation(
                carId,
                startDate,
                endDate,
                List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED
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
                        startDate,
                        endDate,
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.CONFIRMED
                        )
                );

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }


    @Test
    void shouldCreateReservationAndCalculateTotalPrice() {
        Long userId = 1L;
        Long carId = 2L;

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);

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
                        startDate,
                        endDate
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(carRepository.findById(carId))
                .thenReturn(Optional.of(car));

        when(reservationRepository.existsOverlappingReservation(
                carId,
                startDate,
                endDate,
                List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED
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
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
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

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(2);

        Reservation reservation = new Reservation(
                user,
                car,
                startDate,
                endDate,
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
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
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
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
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
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
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

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(2);

        Reservation reservation = new Reservation(
                user,
                car,
                startDate,
                endDate,
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
        assertEquals(startDate, response.startDate());
        assertEquals(endDate, response.endDate());
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
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
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
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
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