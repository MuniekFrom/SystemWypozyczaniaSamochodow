package pl.rafaldobkowski.carrental.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;
import pl.rafaldobkowski.carrental.user.exception.UserNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import pl.rafaldobkowski.carrental.user.dto.CreateUserRequest;
import pl.rafaldobkowski.carrental.user.dto.UserResponse;
import pl.rafaldobkowski.carrental.user.exception.UserAlreadyExistsException;
import pl.rafaldobkowski.carrental.user.model.User;



@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "User with this email already exists"
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordHash,
                request.phoneNumber()
        );

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }


    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public UserResponse blockUser(Long id) {
        User user = findUserByIdOrThrow(id);

        user.block();

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = findUserByIdOrThrow(id);

        user.activate();

        return mapToResponse(user);
    }

    private User findUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + id + " was not found"
                ));
    }
}
