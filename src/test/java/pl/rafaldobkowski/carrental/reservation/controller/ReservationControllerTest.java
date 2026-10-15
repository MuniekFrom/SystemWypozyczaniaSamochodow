package pl.rafaldobkowski.carrental.reservation.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
import java.time.LocalDate;
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

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);

        ReservationResponse response =
                new ReservationResponse(
                        10L,
                        1L,
                        "anna.nowak@example.com",
                        2L,
                        "Toyota",
                        "Corolla",
                        startDate,
                        endDate,
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
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(startDate, endDate);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject("1")
                                ))
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
                .andExpect(jsonPath("$.startDate")
                        .value(startDate.toString()))
                .andExpect(jsonPath("$.endDate")
                        .value(endDate.toString()))
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
                startDate,
                requestCaptor.getValue().startDate()
        );
        assertEquals(
                endDate,
                requestCaptor.getValue().endDate()
        );
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingReservationWithoutToken()
            throws Exception {

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);

        String requestJson = """
                {
                  "carId": 2,
                  "startDate": "%s",
                  "endDate": "%s"
                }
                """.formatted(startDate, endDate);

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

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);

        String requestJson = """
            {
              "carId": null,
              "startDate": "%s",
              "endDate": "%s"
            }
            """.formatted(startDate, endDate);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject("1")
                                ))
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

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);

        String message =
                "Car is not available in selected period";

        when(reservationService.createReservation(
                anyLong(),
                any(CreateReservationRequest.class)
        )).thenThrow(new CarNotAvailableException(message));

        String requestJson = """
            {
              "carId": 2,
              "startDate": "%s",
              "endDate": "%s"
            }
            """.formatted(startDate, endDate);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject("1")
                                ))
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

        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now().plusDays(3);

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
              "startDate": "%s",
              "endDate": "%s"
            }
            """.formatted(startDate, endDate);

        mockMvc.perform(
                        post("/api/reservations")
                                .with(jwt().jwt(jwt ->
                                        jwt.subject("1")
                                ))
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
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                new BigDecimal("399.98"),
                ReservationStatus.PENDING,
                Instant.parse("2026-10-14T10:00:00Z")
        );

        when(reservationService.getUserReservations(userId))
                .thenReturn(List.of(reservation));

        mockMvc.perform(
                        get("/api/reservations/me")
                                .with(jwt().jwt(jwt -> jwt.subject("1")))
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
                .andExpect(jsonPath("$[0].startDate").value("2026-11-10"))
                .andExpect(jsonPath("$[0].endDate").value("2026-11-12"))
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
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
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
                                .with(jwt().jwt(jwt -> jwt.subject("1")))
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
                                .with(jwt().jwt(jwt -> jwt.subject("1")))
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
                                .with(jwt().jwt(jwt -> jwt.subject("1")))
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

}