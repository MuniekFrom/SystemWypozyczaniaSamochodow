package pl.rafaldobkowski.carrental.car.dto;

import pl.rafaldobkowski.carrental.car.model.*;

import java.math.BigDecimal;

public record CarResponse(
        Long id,
        String brand,
        String model,
        Integer productionYear,
        String registrationNumber,
        String vin,
        CarFuelType fuelType,
        CarTransmissionType transmissionType,
        CarBodyType bodyType,
        Integer numberOfSeats,
        CarCategory category,
        String color,
        BigDecimal dailyPrice,
        CarStatus status,
        String description
        ) {


}
