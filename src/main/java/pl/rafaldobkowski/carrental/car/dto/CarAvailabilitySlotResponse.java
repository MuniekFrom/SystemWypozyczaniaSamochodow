package pl.rafaldobkowski.carrental.car.dto;

public record CarAvailabilitySlotResponse(
        String time,
        boolean available
) {
}
