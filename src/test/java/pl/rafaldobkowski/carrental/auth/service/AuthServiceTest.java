package pl.rafaldobkowski.carrental.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.rafaldobkowski.carrental.auth.dto.LoginRequest;
import pl.rafaldobkowski.carrental.auth.dto.LoginResponse;
import pl.rafaldobkowski.carrental.auth.exception.InvalidCredentialsException;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import pl.rafaldobkowski.carrental.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenUserDoesNotExist() {
        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "Haslo123!"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail(request.email());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsIncorrect() {

        LoginRequest request = new LoginRequest(
                "anna.nowak@example.com",
                "BledneHaslo123!"
        );

        User user = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail(request.email());

        verify(passwordEncoder).matches(
                request.password(),
                user.getPasswordHash()
        );
    }

    @Test
    void shouldReturnUserWhenCredentialsAreCorrect(){

        LoginRequest request = new LoginRequest(
                "anna.nowak@example.com",
                "Haslo123!"
        );

        User user = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )).thenReturn(true);

        User authenticatedUser = authService.authenticate(request);

        assertSame(user, authenticatedUser);

        verify(userRepository)
                .findByEmail(request.email());

        verify(passwordEncoder).matches(
                request.password(),
                user.getPasswordHash()
        );
    }

    @Test
    void shouldReturnLoginResponseWhenCredentialsAreCorrect(){

        LoginRequest request = new LoginRequest(
                "anna.nowak@example.com",
                "Haslo123!"
        );

        User user = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test.jwt.token");

        when(jwtService.getExpirationSeconds())
                .thenReturn(3600L);

        LoginResponse response = authService.login(request);


        assertEquals(
                "test.jwt.token",
                response.accessToken()
        );

        assertEquals(
                "Bearer",
                response.tokenType()
        );

        assertEquals(
                3600L,
                response.expiresIn()
        );

        verify(userRepository)
                .findByEmail(user.getEmail());

        verify(passwordEncoder).matches(
                request.password(),
                user.getPasswordHash()
        );

        verify(jwtService)
                .generateToken(user);

        verify(jwtService)
                .getExpirationSeconds();
    }



}