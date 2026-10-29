package pl.rafaldobkowski.carrental.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import pl.rafaldobkowski.carrental.reservation.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,

        Long userId,
        String userEmail,

        Long carId,
        String carBrand,
        String carModel,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime startAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime endAt,

        BigDecimal totalPrice,
        ReservationStatus status,
        Instant createdAt
) {
}
