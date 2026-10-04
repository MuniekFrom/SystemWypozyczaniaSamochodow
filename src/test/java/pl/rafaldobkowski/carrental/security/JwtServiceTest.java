package pl.rafaldobkowski.carrental.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import pl.rafaldobkowski.carrental.user.model.User;
import pl.rafaldobkowski.carrental.user.model.UserRole;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    @Test
    void shouldGenerateTokenWithExpectedClaims() {
        byte[] keyBytes =
                "01234567890123456789012345678901"
                        .getBytes(StandardCharsets.UTF_8);

        SecretKey secretKey = new SecretKeySpec(
                keyBytes,
                "HmacSHA256"
        );

        JwtEncoder jwtEncoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        long expirationSeconds = 3600;

        JwtService jwtService = new JwtService(
                jwtEncoder,
                expirationSeconds
        );

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(user.getEmail())
                .thenReturn("anna.nowak@example.com");
        when(user.getRole()).thenReturn(UserRole.CLIENT);

        String token = jwtService.generateToken(user);

        Jwt decodedToken = jwtDecoder.decode(token);

        assertNotNull(token);
        assertEquals("1", decodedToken.getSubject());
        assertEquals(
                "anna.nowak@example.com",
                decodedToken.getClaimAsString("email")
        );
        assertEquals(
                "CLIENT",
                decodedToken.getClaimAsString("role")
        );
        assertNotNull(decodedToken.getIssuedAt());
        assertNotNull(decodedToken.getExpiresAt());

        long tokenLifetime = Duration.between(
                decodedToken.getIssuedAt(),
                decodedToken.getExpiresAt()
        ).getSeconds();

        assertEquals(expirationSeconds, tokenLifetime);
    }
}