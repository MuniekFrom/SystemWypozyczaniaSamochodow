package pl.rafaldobkowski.carrental.reservation.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rafaldobkowski.carrental.reservation.dto.CreateReservationRequest;
import pl.rafaldobkowski.carrental.reservation.dto.ReservationResponse;
import pl.rafaldobkowski.carrental.reservation.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReservationRequest request) {

        Long userId = Long.valueOf(jwt.getSubject());

        ReservationResponse createdReservation =
                reservationService.createReservation(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdReservation);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                reservationService.getUserReservations(userId)
        );
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                reservationService.cancelReservation(
                        userId,
                        reservationId
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(
                reservationService.getAllReservations()
        );
    }

    @PatchMapping("/{reservationId}/confirm")
    public ResponseEntity<ReservationResponse> confirmReservation(
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                reservationService.confirmReservation(reservationId)
        );
    }

    @PatchMapping("/{reservationId}/complete")
    public ResponseEntity<ReservationResponse> completeReservation(
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                reservationService.completeReservation(reservationId)
        );
    }

    @PatchMapping("/{reservationId}/start")
    public ResponseEntity<ReservationResponse> startReservation(
            @PathVariable Long reservationId) {

        return ResponseEntity.ok(
                reservationService.startReservation(reservationId)
        );
    }
}
