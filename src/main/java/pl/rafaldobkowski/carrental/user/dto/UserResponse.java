package pl.rafaldobkowski.carrental.user.dto;

import pl.rafaldobkowski.carrental.user.model.UserRole;
import pl.rafaldobkowski.carrental.user.model.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UserRole role,
        UserStatus status,
        Instant createdAt
) {
}
