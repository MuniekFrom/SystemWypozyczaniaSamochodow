package pl.rafaldobkowski.carrental.car.dto;

import jakarta.validation.constraints.NotNull;
import pl.rafaldobkowski.carrental.car.model.CarStatus;

public record UpdateCarStatusRequest(
        @NotNull
        CarStatus status) {
}
