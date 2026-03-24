package com.lingfeng.sprite.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * S25-2: Webhook Signature Verification Service
 *
 * Provides secure webhook signature verification and generation:
 * - HMAC-SHA256 signature verification for incoming webhooks
 * - Timestamp validation to prevent replay attacks
 * - Signature generation for outgoing webhooks
 * - Signing key rotation with support for multiple active keys
 * - Integration with HotReloadConfigService for dynamic key management
 */
@Service
public class WebhookSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final long DEFAULT_TIMESTAMP_TOLERANCE_SECONDS = 300; // 5 minutes
    private static final long DEFAULT_KEY_ROTATION_INTERVAL_DAYS = 30;

    private final ObjectMapper jsonMapper;
    private final HotReloadConfigService hotReloadConfigService;
    private final Map<String, SigningKey> signingKeys;
    private final Map<String, KeyMetadata> keyMetadata;
    private final SecureRandom secureRandom;
    private final ScheduledExecutorService scheduler;

    private volatile String currentActiveKeyId;
    private volatile long timestampToleranceSeconds;
    private volatile long keyRotationIntervalDays;

    /**
     * Signing key record
     */
    public record SigningKey(
        String keyId,
        String secret,
        Instant createdAt,
        Instant expiresAt,
        boolean active
    ) {
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Key metadata record
     */
    public record KeyMetadata(
        String keyId,
        String algorithm,
        Instant createdAt,
        Instant lastUsed,
        int useCount
    ) {}

    /**
     * Signature verification result
     */
    public record VerificationResult(
        boolean valid,
        String keyId,
        String error,
        long verificationTimeMs
    ) {}

    /**
     * Signature generation result
     */
    public record SignatureResult(
        String signature,
        String keyId,
        String timestamp,
        long generationTimeMs
    ) {}

    /**
     * Service statistics
     */
    public record SignatureStats(
        int totalKeys,
        String activeKeyId,
        long timestampToleranceSeconds,
        long keyRotationIntervalDays,
        Instant lastRotation
    ) {}

    public WebhookSignatureService() {
        this(null);
    }

    /**
     * Constructor with optional HotReloadConfigService integration
     */
    public WebhookSignatureService(HotReloadConfigService hotReloadConfigService) {
        this.jsonMapper = new ObjectMapper();
        this.hotReloadConfigService = hotReloadConfigService;
        this.signingKeys = new ConcurrentHashMap<>();
        this.keyMetadata = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.timestampToleranceSeconds = DEFAULT_TIMESTAMP_TOLERANCE_SECONDS;
        this.keyRotationIntervalDays = DEFAULT_KEY_ROTATION_INTERVAL_DAYS;

        initializeDefaultKeys();
        startKeyRotationScheduler();
    }

    // ==================== Initialization ====================

    /**
     * Initialize default signing keys if none exist
     */
    private void initializeDefaultKeys() {
        if (signingKeys.isEmpty()) {
            String defaultKeyId = generateKeyId();
            String defaultSecret = generateSecureSecret();
            SigningKey defaultKey = new SigningKey(
                defaultKeyId,
                defaultSecret,
                Instant.now(),
                Instant.now().plusSeconds(keyRotationIntervalDays * 24 * 60 * 60),
                true
            );
            signingKeys.put(defaultKeyId, defaultKey);
            keyMetadata.put(defaultKeyId, new KeyMetadata(defaultKeyId, HMAC_ALGORITHM, Instant.now(), null, 0));
            currentActiveKeyId = defaultKeyId;
            logger.info("Initialized default signing key: {}", defaultKeyId);
        }
    }

    /**
     * Start key rotation scheduler
     */
    private void startKeyRotationScheduler() {
        scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    checkAndRotateExpiredKeys();
                } catch (Exception e) {
                    logger.error("Error during key rotation check: {}", e.getMessage());
                }
            },
            1, // initial delay
            1, // interval
            TimeUnit.HOURS
        );
        logger.info("Started key rotation scheduler");
    }

    // ==================== Signature Verification ====================

    /**
     * S25-2: Verify webhook signature
     *
     * @param payload The raw request payload
     * @param signature The signature to verify (format: sha256=xxx)
     * @param timestamp The request timestamp
     * @return true if signature is valid and timestamp is within tolerance
     */
    public boolean verifySignature(String payload, String signature, String timestamp) {
        VerificationResult result = verifySignatureDetailed(payload, signature, timestamp);
        return result.valid();
    }

    /**
     * S25-2: Verify signature with detailed result
     */
    public VerificationResult verifySignatureDetailed(String payload, String signature, String timestamp) {
        long startTime = System.currentTimeMillis();

        try {
            // Validate inputs
            if (payload == null || signature == null || timestamp == null) {
                return new VerificationResult(false, null, "Missing required parameters", 0);
            }

            // Verify timestamp first to prevent replay attacks
            if (!verifyTimestamp(timestamp)) {
                return new VerificationResult(false, null, "Timestamp outside acceptable range", 0);
            }

            // Extract signature value (remove prefix if present)
            String signatureValue = signature;
            if (signature.startsWith(SIGNATURE_PREFIX)) {
                signatureValue = signature.substring(SIGNATURE_PREFIX.length());
            }

            // Try verification with current active key first
            if (currentActiveKeyId != null) {
                SigningKey activeKey = signingKeys.get(currentActiveKeyId);
                if (activeKey != null && !activeKey.isExpired()) {
                    if (verifyWithKey(payload, timestamp, signatureValue, activeKey)) {
                        updateKeyUsage(currentActiveKeyId);
                        return new VerificationResult(true, currentActiveKeyId, null, System.currentTimeMillis() - startTime);
                    }
                }
            }

            // Try verification with all non-expired keys (for key rotation compatibility)
            for (Map.Entry<String, SigningKey> entry : signingKeys.entrySet()) {
                SigningKey key = entry.getValue();
                if (!key.isExpired() && verifyWithKey(payload, timestamp, signatureValue, key)) {
                    updateKeyUsage(entry.getKey());
                    return new VerificationResult(true, entry.getKey(), null, System.currentTimeMillis() - startTime);
                }
            }

            return new VerificationResult(false, null, "Signature verification failed", System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Error verifying signature: {}", e.getMessage());
            return new VerificationResult(false, null, "Verification error: " + e.getMessage(), 0);
        }
    }

    /**
     * Verify payload with specific signing key
     */
    private boolean verifyWithKey(String payload, String timestamp, String signatureValue, SigningKey key) {
        try {
            String expectedSignature = computeSignature(payload, timestamp, key.secret());
            return constantTimeEquals(expectedSignature, signatureValue);
        } catch (Exception e) {
            logger.error("Error verifying with key {}: {}", key.keyId(), e.getMessage());
            return false;
        }
    }

    /**
     * Compute HMAC-SHA256 signature
     */
    private String computeSignature(String payload, String timestamp, String secret) {
        try {
            String signedPayload = timestamp + "." + payload;
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute signature", e);
        }
    }

    // ==================== Signature Generation ====================

    /**
     * S25-2: Generate signature for outgoing webhook
     *
     * @param payload The payload to sign
     * @param secret The secret key to use
     * @return The generated signature
     */
    public String generateSignature(String payload, String secret) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return generateSignature(payload, secret, timestamp);
    }

    /**
     * Generate signature with specific timestamp
     */
    public String generateSignature(String payload, String secret, String timestamp) {
        SignatureResult result = generateSignatureDetailed(payload, secret, timestamp);
        return SIGNATURE_PREFIX + result.signature();
    }

    /**
     * S25-2: Generate signature with detailed result
     */
    public SignatureResult generateSignatureDetailed(String payload, String secret, String timestamp) {
        long startTime = System.currentTimeMillis();

        try {
            if (payload == null || secret == null) {
                return new SignatureResult(null, null, timestamp, 0);
            }

            String signature = computeSignature(payload, timestamp, secret);
            return new SignatureResult(signature, currentActiveKeyId, timestamp, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Error generating signature: {}", e.getMessage());
            return new SignatureResult(null, null, timestamp, 0);
        }
    }

    /**
     * Generate signature using current active key
     */
    public SignatureResult generateSignatureWithActiveKey(String payload) {
        if (currentActiveKeyId == null) {
            return new SignatureResult(null, null, String.valueOf(Instant.now().getEpochSecond()), 0);
        }

        SigningKey key = signingKeys.get(currentActiveKeyId);
        if (key == null) {
            return new SignatureResult(null, null, String.valueOf(Instant.now().getEpochSecond()), 0);
        }

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = computeSignature(payload, timestamp, key.secret());
        updateKeyUsage(currentActiveKeyId);

        return new SignatureResult(signature, currentActiveKeyId, timestamp, 0);
    }

    // ==================== Timestamp Validation ====================

    /**
     * S25-2: Verify timestamp to prevent replay attacks
     *
     * @param timestamp The timestamp string (Unix epoch seconds)
     * @return true if timestamp is within acceptable tolerance
     */
    public boolean verifyTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) {
                return false;
            }

            long timestampEpoch;
            try {
                timestampEpoch = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                // Try parsing as ISO-8601 string
                try {
                    Instant instant = Instant.parse(timestamp);
                    timestampEpoch = instant.getEpochSecond();
                } catch (Exception ex) {
                    logger.warn("Invalid timestamp format: {}", timestamp);
                    return false;
                }
            }

            long currentEpoch = Instant.now().getEpochSecond();
            long diff = Math.abs(currentEpoch - timestampEpoch);

            return diff <= timestampToleranceSeconds;

        } catch (Exception e) {
            logger.error("Error verifying timestamp: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if timestamp is within tolerance (without logging)
     */
    boolean isTimestampValid(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) {
                return false;
            }

            long timestampEpoch;
            try {
                timestampEpoch = Long.parseLong(timestamp);
            } catch (NumberFormatException e) {
                try {
                    Instant instant = Instant.parse(timestamp);
                    timestampEpoch = instant.getEpochSecond();
                } catch (Exception ex) {
                    return false;
                }
            }

            long currentEpoch = Instant.now().getEpochSecond();
            long diff = Math.abs(currentEpoch - timestampEpoch);

            return diff <= timestampToleranceSeconds;

        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Key Rotation ====================

    /**
     * S25-2: Rotate signing key
     *
     * Creates a new signing key and marks the current key for deprecation.
     * The old key remains valid for verification during a transition period.
     */
    public void rotateSigningKey() {
        rotateSigningKey(keyRotationIntervalDays);
    }

    /**
     * Rotate signing key with custom validity period for old key
     */
    public void rotateSigningKey(long oldKeyValidityDays) {
        String newKeyId = generateKeyId();
        String newSecret = generateSecureSecret();
        Instant now = Instant.now();

        // Deprecate current key but keep it valid for verification
        if (currentActiveKeyId != null) {
            SigningKey currentKey = signingKeys.get(currentActiveKeyId);
            if (currentKey != null) {
                SigningKey deprecatedKey = new SigningKey(
                    currentKey.keyId(),
                    currentKey.secret(),
                    currentKey.createdAt(),
                    now.plusSeconds(oldKeyValidityDays * 24 * 60 * 60),
                    false
                );
                signingKeys.put(currentKey.keyId(), deprecatedKey);
                logger.info("Deprecated key {} (valid until {})", currentKey.keyId(), deprecatedKey.expiresAt());
            }
        }

        // Create new active key
        SigningKey newKey = new SigningKey(
            newKeyId,
            newSecret,
            now,
            now.plusSeconds(keyRotationIntervalDays * 24 * 60 * 60),
            true
        );
        signingKeys.put(newKeyId, newKey);
        keyMetadata.put(newKeyId, new KeyMetadata(newKeyId, HMAC_ALGORITHM, now, null, 0));
        currentActiveKeyId = newKeyId;

        logger.info("Rotated to new signing key: {}", newKeyId);

        // Clean up expired keys
        cleanupExpiredKeys();

        // Notify config service if available
        if (hotReloadConfigService != null) {
            notifyKeyRotation(newKey);
        }
    }

    /**
     * Check and rotate expired keys automatically
     */
    private void checkAndRotateExpiredKeys() {
        SigningKey currentKey = signingKeys.get(currentActiveKeyId);
        if (currentKey != null && currentKey.isExpired()) {
            logger.info("Current active key {} has expired, rotating", currentActiveKeyId);
            rotateSigningKey();
        }
    }

    /**
     * Clean up expired keys
     */
    private void cleanupExpiredKeys() {
        signingKeys.entrySet().removeIf(entry -> {
            SigningKey key = entry.getValue();
            boolean shouldRemove = key.isExpired() && !key.active() &&
                                   entry.getKey() != currentActiveKeyId;
            if (shouldRemove) {
                logger.debug("Removing expired key: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }

    /**
     * Notify external services about key rotation
     */
    private void notifyKeyRotation(SigningKey newKey) {
        try {
            // This could be extended to persist keys to config
            logger.debug("Key rotation notification for key: {}", newKey.keyId());
        } catch (Exception e) {
            logger.error("Error notifying key rotation: {}", e.getMessage());
        }
    }

    // ==================== Key Management ====================

    /**
     * Add a signing key
     */
    public void addSigningKey(String keyId, String secret) {
        addSigningKey(keyId, secret, keyRotationIntervalDays);
    }

    /**
     * Add a signing key with custom expiration
     */
    public void addSigningKey(String keyId, String secret, long validityDays) {
        Instant now = Instant.now();
        SigningKey key = new SigningKey(
            keyId,
            secret,
            now,
            now.plusSeconds(validityDays * 24 * 60 * 60),
            signingKeys.isEmpty()
        );
        signingKeys.put(keyId, key);
        keyMetadata.put(keyId, new KeyMetadata(keyId, HMAC_ALGORITHM, now, null, 0));

        if (key.active() && currentActiveKeyId == null) {
            currentActiveKeyId = keyId;
        }

        logger.info("Added signing key: {}", keyId);
    }

    /**
     * Remove a signing key
     */
    public boolean removeSigningKey(String keyId) {
        if (keyId.equals(currentActiveKeyId)) {
            logger.warn("Cannot remove active key: {}", keyId);
            return false;
        }

        SigningKey removed = signingKeys.remove(keyId);
        keyMetadata.remove(keyId);

        if (removed != null) {
            logger.info("Removed signing key: {}", keyId);
            return true;
        }
        return false;
    }

    /**
     * Get signing key metadata
     */
    public KeyMetadata getKeyMetadata(String keyId) {
        return keyMetadata.get(keyId);
    }

    /**
     * Get all key metadata
     */
    public Map<String, KeyMetadata> getAllKeyMetadata() {
        return new ConcurrentHashMap<>(keyMetadata);
    }

    /**
     * Get current active key ID
     */
    public String getCurrentActiveKeyId() {
        return currentActiveKeyId;
    }

    /**
     * Update key usage statistics
     */
    private void updateKeyUsage(String keyId) {
        KeyMetadata metadata = keyMetadata.get(keyId);
        if (metadata != null) {
            keyMetadata.put(keyId, new KeyMetadata(
                metadata.keyId(),
                metadata.algorithm(),
                metadata.createdAt(),
                Instant.now(),
                metadata.useCount() + 1
            ));
        }
    }

    // ==================== Configuration ====================

    /**
     * Set timestamp tolerance
     */
    public void setTimestampToleranceSeconds(long seconds) {
        this.timestampToleranceSeconds = seconds;
        logger.info("Updated timestamp tolerance to {} seconds", seconds);
    }

    /**
     * Set key rotation interval
     */
    public void setKeyRotationIntervalDays(long days) {
        this.keyRotationIntervalDays = days;
        logger.info("Updated key rotation interval to {} days", days);
    }

    // ==================== Utility Methods ====================

    /**
     * Generate secure key ID
     */
    private String generateKeyId() {
        return "sk-" + System.currentTimeMillis() + "-" + Math.abs(secureRandom.nextInt(10000));
    }

    /**
     * Generate secure secret
     */
    private String generateSecureSecret() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    // ==================== Statistics ====================

    /**
     * Get service statistics
     */
    public SignatureStats getStats() {
        return new SignatureStats(
            signingKeys.size(),
            currentActiveKeyId,
            timestampToleranceSeconds,
            keyRotationIntervalDays,
            keyMetadata.get(currentActiveKeyId) != null ?
                keyMetadata.get(currentActiveKeyId).createdAt() : null
        );
    }

    /**
     * Shutdown service and cleanup resources
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        logger.info("WebhookSignatureService shutdown complete");
    }
}
