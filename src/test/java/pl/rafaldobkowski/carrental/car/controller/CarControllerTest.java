package pl.rafaldobkowski.carrental.car.controller;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.rafaldobkowski.carrental.car.dto.CarResponse;
import pl.rafaldobkowski.carrental.car.dto.CreateCarRequest;
import pl.rafaldobkowski.carrental.car.exception.CarAlreadyExistsException;
import pl.rafaldobkowski.carrental.car.exception.CarNotFoundException;
import pl.rafaldobkowski.carrental.car.model.*;
import pl.rafaldobkowski.carrental.car.service.CarService;
import pl.rafaldobkowski.carrental.security.SecurityConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;


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
                "Samochód testowy"
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
              "description": "Samochód testowy"
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

        mockMvc.perform(get("/api/cars/{id}", carId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Corolla"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(carService).getCarById(carId);
    }

    @Test
    void shouldCreateCarAndReturnCreatedStatus() throws Exception{
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
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ArgumentCaptor<CreateCarRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateCarRequest.class);

        verify(carService).createCar(requestCaptor.capture());

        CreateCarRequest capturedRequest = requestCaptor.getValue();

        assertEquals("Toyota", capturedRequest.brand());
        assertEquals("BI12345", capturedRequest.registrationNumber());
        assertEquals(
                new BigDecimal("199.99"),
                capturedRequest.dailyPrice()
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
    void shouldReturnUnauthorizedWhenCreatingCarWithoutToken() throws Exception {
        mockMvc.perform(
                        post("/api/cars")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateCarJson())
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(carService);
    }

    @Test
    void shouldReturnForbiddenWhenClientCreatesCar() throws Exception {
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
}