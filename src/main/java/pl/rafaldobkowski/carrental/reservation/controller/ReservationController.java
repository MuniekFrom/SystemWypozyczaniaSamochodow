package pl.rafaldobkowski.carrental.reservation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rafaldobkowski.carrental.reservation.dto.CreateReservationRequest;
import pl.rafaldobkowski.carrental.reservation.dto.ReservationResponse;
import pl.rafaldobkowski.carrental.reservation.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
            ReservationService reservationService) {

        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReservationRequest request) {

        Long userId = Long.valueOf(jwt.getSubject());

        ReservationResponse createdReservation =
                reservationService.createReservation(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdReservation);
    }
}