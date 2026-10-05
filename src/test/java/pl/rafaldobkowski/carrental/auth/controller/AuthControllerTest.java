package pl.rafaldobkowski.carrental.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.rafaldobkowski.carrental.auth.dto.LoginRequest;
import pl.rafaldobkowski.carrental.auth.dto.LoginResponse;
import pl.rafaldobkowski.carrental.auth.exception.InvalidCredentialsException;
import pl.rafaldobkowski.carrental.auth.service.AuthService;
import pl.rafaldobkowski.carrental.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnTokenWhenCredentialsAreCorrect() throws Exception {
        LoginResponse loginResponse = new LoginResponse(
                "test.jwt.token",
                "Bearer",
                3600L
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(loginResponse);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "anna.nowak@example.com",
                                  "password": "Haslo123!"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.accessToken")
                        .value("test.jwt.token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(3600));

        ArgumentCaptor<LoginRequest> requestCaptor =
                ArgumentCaptor.forClass(LoginRequest.class);

        verify(authService)
                .login(requestCaptor.capture());

        LoginRequest capturedRequest = requestCaptor.getValue();

        assertEquals(
                "anna.nowak@example.com",
                capturedRequest.email()
        );

        assertEquals(
                "Haslo123!",
                capturedRequest.password()
        );
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "anna.nowak@example.com",
                                  "password": "123"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));

        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        String message = "Invalid email or password";

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException(message));

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "anna.nowak@example.com",
                                  "password": "BledneHaslo123!"
                                }
                                """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));

        verify(authService)
                .login(any(LoginRequest.class));
    }

}
