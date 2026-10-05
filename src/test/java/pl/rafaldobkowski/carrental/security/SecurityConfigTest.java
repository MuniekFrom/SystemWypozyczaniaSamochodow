package pl.rafaldobkowski.carrental.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void shouldConvertAdminRoleClaimToRoleAdminAuthority() {
        SecurityConfig securityConfig = new SecurityConfig();

        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("1")
                .claim("role", "ADMIN")
                .build();

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        )
        );
    }

    @Test
    void shouldConvertClientRoleClaimToRoleClientAuthority() {
        SecurityConfig securityConfig = new SecurityConfig();

        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("1")
                .claim("role", "CLIENT")
                .build();

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_CLIENT")
                        )
        );
    }
}