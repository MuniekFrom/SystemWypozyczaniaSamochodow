package pl.rafaldobkowski.carrental.user.controller;

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
import pl.rafaldobkowski.carrental.security.SecurityConfig;
import pl.rafaldobkowski.carrental.user.dto.CreateUserRequest;
import pl.rafaldobkowski.carrental.user.dto.UserResponse;
import pl.rafaldobkowski.carrental.user.exception.UserAlreadyExistsException;
import pl.rafaldobkowski.carrental.user.exception.UserNotFoundException;
import pl.rafaldobkowski.carrental.user.model.UserRole;
import pl.rafaldobkowski.carrental.user.model.UserStatus;
import pl.rafaldobkowski.carrental.user.service.UserService;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldCreateUserAndReturnCreatedStatus() throws Exception {
        UserResponse response = new UserResponse(
                1L,
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "+48987654321",
                UserRole.CLIENT,
                UserStatus.ACTIVE,
                Instant.parse("2026-10-01T10:00:00Z")
        );

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Anna",
                                          "lastName": "Nowak",
                                          "email": "anna.nowak@example.com",
                                          "password": "Haslo123!",
                                          "phoneNumber": "+48987654321"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.email")
                        .value("anna.nowak@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        ArgumentCaptor<CreateUserRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateUserRequest.class);

        verify(userService).createUser(requestCaptor.capture());

        CreateUserRequest capturedRequest = requestCaptor.getValue();

        assertEquals("Anna", capturedRequest.firstName());
        assertEquals(
                "anna.nowak@example.com",
                capturedRequest.email()
        );
        assertEquals("Haslo123!", capturedRequest.password());
    }


    @Test
    void shouldReturnBadRequestWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Anna",
                                          "lastName": "Nowak",
                                          "email": "anna.nowak@gmail.com",
                                          "password": "123",
                                          "phoneNumber": "+48123456789"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/users"));

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        String message = "User with this email already exists";

        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new UserAlreadyExistsException(message));

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "firstName": "Anna",
                                          "lastName": "Nowak",
                                          "email": "anna.nowak@example.com",
                                          "password": "Haslo123!",
                                          "phoneNumber": "+48123456789"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("/api/users"));

        verify(userService)
                .createUser(any(CreateUserRequest.class));
    }

    @Test
    void shouldBlockUserWhenAdminIsAuthenticated() throws Exception {
        Long userId = 1L;

        UserResponse response = new UserResponse(
                userId,
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "+48123456789",
                UserRole.CLIENT,
                UserStatus.BLOCKED,
                Instant.parse("2026-10-01T10:00:00Z")
        );

        when(userService.blockUser(userId))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/users/{id}/block", userId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email")
                        .value("anna.nowak@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).blockUser(userId);
    }

    @Test
    void shouldActivateUserWhenAdminIsAuthenticated() throws Exception {
        Long userId = 1L;

        UserResponse response = new UserResponse(
                userId,
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "+48123456789",
                UserRole.CLIENT,
                UserStatus.ACTIVE,
                Instant.parse("2026-10-01T10:00:00Z")
        );

        when(userService.activateUser(userId))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/users/{id}/activate", userId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        )
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email")
                        .value("anna.nowak@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).activateUser(userId);
    }


    @Test
    void shouldReturnForbiddenWhenClientTriesToBlockUser() throws Exception {
        Long userId = 1L;

        mockMvc.perform(
                        patch("/api/users/{id}/block", userId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_CLIENT"
                                        )
                                ))
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }


    @Test
    void shouldReturnUnauthorizedWhenBlockingUserWithoutToken()
            throws Exception {

        Long userId = 1L;

        mockMvc.perform(
                        patch("/api/users/{id}/block", userId)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnNotFoundWhenBlockingMissingUser() throws Exception {
        Long userId = 99L;
        String message = "User with id 99 was not found";

        when(userService.blockUser(userId))
                .thenThrow(new UserNotFoundException(message));

        mockMvc.perform(
                        patch("/api/users/{id}/block", userId)
                                .with(jwt().authorities(
                                        new SimpleGrantedAuthority(
                                                "ROLE_ADMIN"
                                        )
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path")
                        .value("/api/users/99/block"));

        verify(userService).blockUser(userId);
    }

}