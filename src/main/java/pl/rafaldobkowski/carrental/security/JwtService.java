package pl.rafaldobkowski.carrental.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import pl.rafaldobkowski.carrental.user.model.User;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    private final long expirationSeconds;

    public JwtService(JwtEncoder jwtEncoder,@Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

}
