package com.lingfeng.sprite.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * S25-1: API Authentication and Authorization Service
 *
 * Provides comprehensive API security:
 * - API key authentication
 * - JWT token validation
 * - Role-based access control (RBAC)
 * - Rate limiting per API key
 * - Authentication attempt tracking
 */
@Service
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    // ==================== Configuration ====================

    /** Default rate limit: 100 requests per minute */
    private static final int DEFAULT_RATE_LIMIT = 100;

    /** Default rate limit window in seconds */
    private static final int DEFAULT_RATE_WINDOW_SECONDS = 60;

    /** Maximum failed authentication attempts before temporary ban */
    private static final int MAX_AUTH_ATTEMPTS = 5;

    /** Ban duration in seconds after max failed attempts */
    private static final long BAN_DURATION_SECONDS = 300;

    /** JWT secret key (in production, load from secure config) */
    private static final String JWT_SECRET = "sprite-digital-being-secret-key-change-in-production";

    /** JWT issuer */
    private static final String JWT_ISSUER = "sprite-digital-being";

    // ==================== Data Structures ====================

    /**
     * Authentication result record
     */
    public record AuthResult(
        boolean success,
        String userId,
        Set<Role> roles,
        Instant expiresAt,
        String errorMessage
    ) {
        public AuthResult(boolean success, String userId, Set<Role> roles, Instant expiresAt) {
            this(success, userId, roles, expiresAt, null);
        }

        public static AuthResult success(String userId, Set<Role> roles, Instant expiresAt) {
            return new AuthResult(true, userId, roles, expiresAt, null);
        }

        public static AuthResult failure(String errorMessage) {
            return new AuthResult(false, null, Set.of(), null, errorMessage);
        }
    }

    /**
     * Rate limit check result
     */
    public record RateLimitResult(
        boolean allowed,
        int remaining,
        Instant resetAt,
        long retryAfterMs
    ) {
        public static RateLimitResult allowed(int remaining, Instant resetAt) {
            return new RateLimitResult(true, remaining, resetAt, 0);
        }

        public static RateLimitResult denied(int remaining, Instant resetAt, long retryAfterMs) {
            return new RateLimitResult(false, remaining, resetAt, retryAfterMs);
        }
    }

    /**
     * Authentication attempt record
     */
    public record AuthAttempt(
        String apiKey,
        Instant timestamp,
        boolean success,
        String ipAddress,
        String userAgent
    ) {}

    /**
     * Role enumeration for RBAC
     */
    public enum Role {
        ADMIN,   // Full access
        USER,    // Standard user access
        DEVICE,  // IoT device access
        API      // API programmatic access
    }

    /**
     * API Key information
     */
    public record ApiKeyInfo(
        String apiKeyHash,
        String userId,
        Set<Role> roles,
        Instant createdAt,
        Instant expiresAt,
        boolean enabled,
        int rateLimit,
        int rateWindowSeconds,
        Map<String, Instant> resourcePermissions
    ) {
        public boolean hasRole(Role role) {
            return roles.contains(role);
        }

        public boolean hasPermission(String resource) {
            // ADMIN has all permissions
            if (roles.contains(Role.ADMIN)) {
                return true;
            }
            // Check specific resource permission
            return resourcePermissions != null && resourcePermissions.containsKey(resource);
        }
    }

    /**
     * Authentication statistics
     */
    public record AuthStats(
        long totalAttempts,
        long successfulAttempts,
        long failedAttempts,
        long blockedAttempts,
        Map<String, Long> attemptsByApiKey
    ) {}

    // ==================== State ====================

    /** API Key registry: keyHash -> ApiKeyInfo */
    private final Map<String, ApiKeyInfo> apiKeyRegistry = new ConcurrentHashMap<>();

    /** Rate limit tracking: keyHash -> RateLimitBucket */
    private final Map<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    /** Authentication attempts log */
    private final List<AuthAttempt> authAttempts = new ArrayList<>();

    /** Blocked API keys: keyHash -> unblock time */
    private final Map<String, Instant> blockedKeys = new ConcurrentHashMap<>();

    /** Failed attempts counter: keyHash -> count */
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    /** Maximum attempts to keep in log */
    private static final int MAX_AUTH_LOG_SIZE = 10000;

    /** ObjectMapper for JWT JSON handling */
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Rate Limit Bucket ====================

    /**
     * Token bucket for rate limiting
     */
    private static class RateLimitBucket {
        final AtomicInteger tokens;
        final int maxTokens;
        final AtomicLong lastRefillTime;
        final int refillRate; // tokens per second

        RateLimitBucket(int maxTokens, int refillRate) {
            this.maxTokens = maxTokens;
            this.tokens = new AtomicInteger(maxTokens);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.refillRate = refillRate;
        }

        synchronized RateLimitResult consume(int tokensToConsume) {
            refill();

            int current = tokens.get();
            Instant now = Instant.now();

            if (current >= tokensToConsume) {
                tokens.addAndGet(-tokensToConsume);
                return RateLimitResult.allowed(current - tokensToConsume, now.plusSeconds(DEFAULT_RATE_WINDOW_SECONDS));
            } else {
                long retryAfterMs = calculateRetryAfter();
                return RateLimitResult.denied(current, now.plusSeconds(DEFAULT_RATE_WINDOW_SECONDS), retryAfterMs);
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime.get();

            if (elapsed >= 1000) { // At least 1 second passed
                int tokensToAdd = (int) (elapsed / 1000 * refillRate);
                if (tokensToAdd > 0) {
                    int newValue = Math.min(maxTokens, tokens.addAndGet(tokensToAdd));
                    if (newValue < maxTokens) {
                        lastRefillTime.set(now);
                    }
                }
            }
        }

        private long calculateRetryAfter() {
            int needed = 1; // For single request
            int current = tokens.get();
            if (current >= needed) return 0;

            // Time to get enough tokens (in milliseconds)
            return (long) Math.ceil((needed - current) * 1000.0 / refillRate);
        }
    }

    // ==================== Constructor ====================

    public SecurityService() {
        // Initialize with default API keys for testing
        initializeDefaultApiKeys();
        logger.info("SecurityService initialized with {} registered API keys", apiKeyRegistry.size());
    }

    /**
     * Initialize default API keys for development/testing
     */
    private void initializeDefaultApiKeys() {
        // Admin API key (hashed)
        String adminKey = "sk-admin-test-key-12345";
        registerApiKey(adminKey, "admin-user", Set.of(Role.ADMIN, Role.USER), 1000, 60);

        // User API key
        String userKey = "sk-user-test-key-67890";
        registerApiKey(userKey, "regular-user", Set.of(Role.USER), 100, 60);

        // Device API key
        String deviceKey = "sk-device-test-key-abcde";
        registerApiKey(deviceKey, "device-001", Set.of(Role.DEVICE), 500, 60);

        // API key for programmatic access
        String apiKey = "sk-api-test-key-fghij";
        registerApiKey(apiKey, "api-client-001", Set.of(Role.API), 200, 60);
    }

    // ==================== API Key Management ====================

    /**
     * Register a new API key
     */
    public ApiKeyInfo registerApiKey(String apiKey, String userId, Set<Role> roles) {
        return registerApiKey(apiKey, userId, roles, DEFAULT_RATE_LIMIT, DEFAULT_RATE_WINDOW_SECONDS);
    }

    /**
     * Register a new API key with custom rate limiting
     */
    public ApiKeyInfo registerApiKey(String apiKey, String userId, Set<Role> roles, int rateLimit, int rateWindowSeconds) {
        String keyHash = hashApiKey(apiKey);

        ApiKeyInfo info = new ApiKeyInfo(
            keyHash,
            userId,
            roles != null ? EnumSet.copyOf(roles) : EnumSet.noneOf(Role.class),
            Instant.now(),
            null, // No expiration
            true,
            rateLimit,
            rateWindowSeconds,
            new ConcurrentHashMap<>()
        );

        apiKeyRegistry.put(keyHash, info);

        // Initialize rate limit bucket
        rateLimitBuckets.put(keyHash, new RateLimitBucket(rateLimit, rateLimit / rateWindowSeconds));

        logger.info("Registered API key for user: {} with roles: {}", userId, roles);
        return info;
    }

    /**
     * Revoke an API key
     */
    public boolean revokeApiKey(String apiKey) {
        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo removed = apiKeyRegistry.remove(keyHash);
        rateLimitBuckets.remove(keyHash);

        if (removed != null) {
            logger.info("Revoked API key for user: {}", removed.userId());
            return true;
        }
        return false;
    }

    /**
     * Get API key info
     */
    public ApiKeyInfo getApiKeyInfo(String apiKey) {
        String keyHash = hashApiKey(apiKey);
        return apiKeyRegistry.get(keyHash);
    }

    /**
     * Check if API key is registered
     */
    public boolean isApiKeyRegistered(String apiKey) {
        String keyHash = hashApiKey(apiKey);
        return apiKeyRegistry.containsKey(keyHash);
    }

    // ==================== S25-1: API Key Authentication ====================

    /**
     * Validate API key and return authentication result
     */
    public AuthResult validateApiKey(String apiKey) {
        return validateApiKey(apiKey, null, null);
    }

    /**
     * Validate API key with request metadata
     */
    public AuthResult validateApiKey(String apiKey, String ipAddress, String userAgent) {
        if (apiKey == null || apiKey.isBlank()) {
            recordAuthAttempt(null, false, ipAddress, userAgent);
            return AuthResult.failure("API key is required");
        }

        String keyHash = hashApiKey(apiKey);

        // Check if blocked
        Instant blockedUntil = blockedKeys.get(keyHash);
        if (blockedUntil != null && Instant.now().isBefore(blockedUntil)) {
            recordAuthAttempt(keyHash, false, ipAddress, userAgent);
            return AuthResult.failure("API key is temporarily blocked due to too many failed attempts");
        } else {
            // Unblock if duration has passed
            blockedKeys.remove(keyHash);
        }

        // Find API key info
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);
        if (info == null) {
            incrementFailedAttempts(keyHash);
            recordAuthAttempt(keyHash, false, ipAddress, userAgent);
            return AuthResult.failure("Invalid API key");
        }

        // Check if enabled
        if (!info.enabled()) {
            recordAuthAttempt(keyHash, false, ipAddress, userAgent);
            return AuthResult.failure("API key is disabled");
        }

        // Check expiration
        if (info.expiresAt() != null && Instant.now().isAfter(info.expiresAt())) {
            recordAuthAttempt(keyHash, false, ipAddress, userAgent);
            return AuthResult.failure("API key has expired");
        }

        // Success
        resetFailedAttempts(keyHash);
        recordAuthAttempt(keyHash, true, ipAddress, userAgent);
        logger.debug("API key validated for user: {}", info.userId());

        return AuthResult.success(info.userId(), info.roles(), info.expiresAt());
    }

    // ==================== S25-1: JWT Token Validation ====================

    /**
     * Validate JWT token and return authentication result
     */
    public AuthResult validateToken(String token) {
        return validateToken(token, null, null);
    }

    /**
     * Validate JWT token with request metadata
     */
    public AuthResult validateToken(String token, String ipAddress, String userAgent) {
        if (token == null || token.isBlank()) {
            recordAuthAttempt(null, false, ipAddress, userAgent);
            return AuthResult.failure("Token is required");
        }

        try {
            // Parse JWT (simple implementation - in production use a proper JWT library)
            JwtPayload payload = parseJwt(token);

            if (payload == null) {
                incrementFailedAttempts("jwt-" + ipAddress);
                recordAuthAttempt(null, false, ipAddress, userAgent);
                return AuthResult.failure("Invalid token format");
            }

            // Check expiration
            if (payload.exp() != null && Instant.now().isAfter(Instant.ofEpochSecond(payload.exp()))) {
                recordAuthAttempt(null, false, ipAddress, userAgent);
                return AuthResult.failure("Token has expired");
            }

            // Check issuer
            if (!JWT_ISSUER.equals(payload.iss())) {
                recordAuthAttempt(null, false, ipAddress, userAgent);
                return AuthResult.failure("Invalid token issuer");
            }

            // Parse roles
            Set<Role> roles = new HashSet<>();
            if (payload.roles() != null) {
                for (String roleStr : payload.roles()) {
                    try {
                        roles.add(Role.valueOf(roleStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Unknown role in token: {}", roleStr);
                    }
                }
            }

            recordAuthAttempt(payload.sub(), true, ipAddress, userAgent);
            logger.debug("JWT token validated for user: {}", payload.sub());

            Instant expiresAt = payload.exp() != null ? Instant.ofEpochSecond(payload.exp()) : null;
            return AuthResult.success(payload.sub(), roles, expiresAt);

        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
            recordAuthAttempt(null, false, ipAddress, userAgent);
            return AuthResult.failure("Token validation failed: " + e.getMessage());
        }
    }

    /**
     * JWT payload structure
     */
    private record JwtPayload(
        String sub,    // Subject (user ID)
        String iss,    // Issuer
        long iat,      // Issued at
        Long exp,      // Expiration
        String[] roles,
        Map<String, Object> customClaims
    ) {}

    /**
     * Parse JWT token (simple implementation)
     * In production, use a proper JWT library like jjwt or java-jwt
     */
    private JwtPayload parseJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // Decode payload (base64url)
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(payloadJson, Map.class);

            String sub = (String) payloadMap.get("sub");
            String iss = (String) payloadMap.get("iss");
            long iat = payloadMap.get("iat") != null ? ((Number) payloadMap.get("iat")).longValue() : 0;
            Long exp = payloadMap.get("exp") != null ? ((Number) payloadMap.get("exp")).longValue() : null;

            @SuppressWarnings("unchecked")
            String[] roles = payloadMap.get("roles") != null
                ? ((List<String>) payloadMap.get("roles")).toArray(new String[0])
                : null;

            @SuppressWarnings("unchecked")
            Map<String, Object> customClaims = new HashMap<>(payloadMap);
            customClaims.remove("sub");
            customClaims.remove("iss");
            customClaims.remove("iat");
            customClaims.remove("exp");
            customClaims.remove("roles");

            return new JwtPayload(sub, iss, iat, exp, roles, customClaims);

        } catch (Exception e) {
            logger.error("Failed to parse JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a JWT token (for testing purposes)
     */
    public String generateToken(String userId, Set<Role> roles, long expirationSeconds) {
        try {
            long now = System.currentTimeMillis() / 1000;
            long exp = now + expirationSeconds;

            String rolesJson = objectMapper.writeValueAsString(
                roles.stream().map(Role::name).toList()
            );

            String payloadJson = String.format(
                "{\"sub\":\"%s\",\"iss\":\"%s\",\"iat\":%d,\"exp\":%d,\"roles\":%s}",
                userId, JWT_ISSUER, now, exp, rolesJson
            );

            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            // Simple signature (in production use HMAC)
            String signatureInput = "header." + payloadBase64;
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest((signatureInput + JWT_SECRET).getBytes(StandardCharsets.UTF_8)));

            return "header." + payloadBase64 + "." + signature;

        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            logger.error("Failed to generate JWT: {}", e.getMessage());
            return null;
        }
    }

    // ==================== S25-1: Permission Checking ====================

    /**
     * Check if API key has permission to access a resource
     */
    public boolean hasPermission(String apiKey, String resource) {
        if (apiKey == null || resource == null) {
            return false;
        }

        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);

        if (info == null) {
            return false;
        }

        return info.hasPermission(resource);
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String apiKey, Role role) {
        if (apiKey == null || role == null) {
            return false;
        }

        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);

        if (info == null) {
            return false;
        }

        return info.hasRole(role);
    }

    /**
     * Grant permission to API key for a specific resource
     */
    public boolean grantPermission(String apiKey, String resource) {
        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);

        if (info == null) {
            return false;
        }

        Map<String, Instant> updatedPermissions = new ConcurrentHashMap<>(info.resourcePermissions());
        updatedPermissions.put(resource, null); // null = no expiration

        ApiKeyInfo updatedInfo = new ApiKeyInfo(
            info.apiKeyHash(),
            info.userId(),
            info.roles(),
            info.createdAt(),
            info.expiresAt(),
            info.enabled(),
            info.rateLimit(),
            info.rateWindowSeconds(),
            updatedPermissions
        );

        apiKeyRegistry.put(keyHash, updatedInfo);
        logger.info("Granted permission to {} for resource: {}", info.userId(), resource);
        return true;
    }

    /**
     * Revoke permission from API key for a specific resource
     */
    public boolean revokePermission(String apiKey, String resource) {
        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);

        if (info == null) {
            return false;
        }

        Map<String, Instant> updatedPermissions = new ConcurrentHashMap<>(info.resourcePermissions());
        updatedPermissions.remove(resource);

        ApiKeyInfo updatedInfo = new ApiKeyInfo(
            info.apiKeyHash(),
            info.userId(),
            info.roles(),
            info.createdAt(),
            info.expiresAt(),
            info.enabled(),
            info.rateLimit(),
            info.rateWindowSeconds(),
            updatedPermissions
        );

        apiKeyRegistry.put(keyHash, updatedInfo);
        logger.info("Revoked permission from {} for resource: {}", info.userId(), resource);
        return true;
    }

    // ==================== S25-1: Rate Limiting ====================

    /**
     * Check rate limit for API key
     */
    public RateLimitResult checkRateLimit(String apiKey) {
        return checkRateLimit(apiKey, 1);
    }

    /**
     * Check rate limit for API key with custom request cost
     */
    public RateLimitResult checkRateLimit(String apiKey, int requestCost) {
        if (apiKey == null) {
            return RateLimitResult.denied(0, Instant.now(), 1000);
        }

        String keyHash = hashApiKey(apiKey);

        // Check if key exists
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);
        if (info == null) {
            // Use default rate limit for unknown keys
            RateLimitBucket defaultBucket = rateLimitBuckets.get("default");
            if (defaultBucket == null) {
                defaultBucket = new RateLimitBucket(DEFAULT_RATE_LIMIT, DEFAULT_RATE_LIMIT / DEFAULT_RATE_WINDOW_SECONDS);
                rateLimitBuckets.put("default", defaultBucket);
            }
            return defaultBucket.consume(requestCost);
        }

        RateLimitBucket bucket = rateLimitBuckets.get(keyHash);
        if (bucket == null) {
            bucket = new RateLimitBucket(info.rateLimit(), info.rateLimit() / info.rateWindowSeconds());
            rateLimitBuckets.put(keyHash, bucket);
        }

        return bucket.consume(requestCost);
    }

    /**
     * Get remaining rate limit for API key
     */
    public int getRemainingRateLimit(String apiKey) {
        if (apiKey == null) {
            return 0;
        }

        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);

        if (info == null) {
            return 0;
        }

        RateLimitBucket bucket = rateLimitBuckets.get(keyHash);
        if (bucket == null) {
            return info.rateLimit();
        }

        return bucket.tokens.get();
    }

    /**
     * Reset rate limit for API key
     */
    public void resetRateLimit(String apiKey) {
        if (apiKey == null) {
            return;
        }

        String keyHash = hashApiKey(apiKey);
        ApiKeyInfo info = apiKeyRegistry.get(keyHash);

        if (info != null) {
            rateLimitBuckets.put(keyHash, new RateLimitBucket(info.rateLimit(), info.rateLimit() / info.rateWindowSeconds()));
            logger.info("Rate limit reset for user: {}", info.userId());
        }
    }

    // ==================== Authentication Attempt Tracking ====================

    /**
     * Record an authentication attempt
     */
    private void recordAuthAttempt(String apiKey, boolean success, String ipAddress, String userAgent) {
        AuthAttempt attempt = new AuthAttempt(
            apiKey != null ? maskApiKey(apiKey) : null,
            Instant.now(),
            success,
            ipAddress,
            userAgent
        );

        synchronized (authAttempts) {
            authAttempts.add(attempt);

            // Trim if exceeds max size
            if (authAttempts.size() > MAX_AUTH_LOG_SIZE) {
                authAttempts.subList(0, authAttempts.size() - MAX_AUTH_LOG_SIZE).clear();
            }
        }
    }

    /**
     * Increment failed attempts counter
     */
    private void incrementFailedAttempts(String keyHash) {
        AtomicInteger attempts = failedAttempts.computeIfAbsent(keyHash, k -> new AtomicInteger(0));
        int count = attempts.incrementAndGet();

        if (count >= MAX_AUTH_ATTEMPTS) {
            blockedKeys.put(keyHash, Instant.now().plusSeconds(BAN_DURATION_SECONDS));
            logger.warn("API key {} blocked due to {} failed attempts", maskApiKey(keyHash), count);
        }
    }

    /**
     * Reset failed attempts counter
     */
    private void resetFailedAttempts(String keyHash) {
        failedAttempts.remove(keyHash);
    }

    /**
     * Get authentication statistics
     */
    public AuthStats getAuthStats() {
        long total = authAttempts.size();
        long success = authAttempts.stream().filter(AuthAttempt::success).count();
        long failed = authAttempts.stream().filter(a -> !a.success()).count();
        long blocked = blockedKeys.size();

        Map<String, Long> attemptsByApiKey = new HashMap<>();
        for (AuthAttempt attempt : authAttempts) {
            if (attempt.apiKey() != null) {
                attemptsByApiKey.merge(attempt.apiKey(), 1L, Long::sum);
            }
        }

        return new AuthStats(total, success, failed, blocked, attemptsByApiKey);
    }

    /**
     * Get recent authentication attempts
     */
    public List<AuthAttempt> getRecentAuthAttempts(int limit) {
        synchronized (authAttempts) {
            int size = authAttempts.size();
            int from = Math.max(0, size - limit);
            return new ArrayList<>(authAttempts.subList(from, size));
        }
    }

    /**
     * Get blocked API keys
     */
    public Map<String, Instant> getBlockedApiKeys() {
        return new HashMap<>(blockedKeys);
    }

    /**
     * Unblock an API key manually
     */
    public boolean unblockApiKey(String apiKey) {
        String keyHash = hashApiKey(apiKey);
        Instant removed = blockedKeys.remove(keyHash);
        if (removed != null) {
            logger.info("API key manually unblocked: {}", maskApiKey(keyHash));
            return true;
        }
        return false;
    }

    // ==================== Utility Methods ====================

    /**
     * Hash API key using SHA-256
     */
    private String hashApiKey(String apiKey) {
        if (apiKey == null) {
            return "null";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Mask API key for logging
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    // ==================== Cleanup ====================

    /**
     * Clean up expired blocked keys
     */
    public void cleanupExpiredBlocks() {
        Instant now = Instant.now();
        blockedKeys.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }

    /**
     * Get registered API key count
     */
    public int getRegisteredKeyCount() {
        return apiKeyRegistry.size();
    }
}
