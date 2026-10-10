package pl.rafaldobkowski.carrental.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pl.rafaldobkowski.carrental.user.dto.CreateUserRequest;
import pl.rafaldobkowski.carrental.user.dto.UserResponse;
import pl.rafaldobkowski.carrental.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {

        UserResponse createdUser = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<UserResponse> blockUser(
            @PathVariable Long id) {

        UserResponse blockedUser = userService.blockUser(id);

        return ResponseEntity.ok(blockedUser);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable Long id) {

        UserResponse activatedUser = userService.activateUser(id);

        return ResponseEntity.ok(activatedUser);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());

        UserResponse user = userService.getUserById(userId);

        return ResponseEntity.ok(user);
    }

}
