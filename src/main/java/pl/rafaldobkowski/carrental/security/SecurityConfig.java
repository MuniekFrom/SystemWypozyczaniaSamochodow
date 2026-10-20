package pl.rafaldobkowski.carrental.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Publiczne endpointy
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/cars",
                                "/api/cars/**"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()


                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users",
                                "/api/users/*"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/users/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/cars"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/cars/**"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/cars/**"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        // Operacje klienta
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reservations"
                        ).hasRole("CLIENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reservations/me"
                        ).hasRole("CLIENT")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/cancel"
                        ).hasRole("CLIENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reservations"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/confirm"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/complete"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/start"
                        ).hasAnyRole("ADMIN", "EMPLOYEE")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter((jwtAuthenticationConverter)))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
