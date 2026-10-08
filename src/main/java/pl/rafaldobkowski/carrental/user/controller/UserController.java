package pl.rafaldobkowski.carrental.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rafaldobkowski.carrental.user.dto.CreateUserRequest;
import pl.rafaldobkowski.carrental.user.dto.UserResponse;
import pl.rafaldobkowski.carrental.user.service.UserService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request){

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
}
