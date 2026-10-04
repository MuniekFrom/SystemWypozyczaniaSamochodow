package pl.rafaldobkowski.carrental.auth.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rafaldobkowski.carrental.auth.dto.LoginRequest;
import pl.rafaldobkowski.carrental.auth.dto.LoginResponse;
import pl.rafaldobkowski.carrental.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){

        LoginResponse loginResponse = authService.login(request);

        return ResponseEntity.ok(loginResponse);
    }
}
