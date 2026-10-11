package pl.rafaldobkowski.carrental.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateReservationRequest(

        @NotNull
        @Positive
        Long carId,

        @NotNull
        @FutureOrPresent
        LocalDate startDate,

        @NotNull
        @Future
        LocalDate endDate

) {
}