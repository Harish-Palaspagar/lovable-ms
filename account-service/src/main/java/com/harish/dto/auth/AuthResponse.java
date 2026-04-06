package com.harish.dto.auth;

public record AuthResponse(
        String token,
        UserProfileResponse user
) {
}
