package pl.rafaldobkowski.carrental.user.dto;

import jakarta.validation.constraints.NotNull;
import pl.rafaldobkowski.carrental.user.model.UserRole;

public record UpdateUserRoleRequest(

        @NotNull
        UserRole role

) {
}