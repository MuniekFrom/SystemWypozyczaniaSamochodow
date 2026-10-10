package pl.rafaldobkowski.carrental.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.rafaldobkowski.carrental.user.dto.CreateUserRequest;
import pl.rafaldobkowski.carrental.user.dto.UserResponse;
import pl.rafaldobkowski.carrental.user.exception.UserAlreadyExistsException;
import pl.rafaldobkowski.carrental.user.exception.UserNotFoundException;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.model.UserRole;
import pl.rafaldobkowski.carrental.user.model.UserStatus;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldThrowUserAlreadyExistsExceptionWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest(
                "Jan",
                "Kowalski",
                "jan.kowalski@example.com",
                "Haslo123!",
                "+48123456789"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        assertEquals(
                "User with this email already exists",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail(request.email());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldCreateUserWhenEmailIsUnique() {
        CreateUserRequest request = new CreateUserRequest(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "Haslo123!",
                "+48987654321"
        );

        String passwordHash = "$2a$10$test-password-hash";

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn(passwordHash);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0, User.class)
                );

        UserResponse response = userService.createUser(request);

        assertEquals("Anna", response.firstName());
        assertEquals("Nowak", response.lastName());
        assertEquals("anna.nowak@example.com", response.email());
        assertEquals("+48987654321", response.phoneNumber());
        assertEquals(UserRole.CLIENT, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertNotNull(response.createdAt());

        verify(userRepository).existsByEmail(request.email());
        verify(passwordEncoder).encode(request.password());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(passwordHash, savedUser.getPasswordHash());
    }

    @Test
    void shouldBlockUserWhenUserExists() {
        Long userId = 1L;

        User user = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.blockUser(userId);

        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertEquals(UserStatus.BLOCKED, response.status());

        verify(userRepository).findById(userId);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldActivateUserWhenUserExists() {
        Long userId = 1L;

        User user = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );

        user.block();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.activateUser(userId);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(UserStatus.ACTIVE, response.status());

        verify(userRepository).findById(userId);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenBlockingMissingUser() {
        Long userId = 99L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.blockUser(userId)
        );

        assertEquals(
                "User with id 99 was not found",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldReturnAllUsers() {
        User firstUser = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "first-password-hash",
                "+48111111111"
        );

        User secondUser = new User(
                "Jan",
                "Kowalski",
                "jan.kowalski@example.com",
                "second-password-hash",
                "+48222222222"
        );

        secondUser.block();

        when(userRepository.findAll())
                .thenReturn(List.of(firstUser, secondUser));

        List<UserResponse> responses = userService.getAllUsers();

        assertEquals(2, responses.size());

        assertEquals("anna.nowak@example.com",
                responses.get(0).email());
        assertEquals(UserStatus.ACTIVE,
                responses.get(0).status());

        assertEquals("jan.kowalski@example.com",
                responses.get(1).email());
        assertEquals(UserStatus.BLOCKED,
                responses.get(1).status());

        verify(userRepository).findAll();
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldReturnUserByIdWhenUserExists() {
        Long userId = 1L;

        User user = new User(
                "Anna",
                "Nowak",
                "anna.nowak@example.com",
                "stored-password-hash",
                "+48123456789"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(userId);

        assertEquals("Anna", response.firstName());
        assertEquals("Nowak", response.lastName());
        assertEquals(
                "anna.nowak@example.com",
                response.email()
        );
        assertEquals(UserRole.CLIENT, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder);
    }

}