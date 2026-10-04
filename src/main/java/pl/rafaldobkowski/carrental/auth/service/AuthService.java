package pl.rafaldobkowski.carrental.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.rafaldobkowski.carrental.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import pl.rafaldobkowski.carrental.auth.dto.LoginRequest;
import pl.rafaldobkowski.carrental.auth.exception.InvalidCredentialsException;
import pl.rafaldobkowski.carrental.user.model.User;

import pl.rafaldobkowski.carrental.auth.dto.LoginResponse;
import pl.rafaldobkowski.carrental.security.JwtService;


@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public User authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"
                ));

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        return user;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = authenticate(request);

        String accessToken = jwtService.generateToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }

}
