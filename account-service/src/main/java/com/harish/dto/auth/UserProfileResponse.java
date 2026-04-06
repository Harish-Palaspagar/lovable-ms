package com.harish.dto.auth;

public record UserProfileResponse(
        Long id,
        String username,
        String name
) {
}
