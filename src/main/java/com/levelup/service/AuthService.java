package com.levelup.service;

import com.levelup.dto.ApiDtos.UserResponse;
import com.levelup.dto.AuthDtos.LoginRequest;
import com.levelup.dto.AuthDtos.LoginResponse;
import com.levelup.dto.AuthDtos.SignupRequest;
import com.levelup.entity.User;
import com.levelup.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long tokenExpirationSeconds;
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${app.auth.token-expiration-seconds:900}") long tokenExpirationSeconds) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("constant-time-login-check");
        if (tokenExpirationSeconds <= 0) throw new IllegalStateException("Token expiration must be positive");
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }

    public void signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        validatePasswordLength(request.password());
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Email is already registered");
        String passwordHash = passwordEncoder.encode(request.password());
        users.save(new User(request.name().trim(), email, passwordHash));
    }

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = users.findByEmail(email).orElse(null);
        boolean passwordLengthValid = request.password().getBytes(StandardCharsets.UTF_8).length <= 72;
        if (user == null || user.getPasswordHash() == null || !passwordLengthValid) {
            passwordEncoder.matches(passwordLengthValid ? request.password() : "invalid", dummyPasswordHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(tokenExpirationSeconds))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(UserResponse.from(user), token);
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Invalid email or password");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes");
        }
    }
}
