package com.ventureai.dto.auth;

import com.ventureai.entity.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Invalid email address") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
        @NotBlank(message = "Please confirm your password") String confirmPassword,
        @NotBlank(message = "User type is required") String userType
) {
    public UserType resolvedUserType() {
        try {
            return UserType.valueOf(userType.trim().toUpperCase().replace(" ", "_"));
        } catch (Exception e) {
            return UserType.ENTREPRENEUR;
        }
    }
}
