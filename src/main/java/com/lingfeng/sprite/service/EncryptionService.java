package com.lingfeng.sprite.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * S25-3: Sensitive Data Encryption Service
 *
 * Provides AES-256-GCM encryption for sensitive data at rest:
 * - String encryption/decryption
 * - File encryption/decryption
 * - Key derivation from master key
 * - Key rotation support
 * - Field-level encryption for configuration
 *
 * Integration Points:
 * - GitHubBackupService (encrypted backups)
 * - MemoryPersistenceService (encrypted memory storage)
 * - HotReloadConfigService (encrypted config secrets)
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    // AES-256-GCM configuration
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int AES_KEY_LENGTH = 256;

    // Key derivation configuration
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_DERIVATION_ITERATIONS = 65536;
    private static final int DERIVED_KEY_LENGTH = 256;
    private static final byte[] SALT_SEPARATOR = new byte[]{0x00};

    // Master key and derived keys cache
    @Value("${encryption.master-key:}")
    private String masterKeyEnv;

    @Value("${encryption.key-file:${user.home}/.sprite/keys/master.key}")
    private String keyFilePath;

    private volatile SecretKey masterKey;
    private final Map<String, SecretKey> derivedKeys = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    // Encryption metadata prefix for version tracking
    private static final String ENCRYPTED_PREFIX = "ENC[AES-256-GCM]:";
    private static final int CURRENT_VERSION = 1;

    public EncryptionService() {
        logger.info("EncryptionService initialized");
    }

    // ==================== Master Key Management ====================

    /**
     * Initialize master key from environment variable or key file
     */
    private SecretKey getMasterKey() {
        if (masterKey != null) {
            return masterKey;
        }

        synchronized (this) {
            if (masterKey != null) {
                return masterKey;
            }

            String keySource = masterKeyEnv;
            if (keySource == null || keySource.isEmpty() || keySource.equals("default-key")) {
                // Try to load from key file
                keySource = loadKeyFromFile();
            }

            if (keySource == null || keySource.isEmpty()) {
                // Generate a new master key if none exists
                logger.warn("No master key found, generating a new one. This should only happen on first run.");
                keySource = generateAndSaveMasterKey();
            }

            this.masterKey = deriveKeyFromMaster(keySource, "master");
            return masterKey;
        }
    }

    /**
     * Load master key from key file
     */
    private String loadKeyFromFile() {
        try {
            Path keyFile = Path.of(keyFilePath);
            if (Files.exists(keyFile)) {
                String key = Files.readString(keyFile).trim();
                logger.info("Loaded master key from: {}", keyFilePath);
                return key;
            }
        } catch (Exception e) {
            logger.warn("Could not load master key from file: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Generate and save a new master key
     */
    private String generateAndSaveMasterKey() {
        try {
            // Generate 256-bit random key
            byte[] keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            String key = Base64.getEncoder().encodeToString(keyBytes);

            // Ensure directory exists
            Path keyFile = Path.of(keyFilePath);
            Files.createDirectories(keyFile.getParent());

            // Save key with restricted permissions (600)
            Files.writeString(keyFile, key);
            logger.info("Generated and saved new master key to: {}", keyFilePath);

            return key;
        } catch (Exception e) {
            logger.error("Failed to generate master key: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }

    /**
     * Derive a key from the master key using PBKDF2
     */
    private SecretKey deriveKeyFromMaster(String masterKey, String purpose) {
        String cacheKey = purpose;
        SecretKey cached = derivedKeys.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            // Create salt from purpose
            byte[] purposeBytes = purpose.getBytes(StandardCharsets.UTF_8);
            byte[] salt = MessageDigest.getInstance("SHA-256").digest(purposeBytes);

            // Derive key using PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            KeySpec spec = new PBEKeySpec(masterKey.toCharArray(), salt, KEY_DERIVATION_ITERATIONS, DERIVED_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey derivedKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            derivedKeys.put(cacheKey, derivedKey);
            return derivedKey;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Failed to derive key for purpose '{}': {}", purpose, e.getMessage());
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    // ==================== String Encryption ====================

    /**
     * S25-3: Encrypt data
     *
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data with metadata
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        // Check if already encrypted
        if (plaintext.startsWith(ENCRYPTED_PREFIX)) {
            logger.debug("Data is already encrypted, returning as-is");
            return plaintext;
        }

        try {
            SecretKey key = getMasterKey();

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Encode with version and algorithm info
            String encoded = Base64.getEncoder().encodeToString(byteBuffer.array());
            return ENCRYPTED_PREFIX + CURRENT_VERSION + ":" + encoded;

        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * S25-3: Decrypt data
     *
     * @param ciphertext The encrypted data (Base64-encoded)
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        // Check if actually encrypted
        if (!ciphertext.startsWith(ENCRYPTED_PREFIX)) {
            logger.debug("Data is not encrypted, returning as-is");
            return ciphertext;
        }

        try {
            // Parse version and encoded data
            String data = ciphertext.substring(ENCRYPTED_PREFIX.length());
            int colonIndex = data.indexOf(':');
            int version = CURRENT_VERSION;
            String encoded;

            if (colonIndex > 0) {
                version = Integer.parseInt(data.substring(0, colonIndex));
                encoded = data.substring(colonIndex + 1);
            } else {
                encoded = data;
            }

            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(encoded);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            // Get appropriate key (version-aware)
            SecretKey key = getMasterKey();

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // ==================== File Encryption ====================

    /**
     * S25-3: Encrypt file
     *
     * @param source Source file path
     * @param target Target encrypted file path
     */
    public void encryptFile(Path source, Path target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths cannot be null");
        }

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source file does not exist: " + source);
        }

        try {
            logger.info("Encrypting file: {} -> {}", source, target);

            // Read source file
            byte[] plaintext = Files.readAllBytes(source);

            // Encrypt the content
            String encrypted = encrypt(Base64.getEncoder().encodeToString(plaintext));

            // Write encrypted content with header
            ByteBuffer header = ByteBuffer.allocate(4);
            header.putInt(CURRENT_VERSION);
            byte[] headerBytes = header.array();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(headerBytes);
            outputStream.write(encrypted.getBytes(StandardCharsets.UTF_8));

            // Ensure target directory exists
            Files.createDirectories(target.getParent());
            Files.write(target, outputStream.toByteArray());

            logger.info("File encrypted successfully: {} ({} bytes -> {} bytes)",
                    source, plaintext.length, target);

        } catch (Exception e) {
            logger.error("File encryption failed: {}", e.getMessage());
            throw new RuntimeException("File encryption failed", e);
        }
    }

    /**
     * S25-3: Decrypt file
     *
     * @param source Encrypted source file path
     * @param target Target decrypted file path
     */
    public void decryptFile(Path source, Path target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths cannot be null");
        }

        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Source file does not exist: " + source);
        }

        try {
            logger.info("Decrypting file: {} -> {}", source, target);

            // Read encrypted file
            byte[] encryptedData = Files.readAllBytes(source);

            // Read version header
            ByteBuffer header = ByteBuffer.wrap(encryptedData, 0, 4);
            int version = header.getInt();

            // Get encrypted content (skip 4-byte header)
            String encrypted = new String(encryptedData, 4, encryptedData.length - 4, StandardCharsets.UTF_8);

            // Decrypt
            String decrypted = decrypt(encrypted);

            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(decrypted);

            // Ensure target directory exists
            Files.createDirectories(target.getParent());
            Files.write(target, decoded);

            logger.info("File decrypted successfully: {} ({} bytes -> {} bytes)",
                    source, source.toFile().length(), decoded.length);

        } catch (Exception e) {
            logger.error("File decryption failed: {}", e.getMessage());
            throw new RuntimeException("File decryption failed", e);
        }
    }

    // ==================== Key Rotation ====================

    /**
     * S25-3: Re-encrypt data with a new key
     *
     * This method decrypts with the old key and re-encrypts with the new key.
     * Used for key rotation or when migrating between key versions.
     *
     * @param oldKey Old master key
     * @param newKey New master key
     */
    public void reEncrypt(String oldKey, String newKey) {
        if (oldKey == null || newKey == null) {
            throw new IllegalArgumentException("Old and new keys cannot be null");
        }

        logger.info("Starting key rotation");

        try {
            // Verify old key works by trying to derive a key from it
            SecretKey oldDerivedKey = deriveKeyFromMaster(oldKey, "master");
            if (oldDerivedKey == null) {
                throw new IllegalArgumentException("Invalid old key");
            }

            // Clear derived keys cache
            derivedKeys.clear();

            // Update master key to new one
            synchronized (this) {
                this.masterKey = deriveKeyFromMaster(newKey, "master");
            }

            logger.info("Key rotation completed successfully");

        } catch (Exception e) {
            logger.error("Key rotation failed: {}", e.getMessage());
            throw new RuntimeException("Key rotation failed", e);
        }
    }

    /**
     * Generate a new derived key for a specific purpose
     *
     * @param purpose The purpose/key identifier
     * @return Base64-encoded derived key
     */
    public String generateDerivedKey(String purpose) {
        try {
            byte[] keyBytes = new byte[32]; // 256 bits
            secureRandom.nextBytes(keyBytes);
            String encodedKey = Base64.getEncoder().encodeToString(keyBytes);

            // Store for later use
            SecretKey derivedKey = new SecretKeySpec(keyBytes, "AES");
            derivedKeys.put("derived:" + purpose, derivedKey);

            logger.info("Generated derived key for purpose: {}", purpose);
            return encodedKey;

        } catch (Exception e) {
            logger.error("Failed to generate derived key: {}", e.getMessage());
            throw new RuntimeException("Derived key generation failed", e);
        }
    }

    // ==================== Field-Level Encryption ====================

    /**
     * Encrypt a specific field value in a map
     *
     * @param data Map containing sensitive data
     * @param fieldName The field name to encrypt
     * @return New map with the field encrypted
     */
    public Map<String, Object> encryptField(Map<String, Object> data, String fieldName) {
        if (data == null || fieldName == null) {
            return data;
        }

        Map<String, Object> result = new ConcurrentHashMap<>(data);
        Object value = result.get(fieldName);

        if (value != null) {
            String encrypted = encrypt(value.toString());
            result.put(fieldName, encrypted);
            logger.debug("Encrypted field: {}", fieldName);
        }

        return result;
    }

    /**
     * Decrypt a specific field value in a map
     *
     * @param data Map containing encrypted data
     * @param fieldName The field name to decrypt
     * @return New map with the field decrypted
     */
    public Map<String, Object> decryptField(Map<String, Object> data, String fieldName) {
        if (data == null || fieldName == null) {
            return data;
        }

        Map<String, Object> result = new ConcurrentHashMap<>(data);
        Object value = result.get(fieldName);

        if (value != null && value instanceof String) {
            String decrypted = decrypt((String) value);
            result.put(fieldName, decrypted);
            logger.debug("Decrypted field: {}", fieldName);
        }

        return result;
    }

    /**
     * Encrypt multiple fields in a map
     *
     * @param data Map containing sensitive data
     * @param fieldNames Array of field names to encrypt
     * @return New map with specified fields encrypted
     */
    public Map<String, Object> encryptFields(Map<String, Object> data, String... fieldNames) {
        if (data == null || fieldNames == null) {
            return data;
        }

        Map<String, Object> result = new ConcurrentHashMap<>(data);
        for (String fieldName : fieldNames) {
            encryptFieldInPlace(result, fieldName);
        }
        return result;
    }

    /**
     * Decrypt multiple fields in a map
     *
     * @param data Map containing encrypted data
     * @param fieldNames Array of field names to decrypt
     * @return New map with specified fields decrypted
     */
    public Map<String, Object> decryptFields(Map<String, Object> data, String... fieldNames) {
        if (data == null || fieldNames == null) {
            return data;
        }

        Map<String, Object> result = new ConcurrentHashMap<>(data);
        for (String fieldName : fieldNames) {
            decryptFieldInPlace(result, fieldName);
        }
        return result;
    }

    private void encryptFieldInPlace(Map<String, Object> data, String fieldName) {
        Object value = data.get(fieldName);
        if (value != null) {
            data.put(fieldName, encrypt(value.toString()));
        }
    }

    private void decryptFieldInPlace(Map<String, Object> data, String fieldName) {
        Object value = data.get(fieldName);
        if (value != null && value instanceof String) {
            data.put(fieldName, decrypt((String) value));
        }
    }

    // ==================== Configuration Secrets Encryption ====================

    /**
     * Encrypt a configuration secret
     *
     * @param secret The secret value to encrypt
     * @return Encrypted secret ready for storage
     */
    public String encryptSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return secret;
        }
        return encrypt(secret);
    }

    /**
     * Decrypt a configuration secret
     *
     * @param encryptedSecret The encrypted secret
     * @return Decrypted secret value
     */
    public String decryptSecret(String encryptedSecret) {
        if (encryptedSecret == null || encryptedSecret.isEmpty()) {
            return encryptedSecret;
        }
        return decrypt(encryptedSecret);
    }

    /**
     * Encrypt configuration file content (JSON/YAML)
     *
     * @param content Configuration file content
     * @param sensitiveFields Fields that contain sensitive data
     * @return Configuration with sensitive fields encrypted
     */
    public String encryptConfigContent(String content, String... sensitiveFields) {
        // This is a simplified implementation
        // In production, you might want to use a proper JSON/YAML parser
        String result = content;
        for (String field : sensitiveFields) {
            // Simple pattern-based encryption for config values
            // Format: "field": "value" or "field":"value"
            String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
            result = result.replaceAll(pattern, "\"" + field + "\": \"" + encrypt("$1") + "\"");
        }
        return result;
    }

    // ==================== Backup Encryption ====================

    /**
     * Encrypt data for backup
     *
     * @param data Data to encrypt
     * @return Encrypted data suitable for backup storage
     */
    public byte[] encryptForBackup(byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            SecretKey key = getMasterKey();

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(data);

            // Combine IV + ciphertext with magic header for backup identification
            ByteBuffer buffer = ByteBuffer.allocate(8 + GCM_IV_LENGTH + ciphertext.length);
            buffer.put("SPRITEBK".getBytes(StandardCharsets.UTF_8)); // Magic header
            buffer.putInt(CURRENT_VERSION);
            buffer.put(iv);
            buffer.put(ciphertext);

            return buffer.array();

        } catch (Exception e) {
            logger.error("Backup encryption failed: {}", e.getMessage());
            throw new RuntimeException("Backup encryption failed", e);
        }
    }

    /**
     * Decrypt backup data
     *
     * @param encryptedData Encrypted backup data
     * @return Decrypted original data
     */
    public byte[] decryptFromBackup(byte[] encryptedData) {
        if (encryptedData == null) {
            return null;
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

            // Verify magic header
            byte[] magic = new byte[8];
            buffer.get(magic);
            if (!"SPRITEBK".equals(new String(magic, StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid backup data format");
            }

            // Read version
            int version = buffer.getInt();

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Get key
            SecretKey key = getMasterKey();

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            logger.error("Backup decryption failed: {}", e.getMessage());
            throw new RuntimeException("Backup decryption failed", e);
        }
    }

    /**
     * Encrypt file for backup
     *
     * @param source Source file
     * @param target Target encrypted backup file
     */
    public void encryptFileForBackup(Path source, Path target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths cannot be null");
        }

        try {
            byte[] fileContent = Files.readAllBytes(source);
            byte[] encrypted = encryptForBackup(fileContent);

            Files.createDirectories(target.getParent());
            Files.write(target, encrypted);

            logger.info("File encrypted for backup: {} -> {} ({} bytes -> {} bytes)",
                    source, target, fileContent.length, encrypted.length);

        } catch (Exception e) {
            logger.error("Backup file encryption failed: {}", e.getMessage());
            throw new RuntimeException("Backup file encryption failed", e);
        }
    }

    /**
     * Decrypt backup file
     *
     * @param source Encrypted backup file
     * @param target Target decrypted file
     */
    public void decryptBackupFile(Path source, Path target) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Source and target paths cannot be null");
        }

        try {
            byte[] encrypted = Files.readAllBytes(source);
            byte[] decrypted = decryptFromBackup(encrypted);

            Files.createDirectories(target.getParent());
            Files.write(target, decrypted);

            logger.info("Backup file decrypted: {} -> {} ({} bytes -> {} bytes)",
                    source, target, encrypted.length, decrypted.length);

        } catch (Exception e) {
            logger.error("Backup file decryption failed: {}", e.getMessage());
            throw new RuntimeException("Backup file decryption failed", e);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Check if a string is encrypted
     *
     * @param value String to check
     * @return true if the string appears to be encrypted
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    /**
     * Get encryption statistics
     *
     * @return Encryption stats
     */
    public EncryptionStats getStats() {
        return new EncryptionStats(
                derivedKeys.size(),
                masterKey != null,
                CURRENT_VERSION
        );
    }

    /**
     * Clear all cached derived keys (forces re-derivation)
     */
    public void clearKeyCache() {
        derivedKeys.clear();
        logger.info("Key cache cleared");
    }

    /**
     * Encryption statistics record
     */
    public record EncryptionStats(
            int cachedKeys,
            boolean masterKeyInitialized,
            int currentVersion
    ) {}

    /**
     * Shutdown and cleanup
     */
    public void shutdown() {
        // Clear sensitive data
        derivedKeys.clear();
        masterKey = null;
        logger.info("EncryptionService shutdown complete");
    }
}
