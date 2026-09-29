package pl.rafaldobkowski.carrental.car.dto;

import jakarta.validation.constraints.*;
import pl.rafaldobkowski.carrental.car.model.CarBodyType;
import pl.rafaldobkowski.carrental.car.model.CarCategory;
import pl.rafaldobkowski.carrental.car.model.CarFuelType;
import pl.rafaldobkowski.carrental.car.model.CarTransmissionType;

import java.math.BigDecimal;

public record UpdateCarRequest(
        @NotBlank
        @Size(max = 50)
        String brand,

        @NotBlank
        @Size(max = 50)
        String model,

        @NotNull
        @Min(1900)
        Integer productionYear,

        @NotBlank
        @Size(max = 20)
        String registrationNumber,

        @NotBlank
        @Size(min = 17, max = 17)
        String vin,

        @NotNull
        CarFuelType fuelType,

        @NotNull
        CarTransmissionType transmissionType,

        @NotNull
        CarBodyType bodyType,

        @NotNull
        @Min(1)
        @Max(9)
        Integer numberOfSeats,

        @NotNull
        CarCategory category,

        @NotBlank
        @Size(max = 30)
        String color,

        @NotNull
        @Positive
        BigDecimal dailyPrice,

        @Size(max = 1000)
        String description
) {
}
