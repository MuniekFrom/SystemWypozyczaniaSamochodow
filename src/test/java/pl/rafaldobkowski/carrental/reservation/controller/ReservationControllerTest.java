package pl.rafaldobkowski.carrental.reservation.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pl.rafaldobkowski.carrental.reservation.dto.CreateReservationRequest;
import pl.rafaldobkowski.carrental.reservation.dto.ReservationResponse;
import pl.rafaldobkowski.carrental.reservation.exception.CarNotAvailableException;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationPeriodException;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationStatusException;
import pl.rafaldobkowski.carrental.reservation.exception.ReservationNotFoundException;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;
import pl.rafaldobkowski.carrental.reservation.service.ReservationService;
import pl.rafaldobkowski.carrental.security.SecurityConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreateReservationForAuthenticatedUser()
            throws Exception {

        LocalDateTime startAt = futureSlot(2);
        LocalDateTime endAt = futureSlot(5);

        ReservationResponse response =
                new ReservationResponse(
                        10L,
                        1L,
                        "anna.nowak@example.com",
                        2L,
                        "Toyota",
                        "Corolla",
                        startAt,
                        endAt,
                        new BigDecimal("599.97"),
                        ReservationStatus.PENDING,
                        Instant.parse("2026-10-12T10:00:00Z")
                );

        when(reservationService.createReservation(
                anyLong(),
                any(CreateReservationRequest.class)
        )).thenReturn(response);

        String requestJson = """
                {
                  "carId": 2,
                  "startAt": "%s",
                  "endAt": "%s"
                }
                """.formatted(startAt, endAt);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(clientJwt(1L))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestJson)
                )
                .andExpect(status().isCreated())
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userEmail")
                        .value("anna.nowak@example.com"))
                .andExpect(jsonPath("$.carId").value(2))
                .andExpect(jsonPath("$.carBrand")
                        .value("Toyota"))
                .andExpect(jsonPath("$.carModel")
                        .value("Corolla"))
                .andExpect(jsonPath("$.startAt")
                        .value(startAt.toString()))
                .andExpect(jsonPath("$.endAt")
                        .value(endAt.toString()))
                .andExpect(jsonPath("$.totalPrice")
                        .value(599.97))
                .andExpect(jsonPath("$.status")
                        .value("PENDING"));

        ArgumentCaptor<Long> userIdCaptor =
                ArgumentCaptor.forClass(Long.class);

        ArgumentCaptor<CreateReservationRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateReservationRequest.class
                );

        verify(reservationService).createReservation(
                userIdCaptor.capture(),
                requestCaptor.capture()
        );

        assertEquals(1L, userIdCaptor.getValue());
        assertEquals(2L, requestCaptor.getValue().carId());
        assertEquals(
                startAt,
                requestCaptor.getValue().startAt()
        );
        assertEquals(
                endAt,
                requestCaptor.getValue().endAt()
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingReservationWithoutToken()
            throws Exception {

        LocalDateTime startAt = futureSlot(2);
        LocalDateTime endAt = futureSlot(5);

        String requestJson = """
                {
                  "carId": 2,
                  "startAt": "%s",
                  "endAt": "%s"
                }
                """.formatted(startAt, endAt);

        mockMvc.perform(
                        post("/api/reservations")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestJson)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnBadRequestWhenCarIdIsMissing()
            throws Exception {

        LocalDateTime startAt = futureSlot(2);
        LocalDateTime endAt = futureSlot(5);

        String requestJson = """
            {
              "carId": null,
              "startAt": "%s",
              "endAt": "%s"
            }
            """.formatted(startAt, endAt);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(clientJwt(1L))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations"));

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnConflictWhenCarIsNotAvailable()
            throws Exception {

        LocalDateTime startAt = futureSlot(2);
        LocalDateTime endAt = futureSlot(5);

        String message =
                "Car is not available in selected period";

        when(reservationService.createReservation(
                anyLong(),
                any(CreateReservationRequest.class)
        )).thenThrow(new CarNotAvailableException(message));

        String requestJson = """
            {
              "carId": 2,
              "startAt": "%s",
              "endAt": "%s"
            }
            """.formatted(startAt, endAt);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(clientJwt(1L))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestJson)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(message))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations"));

        verify(reservationService).createReservation(
                anyLong(),
                any(CreateReservationRequest.class)
        );
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsBeforeStartDate()
            throws Exception {

        LocalDateTime startAt = futureSlot(5);
        LocalDateTime endAt = futureSlot(3);

        String message =
                "End date must be after start date";

        when(reservationService.createReservation(
                anyLong(),
                any(CreateReservationRequest.class)
        )).thenThrow(
                new InvalidReservationPeriodException(message)
        );

        String requestJson = """
            {
              "carId": 2,
              "startAt": "%s",
              "endAt": "%s"
            }
            """.formatted(startAt, endAt);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(clientJwt(1L))
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value(message))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations"));

        verify(reservationService).createReservation(
                anyLong(),
                any(CreateReservationRequest.class)
        );
    }

    @Test
    void shouldReturnCurrentUserReservations() throws Exception {
        Long userId = 1L;

        ReservationResponse reservation = new ReservationResponse(
                10L,
                userId,
                "anna.nowak@example.com",
                2L,
                "Toyota",
                "Corolla",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("399.98"),
                ReservationStatus.PENDING,
                Instant.parse("2026-10-14T10:00:00Z")
        );

        when(reservationService.getUserReservations(userId))
                .thenReturn(List.of(reservation));

        mockMvc.perform(
                        get("/api/reservations/me")
                                .with(clientJwt(1L))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].carId").value(2))
                .andExpect(jsonPath("$[0].carBrand").value("Toyota"))
                .andExpect(jsonPath("$[0].carModel").value("Corolla"))
                .andExpect(jsonPath("$[0].startAt").value("2026-11-10T10:00"))
                .andExpect(jsonPath("$[0].endAt").value("2026-11-12T13:00"))
                .andExpect(jsonPath("$[0].totalPrice").value(399.98))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(reservationService)
                .getUserReservations(userId);
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingReservationsWithoutToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/reservations/me")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldCancelCurrentUserReservation() throws Exception {
        Long userId = 1L;
        Long reservationId = 10L;

        ReservationResponse response = new ReservationResponse(
                reservationId,
                userId,
                "anna.nowak@example.com",
                2L,
                "Toyota",
                "Corolla",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("399.98"),
                ReservationStatus.CANCELLED,
                Instant.parse("2026-10-14T10:00:00Z")
        );

        when(reservationService.cancelReservation(
                userId,
                reservationId
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/cancel",
                                reservationId)
                                .with(clientJwt(1L))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(reservationService)
                .cancelReservation(userId, reservationId);
    }

    @Test
    void shouldReturnUnauthorizedWhenCancellingReservationWithoutToken()
            throws Exception {

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/cancel", 10L)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnNotFoundWhenCancellingUnavailableReservation()
            throws Exception {

        Long userId = 1L;
        Long reservationId = 99L;

        when(reservationService.cancelReservation(
                userId,
                reservationId
        )).thenThrow(new ReservationNotFoundException(
                "Reservation not found"
        ));

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/cancel",
                                reservationId)
                                .with(clientJwt(1L))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Reservation not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations/99/cancel"));

        verify(reservationService)
                .cancelReservation(userId, reservationId);
    }

    @Test
    void shouldReturnConflictWhenReservationCannotBeCancelled()
            throws Exception {

        Long userId = 1L;
        Long reservationId = 10L;

        when(reservationService.cancelReservation(
                userId,
                reservationId
        )).thenThrow(new InvalidReservationStatusException(
                "Reservation cannot be cancelled in its current status"
        ));

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/cancel",
                                reservationId)
                                .with(clientJwt(1L))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Reservation cannot be cancelled in its current status"
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/reservations/10/cancel"
                ));

        verify(reservationService)
                .cancelReservation(userId, reservationId);
    }

    @Test
    void shouldReturnAllReservationsForAdmin() throws Exception {
        ReservationResponse reservation = new ReservationResponse(
                10L,
                1L,
                "anna.nowak@example.com",
                2L,
                "Toyota",
                "Corolla",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("399.98"),
                ReservationStatus.PENDING,
                Instant.parse("2026-10-14T10:00:00Z")
        );

        when(reservationService.getAllReservations())
                .thenReturn(List.of(reservation));

        mockMvc.perform(
                        get("/api/reservations")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userEmail")
                        .value("anna.nowak@example.com"))
                .andExpect(jsonPath("$[0].carBrand").value("Toyota"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(reservationService)
                .getAllReservations();
    }

    @Test
    void shouldReturnForbiddenWhenClientGetsAllReservations()
            throws Exception {

        mockMvc.perform(
                        get("/api/reservations")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_CLIENT")
                                ))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnUnauthorizedWhenGettingAllReservationsWithoutToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/reservations")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldConfirmReservationForAdmin() throws Exception {
        Long reservationId = 10L;

        ReservationResponse response = new ReservationResponse(
                reservationId,
                1L,
                "anna.nowak@example.com",
                2L,
                "Toyota",
                "Corolla",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("399.98"),
                ReservationStatus.CONFIRMED,
                Instant.parse("2026-10-14T10:00:00Z")
        );

        when(reservationService.confirmReservation(reservationId))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/confirm",
                                reservationId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(reservationService)
                .confirmReservation(reservationId);
    }

    @Test
    void shouldReturnForbiddenWhenClientConfirmsReservation()
            throws Exception {

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/confirm", 10L)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_CLIENT")
                                ))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnUnauthorizedWhenConfirmingReservationWithoutToken()
            throws Exception {

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/confirm", 10L)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnNotFoundWhenAdminConfirmsMissingReservation()
            throws Exception {

        Long reservationId = 99L;

        when(reservationService.confirmReservation(reservationId))
                .thenThrow(new ReservationNotFoundException(
                        "Reservation not found"
                ));

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/confirm",
                                reservationId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Reservation not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations/99/confirm"));

        verify(reservationService)
                .confirmReservation(reservationId);
    }

    @Test
    void shouldReturnConflictWhenReservationCannotBeConfirmed()
            throws Exception {

        Long reservationId = 10L;

        when(reservationService.confirmReservation(reservationId))
                .thenThrow(new InvalidReservationStatusException(
                        "Only pending reservations can be confirmed"
                ));

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/confirm",
                                reservationId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only pending reservations can be confirmed"))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations/10/confirm"));

        verify(reservationService)
                .confirmReservation(reservationId);
    }

    @Test
    void shouldCompleteReservationForAdmin() throws Exception {
        Long reservationId = 10L;

        ReservationResponse response = new ReservationResponse(
                reservationId,
                1L,
                "anna.nowak@example.com",
                2L,
                "Toyota",
                "Corolla",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("399.98"),
                ReservationStatus.COMPLETED,
                Instant.parse("2026-10-14T10:00:00Z")
        );

        when(reservationService.completeReservation(reservationId))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/complete",
                                reservationId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(reservationService)
                .completeReservation(reservationId);
    }

    @Test
    void shouldReturnForbiddenWhenClientCompletesReservation()
            throws Exception {

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/complete", 10L)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_CLIENT")
                                ))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnUnauthorizedWhenCompletingReservationWithoutToken()
            throws Exception {

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/complete", 10L)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReturnNotFoundWhenAdminCompletesMissingReservation()
            throws Exception {

        Long reservationId = 99L;

        when(reservationService.completeReservation(reservationId))
                .thenThrow(new ReservationNotFoundException(
                        "Reservation not found"
                ));

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/complete",
                                reservationId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Reservation not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations/99/complete"));

        verify(reservationService)
                .completeReservation(reservationId);
    }

    @Test
    void shouldReturnConflictWhenReservationCannotBeCompleted()
            throws Exception {

        Long reservationId = 10L;

        when(reservationService.completeReservation(reservationId))
                .thenThrow(new InvalidReservationStatusException(
                        "Only in-progress reservations can be completed"
                ));

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/complete",
                                reservationId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only in-progress reservations can be completed"))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations/10/complete"));

        verify(reservationService)
                .completeReservation(reservationId);
    }

    @Test
    void shouldStartReservationForEmployee() throws Exception {
        Long reservationId = 1L;

        ReservationResponse response = new ReservationResponse(
                reservationId,
                2L,
                "jan.kowalski@example.com",
                3L,
                "Toyota",
                "Corolla",
                LocalDateTime.of(2026, 11, 10, 10, 0),
                LocalDateTime.of(2026, 11, 12, 13, 0),
                new BigDecimal("399.98"),
                ReservationStatus.IN_PROGRESS,
                Instant.parse("2026-10-21T10:00:00Z")
        );

        when(reservationService.startReservation(reservationId))
                .thenReturn(response);

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/start",
                                reservationId
                        )
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.carBrand").value("Toyota"))
                .andExpect(jsonPath("$.carModel").value("Corolla"));

        verify(reservationService).startReservation(reservationId);
    }

    @Test
    void shouldReturnForbiddenWhenClientStartsReservation() throws Exception {
        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/start", 1L)
                                .with(clientJwt(1L))
                )
                .andExpect(status().isForbidden());

        verify(reservationService, never())
                .startReservation(anyLong());
    }

    @Test
    void shouldReturnUnauthorizedWhenStartingReservationWithoutToken()
            throws Exception {

        mockMvc.perform(
                        patch("/api/reservations/{reservationId}/start", 1L)
                )
                .andExpect(status().isUnauthorized());

        verify(reservationService, never())
                .startReservation(anyLong());
    }

    @Test
    void shouldReturnNotFoundWhenStartingMissingReservation()
            throws Exception {

        Long reservationId = 99L;

        when(reservationService.startReservation(reservationId))
                .thenThrow(new ReservationNotFoundException(
                        "Reservation not found"
                ));

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/start",
                                reservationId
                        )
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Reservation not found"))
                .andExpect(jsonPath("$.path")
                        .value("/api/reservations/99/start"));

        verify(reservationService).startReservation(reservationId);
    }

    @Test
    void shouldReturnConflictWhenStartingReservationInInvalidStatus()
            throws Exception {

        Long reservationId = 1L;

        when(reservationService.startReservation(reservationId))
                .thenThrow(new InvalidReservationStatusException(
                        "Only confirmed reservations can be started"
                ));

        mockMvc.perform(
                        patch(
                                "/api/reservations/{reservationId}/start",
                                reservationId
                        )
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_EMPLOYEE")
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "Only confirmed reservations can be started"
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/reservations/1/start"
                ));

        verify(reservationService).startReservation(reservationId);
    }

    private RequestPostProcessor clientJwt(Long userId) {
        return jwt()
                .jwt(jwt -> jwt.subject(userId.toString()))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_CLIENT")
                );
    }

    private LocalDateTime futureSlot(long days) {
        return LocalDateTime.now()
                .plusDays(days)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

}
