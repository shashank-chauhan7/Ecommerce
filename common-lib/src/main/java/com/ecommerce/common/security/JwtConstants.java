package com.ecommerce.common.security;

public final class JwtConstants {

    private JwtConstants() {}

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";

    public static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000L;       // 15 minutes
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days
}
