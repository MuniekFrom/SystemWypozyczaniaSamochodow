package pl.rafaldobkowski.carrental.car.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rafaldobkowski.carrental.car.dto.CarAvailabilitySlotResponse;
import pl.rafaldobkowski.carrental.car.dto.CarResponse;
import pl.rafaldobkowski.carrental.car.dto.CreateCarRequest;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarRequest;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarStatusRequest;
import pl.rafaldobkowski.carrental.car.service.CarService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping
    public ResponseEntity<CarResponse> createCar(
            @Valid @RequestBody CreateCarRequest request) {

        CarResponse carResponse = carService.createCar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(carResponse);
    }

    @GetMapping
    public ResponseEntity<List<CarResponse>> getAllCars() {
        return ResponseEntity.ok(carService.getAllCars());
    }

    @GetMapping("/available")
    public ResponseEntity<List<CarResponse>> getAvailableCars(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        return ResponseEntity.ok(
                carService.getAvailableCars(startAt, endAt)
        );
    }

    @GetMapping("/{id}/availability-slots")
    public ResponseEntity<List<CarAvailabilitySlotResponse>>
    getAvailabilitySlots(
            @PathVariable Long id,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(
                carService.getAvailabilitySlots(id, date)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getCarById(
            @PathVariable Long id) {

        return ResponseEntity.ok(carService.getCarById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> updateCar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarRequest request) {

        return ResponseEntity.ok(carService.updateCar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CarResponse> updateCarStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarStatusRequest request) {

        return ResponseEntity.ok(
                carService.updateCarStatus(id, request)
        );
    }
}
