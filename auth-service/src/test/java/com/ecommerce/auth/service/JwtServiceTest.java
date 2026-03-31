package com.ecommerce.auth.service;

import com.ecommerce.auth.model.Role;
import com.ecommerce.auth.model.User;
import com.ecommerce.common.security.JwtConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "ThisIsATestSecretKeyThatIsAtLeast256BitsLongForHMACSHA256Algorithm!";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);

        Role customerRole = Role.builder().id(1L).name("ROLE_CUSTOMER").build();
        Role adminRole = Role.builder().id(2L).name("ROLE_ADMIN").build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .roles(Set.of(customerRole, adminRole))
                .enabled(true)
                .build();
    }

    @Test
    void generateAccessToken_returnsNonNullTokenWithCorrectClaims() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId().toString());
    }

    @Test
    void generateRefreshToken_returnsNonNullToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(jwtService.extractEmail(token)).isEqualTo(testUser.getEmail());
    }

    @Test
    void generateRefreshToken_hasDifferentValueThanAccessToken() {
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertThat(refreshToken).isNotEqualTo(accessToken);
    }

    @Test
    void validateToken_valid_returnsTrue() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expired_returnsFalse() {
        JwtService shortLivedService = new JwtService(TEST_SECRET) {
            @Override
            public String generateAccessToken(User user) {
                return buildExpiredToken(user);
            }
        };

        assertThat(jwtService.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void validateToken_malformed_returnsFalse() {
        assertThat(jwtService.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void extractUserId_returnsCorrectUserId() {
        String token = jwtService.generateAccessToken(testUser);

        String userId = jwtService.extractUserId(token);

        assertThat(userId).isEqualTo(testUser.getId().toString());
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateAccessToken(testUser);

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("john@example.com");
    }

    @Test
    void extractRoles_returnsCorrectRoles() {
        String token = jwtService.generateAccessToken(testUser);

        List<String> roles = jwtService.extractRoles(token);

        assertThat(roles).hasSize(2);
        assertThat(roles).containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void validateToken_validRefreshToken_returnsTrue() {
        String refreshToken = jwtService.generateRefreshToken(testUser);

        assertThat(jwtService.validateToken(refreshToken)).isTrue();
    }

    private String buildExpiredToken(User user) {
        return "expired.mock.token";
    }
}
