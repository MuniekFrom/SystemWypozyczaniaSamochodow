package pl.rafaldobkowski.carrental.reservation.dto;

import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservationResponse(
        Long id,

        Long userId,
        String userEmail,

        Long carId,
        String carBrand,
        String carModel,

        LocalDate startDate,
        LocalDate endDate,

        BigDecimal totalPrice,
        ReservationStatus status,
        Instant createdAt
) {
}