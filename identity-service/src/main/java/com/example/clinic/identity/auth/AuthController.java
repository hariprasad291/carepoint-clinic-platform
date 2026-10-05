package com.example.clinic.identity.auth;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final long TOKEN_LIFETIME_SECONDS = 3600;

    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String encodedPassword;

    public AuthController(
            JwtEncoder jwtEncoder,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.username}") String username,
            @Value("${app.auth.password}") String password) {
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.encodedPassword = passwordEncoder.encode(password);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (!username.equals(request.username()) || !passwordEncoder.matches(request.password(), encodedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("clinic-identity-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS))
                .subject(username)
                .claim("scope", "clinic.read clinic.write")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", TOKEN_LIFETIME_SECONDS);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
