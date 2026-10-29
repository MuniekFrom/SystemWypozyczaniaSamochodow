package pl.rafaldobkowski.carrental.car.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.rafaldobkowski.carrental.car.dto.CarAvailabilitySlotResponse;
import pl.rafaldobkowski.carrental.car.dto.CarResponse;
import pl.rafaldobkowski.carrental.car.dto.CreateCarRequest;
import pl.rafaldobkowski.carrental.car.dto.UpdateCarRequest;
import pl.rafaldobkowski.carrental.car.exception.CarAlreadyExistsException;
import pl.rafaldobkowski.carrental.car.exception.CarNotFoundException;
import pl.rafaldobkowski.carrental.car.model.*;
import pl.rafaldobkowski.carrental.car.service.CarService;
import pl.rafaldobkowski.carrental.reservation.exception.InvalidReservationPeriodException;
import pl.rafaldobkowski.carrental.security.SecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarController.class)
@Import(SecurityConfig.class)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarService carService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private CarResponse createTestCarResponse() {
        return new CarResponse(
                1L,
                "Toyota",
                "Corolla",
                2024,
                "BI12345",
                "JTDBR32E720123456",
                CarFuelType.GASOLINE,
                CarTransmissionType.AUTOMATIC,
                CarBodyType.SEDAN,
                5,
                CarCategory.STANDARD,
                "Gray",
                new BigDecimal("199.99"),
                CarStatus.ACTIVE,
                "Samochód testowy",
                1798,
                140,
                CarDriveType.FWD,
                new BigDecimal("9.20")
        );
    }

    private String validCreateCarJson() {
        return """
            {
              "brand": "Toyota",
              "model": "Corolla",
              "productionYear": 2024,
              "registrationNumber": "BI12345",
              "vin": "JTDBR32E720123456",
              "fuelType": "GASOLINE",
              "transmissionType": "AUTOMATIC",
              "bodyType": "SEDAN",
              "numberOfSeats": 5,
              "category": "STANDARD",
              "color": "Gray",
              "dailyPrice": 199.99,
              "description": "Samochód testowy",
              "engineCapacityCm3": 1798,
              "powerHp": 140,
              "driveType": "FWD",
              "accelerationTo100Seconds": 9.20
            }
            """;
    }

    @Test
    void shouldReturnEmptyCarList() throws Exception {
        when(carService.getAllCars())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(carService).getAllCars();
    }

    @Test
    void shouldReturnNotFoundWhenCarDoesNotExist() throws Exception {
        Long carId = 99L;
        String message = "Car with id 99 was not found.";

        when(carService.getCarById(carId))
                .thenThrow(new CarNotFoundException(message));

        mockMvc.perform(get("/api/cars/{id}", carId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/api/cars/99"));

        verify(carService).getCarById(carId);
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsNull() throws Exception {
        Long carId = 1L;

        mockMvc.perform(
                        patch("/api/cars/{id}/status", carId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "status": null
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path")
                        .value("/api/cars/1/status"));

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnCarWhenCarExists() throws Exception {
        Long carId = 1L;
        CarResponse carResponse = createTestCarResponse();

        when(carService.getCarById(carId))
                .thenReturn(carResponse);

        mockMvc.perform(
                        get("/api/cars/{id}", carId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Corolla"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.engineCapacityCm3").value(1798))
                .andExpect(jsonPath("$.powerHp").value(140))
                .andExpect(jsonPath("$.driveType").value("FWD"))
                .andExpect(jsonPath("$.accelerationTo100Seconds").value(9.2));

        verify(carService).getCarById(carId);
    }

    @Test
    void shouldCreateCarAndReturnCreatedStatus() throws Exception {
        CarResponse carResponse = createTestCarResponse();

        when(carService.createCar(any(CreateCarRequest.class)))
                .thenReturn(carResponse);

        mockMvc.perform(
                        post("/api/cars")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateCarJson())
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.engineCapacityCm3").value(1798))
                .andExpect(jsonPath("$.powerHp").value(140))
                .andExpect(jsonPath("$.driveType").value("FWD"))
                .andExpect(jsonPath("$.accelerationTo100Seconds").value(9.2));

        ArgumentCaptor<CreateCarRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateCarRequest.class);

        verify(carService).createCar(requestCaptor.capture());

        CreateCarRequest capturedRequest = requestCaptor.getValue();

        assertEquals("Toyota", capturedRequest.brand());
        assertEquals(
                "BI12345",
                capturedRequest.registrationNumber()
        );
        assertEquals(
                new BigDecimal("199.99"),
                capturedRequest.dailyPrice()
        );
        assertEquals(
                1798,
                capturedRequest.engineCapacityCm3()
        );
        assertEquals(
                140,
                capturedRequest.powerHp()
        );
        assertEquals(
                CarDriveType.FWD,
                capturedRequest.driveType()
        );
        assertEquals(
                new BigDecimal("9.20"),
                capturedRequest.accelerationTo100Seconds()
        );
    }

    @Test
    void shouldReturnConflictWhenVinAlreadyExists() throws Exception {
        String message = "Car with this VIN already exists";

        when(carService.createCar(any(CreateCarRequest.class)))
                .thenThrow(new CarAlreadyExistsException(message));

        mockMvc.perform(
                        post("/api/cars")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateCarJson())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/api/cars"));

        verify(carService)
                .createCar(any(CreateCarRequest.class));
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingCarWithoutToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/cars")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateCarJson())
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnForbiddenWhenClientCreatesCar()
            throws Exception {

        mockMvc.perform(
                        post("/api/cars")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_CLIENT")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateCarJson())
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnAvailableCarsForSelectedPeriod()
            throws Exception {

        LocalDateTime startAt =
                LocalDateTime.of(2026, 11, 20, 10, 0);
        LocalDateTime endAt =
                LocalDateTime.of(2026, 11, 22, 13, 0);

        CarResponse carResponse = createTestCarResponse();

        when(carService.getAvailableCars(startAt, endAt))
                .thenReturn(List.of(carResponse));

        mockMvc.perform(
                        get("/api/cars/available")
                                .param(
                                        "startAt",
                                        startAt.toString()
                                )
                                .param(
                                        "endAt",
                                        endAt.toString()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].brand").value("Toyota"))
                .andExpect(jsonPath("$[0].model").value("Corolla"))
                .andExpect(jsonPath("$[0].dailyPrice").value(199.99))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].engineCapacityCm3").value(1798))
                .andExpect(jsonPath("$[0].powerHp").value(140))
                .andExpect(jsonPath("$[0].driveType").value("FWD"))
                .andExpect(
                        jsonPath("$[0].accelerationTo100Seconds")
                                .value(9.2)
                );

        verify(carService).getAvailableCars(
                startAt,
                endAt
        );
    }

    @Test
    void shouldReturnBadRequestWhenAvailabilityPeriodIsInvalid()
            throws Exception {

        LocalDateTime startAt =
                LocalDateTime.of(2026, 11, 20, 10, 0);
        LocalDateTime endAt = startAt;

        when(carService.getAvailableCars(startAt, endAt))
                .thenThrow(new InvalidReservationPeriodException(
                        "End date and time must be after start date and time"
                ));

        mockMvc.perform(
                        get("/api/cars/available")
                                .param(
                                        "startAt",
                                        startAt.toString()
                                )
                                .param(
                                        "endAt",
                                        endAt.toString()
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(
                        "End date and time must be after start date and time"
                ))
                .andExpect(jsonPath("$.path").value(
                        "/api/cars/available"
                ));

        verify(carService).getAvailableCars(
                startAt,
                endAt
        );
    }

    @Test
    void shouldReturnBadRequestWhenEndDateIsMissing()
            throws Exception {

        mockMvc.perform(
                        get("/api/cars/available")
                                .param(
                                        "startAt",
                                        "2026-11-20T10:00"
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnBadRequestWhenDateFormatIsInvalid()
            throws Exception {

        mockMvc.perform(
                        get("/api/cars/available")
                                .param(
                                        "startAt",
                                        "20-11-2026 10:00"
                                )
                                .param(
                                        "endAt",
                                        "2026-11-22T13:00"
                                )
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnHalfHourAvailabilitySlots() throws Exception {
        Long carId = 1L;
        LocalDate date = LocalDate.of(2026, 12, 3);

        when(carService.getAvailabilitySlots(carId, date))
                .thenReturn(List.of(
                        new CarAvailabilitySlotResponse("16:30", false),
                        new CarAvailabilitySlotResponse("17:00", true)
                ));

        mockMvc.perform(
                        get(
                                "/api/cars/{id}/availability-slots",
                                carId
                        )
                                .param("date", date.toString())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0].time").value("16:30"))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[1].time").value("17:00"))
                .andExpect(jsonPath("$[1].available").value(true));

        verify(carService).getAvailabilitySlots(carId, date);
    }

    @Test
    void shouldReturnBadRequestWhenTechnicalDataIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/cars")
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "brand": "Toyota",
                                  "model": "Corolla",
                                  "productionYear": 2024,
                                  "registrationNumber": "BI12345",
                                  "vin": "JTDBR32E720123456",
                                  "fuelType": "GASOLINE",
                                  "transmissionType": "AUTOMATIC",
                                  "bodyType": "SEDAN",
                                  "numberOfSeats": 5,
                                  "category": "STANDARD",
                                  "color": "Gray",
                                  "dailyPrice": 199.99,
                                  "description": "Samochód bez danych technicznych"
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnBadRequestWhenUpdatingCarWithoutTechnicalData()
            throws Exception {

        Long carId = 1L;

        mockMvc.perform(
                        put("/api/cars/{id}", carId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "brand": "Toyota",
                                  "model": "Corolla",
                                  "productionYear": 2024,
                                  "registrationNumber": "BI12345",
                                  "vin": "JTDBR32E720123456",
                                  "fuelType": "GASOLINE",
                                  "transmissionType": "AUTOMATIC",
                                  "bodyType": "SEDAN",
                                  "numberOfSeats": 5,
                                  "category": "STANDARD",
                                  "color": "Gray",
                                  "dailyPrice": 199.99,
                                  "description": "Samochód bez danych technicznych"
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldUpdateCarAndReturnOkStatus() throws Exception {
        Long carId = 1L;
        CarResponse carResponse = createTestCarResponse();

        when(carService.updateCar(
                eq(carId),
                any(UpdateCarRequest.class)
        )).thenReturn(carResponse);

        mockMvc.perform(
                        put("/api/cars/{id}", carId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_ADMIN")
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateCarJson())
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.engineCapacityCm3").value(1798))
                .andExpect(jsonPath("$.powerHp").value(140))
                .andExpect(jsonPath("$.driveType").value("FWD"))
                .andExpect(
                        jsonPath("$.accelerationTo100Seconds")
                                .value(9.2)
                );

        ArgumentCaptor<UpdateCarRequest> requestCaptor =
                ArgumentCaptor.forClass(UpdateCarRequest.class);

        verify(carService).updateCar(
                eq(carId),
                requestCaptor.capture()
        );

        UpdateCarRequest capturedRequest =
                requestCaptor.getValue();

        assertEquals(1798, capturedRequest.engineCapacityCm3());
        assertEquals(140, capturedRequest.powerHp());
        assertEquals(
                CarDriveType.FWD,
                capturedRequest.driveType()
        );
        assertEquals(
                new BigDecimal("9.20"),
                capturedRequest.accelerationTo100Seconds()
        );
    }

}
