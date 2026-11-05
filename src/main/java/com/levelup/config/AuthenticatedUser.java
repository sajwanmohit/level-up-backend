package com.levelup.config;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AuthenticatedUser {
    private AuthenticatedUser() {}

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public static void requireMatch(UUID requestedId, Jwt jwt) {
        UUID authenticatedId = id(jwt);
        if (!authenticatedId.equals(requestedId)) throw new AccessDeniedException("User does not own this resource");
    }
}
