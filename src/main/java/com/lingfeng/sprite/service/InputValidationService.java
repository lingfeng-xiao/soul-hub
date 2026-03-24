package com.lingfeng.sprite.service;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * S25-4: Input Validation Enhancement Service
 *
 * Comprehensive input validation service for all external inputs:
 * - String validation with configurable rules
 * - Number range validation
 * - File upload validation (type, size)
 * - JSON validation with schema support
 * - Input sanitization for XSS and injection prevention
 *
 * Integration Points:
 * - SpriteController (API request parameter validation)
 * - WebhookService (incoming webhook payload validation)
 */
@Service
public class InputValidationService {

    private static final Logger logger = LoggerFactory.getLogger(InputValidationService.class);

    // Default allowed HTML tags for sanitization
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(';|--|;|/\\\\*|\\\\*\\/|xp_|sp_|exec|execute|insert|delete|update|drop|alter|create|truncate)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[<>\"'&]");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("(\\.\\./|\\.\\.\\\\|%2e%2e/|%2e%2e\\\\)", Pattern.CASE_INSENSITIVE);

    // Default validation limits
    private static final int DEFAULT_MAX_STRING_LENGTH = 10000;
    private static final int DEFAULT_MIN_STRING_LENGTH = 0;
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> DEFAULT_ALLOWED_FILE_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/json", "text/plain", "text/csv",
        "application/pdf", "application/xml", "text/xml"
    );

    /**
     * Validation result record containing validation status and error messages
     */
    public record ValidationResult(
        boolean valid,
        List<String> errors
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public ValidationResult merge(ValidationResult other) {
            if (this.valid && other.valid) {
                return this;
            }
            List<String> combinedErrors = new ArrayList<>(this.errors);
            combinedErrors.addAll(other.errors);
            return new ValidationResult(false, combinedErrors);
        }
    }

    /**
     * String validation rules
     */
    public record ValidationRules(
        int minLength,
        int maxLength,
        Pattern pattern,
        String allowedChars,
        boolean allowHtml,
        boolean allowSql
    ) {
        public static ValidationRules defaultRules() {
            return new ValidationRules(DEFAULT_MIN_STRING_LENGTH, DEFAULT_MAX_STRING_LENGTH, null, null, false, false);
        }

        public static ValidationRules strictRules() {
            return new ValidationRules(1, 1000, null, null, false, false);
        }

        public static ValidationRules relaxedRules() {
            return new ValidationRules(0, DEFAULT_MAX_STRING_LENGTH, null, null, true, true);
        }
    }

    /**
     * File validation rules
     */
    public record FileValidationRules(
        List<String> allowedTypes,
        long maxSize,
        boolean allowEmpty,
        List<String> blockedTypes
    ) {
        public static FileValidationRules defaultRules() {
            return new FileValidationRules(DEFAULT_ALLOWED_FILE_TYPES, DEFAULT_MAX_FILE_SIZE, false, List.of("application/x-executable", "application/x-msdownload"));
        }

        public static FileValidationRules imageOnly() {
            return new FileValidationRules(List.of("image/jpeg", "image/png", "image/gif", "image/webp"), DEFAULT_MAX_FILE_SIZE, false, List.of());
        }

        public static FileValidationRules documentOnly() {
            return new FileValidationRules(List.of("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"), 50 * 1024 * 1024, false, List.of());
        }
    }

    /**
     * JSON schema for validation
     */
    public record JsonSchema(
        boolean requireNonNull,
        List<String> requiredFields,
        int maxDepth,
        int maxLength
    ) {
        public static JsonSchema defaultSchema() {
            return new JsonSchema(false, List.of(), 10, DEFAULT_MAX_STRING_LENGTH);
        }

        public static JsonSchema strictSchema(List<String> requiredFields) {
            return new JsonSchema(true, requiredFields, 5, 10000);
        }
    }

    // ==================== String Validation ====================

    /**
     * S25-4: Validate string input with configurable rules
     *
     * @param input  The string to validate
     * @param rules  Validation rules to apply
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validateString(String input, ValidationRules rules) {
        List<String> errors = new ArrayList<>();

        if (input == null) {
            if (rules.minLength() > 0) {
                errors.add("Input string cannot be null");
                return ValidationResult.failure(errors);
            }
            return ValidationResult.success();
        }

        // Length validation
        if (input.length() < rules.minLength()) {
            errors.add(String.format("Input string length %d is below minimum required length %d", input.length(), rules.minLength()));
        }

        if (input.length() > rules.maxLength()) {
            errors.add(String.format("Input string length %d exceeds maximum allowed length %d", input.length(), rules.maxLength()));
        }

        // Pattern validation
        if (rules.pattern() != null && !rules.pattern().matcher(input).find()) {
            errors.add("Input string does not match required pattern");
        }

        // Allowed characters validation
        if (rules.allowedChars() != null && !rules.allowedChars().isEmpty()) {
            String invalidChars = findInvalidChars(input, rules.allowedChars());
            if (!invalidChars.isEmpty()) {
                errors.add(String.format("Input contains invalid characters: %s", invalidChars));
            }
        }

        // HTML validation
        if (!rules.allowHtml() && containsHtml(input)) {
            errors.add("Input contains HTML tags which are not allowed");
        }

        // SQL injection check
        if (!rules.allowSql() && containsSqlInjection(input)) {
            errors.add("Input contains potential SQL injection patterns");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * S25-4: Validate string with default rules
     */
    public ValidationResult validateString(String input) {
        return validateString(input, ValidationRules.defaultRules());
    }

    // ==================== Number Validation ====================

    /**
     * S25-4: Validate number is within specified range
     *
     * @param input The number to validate
     * @param min   Minimum allowed value (inclusive)
     * @param max   Maximum allowed value (inclusive)
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validateNumber(Number input, double min, double max) {
        List<String> errors = new ArrayList<>();

        if (input == null) {
            errors.add("Input number cannot be null");
            return ValidationResult.failure(errors);
        }

        double value = input.doubleValue();

        if (value < min) {
            errors.add(String.format("Number value %.2f is below minimum allowed value %.2f", value, min));
        }

        if (value > max) {
            errors.add(String.format("Number value %.2f exceeds maximum allowed value %.2f", value, max));
        }

        // Check for NaN and Infinity
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            errors.add("Number value must be finite (not NaN or Infinite)");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * S25-4: Validate integer within range
     */
    public ValidationResult validateInteger(Integer input, int min, int max) {
        List<String> errors = new ArrayList<>();

        if (input == null) {
            errors.add("Input integer cannot be null");
            return ValidationResult.failure(errors);
        }

        if (input < min) {
            errors.add(String.format("Integer value %d is below minimum allowed value %d", input, min));
        }

        if (input > max) {
            errors.add(String.format("Integer value %d exceeds maximum allowed value %d", input, max));
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    // ==================== File Validation ====================

    /**
     * S25-4: Validate file upload with configurable rules
     *
     * @param file  The file to validate
     * @param rules File validation rules to apply
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validateFile(File file, FileValidationRules rules) {
        List<String> errors = new ArrayList<>();

        if (file == null) {
            errors.add("File cannot be null");
            return ValidationResult.failure(errors);
        }

        // Check file existence
        if (!file.exists()) {
            errors.add("File does not exist");
            return ValidationResult.failure(errors);
        }

        // Check if empty
        if (!rules.allowEmpty() && file.length() == 0) {
            errors.add("File is empty");
        }

        // Size validation
        if (file.length() > rules.maxSize()) {
            errors.add(String.format("File size %d bytes exceeds maximum allowed size %d bytes", file.length(), rules.maxSize()));
        }

        // Type validation based on extension
        String fileName = file.getName().toLowerCase();
        String detectedType = detectFileType(fileName);

        if (!rules.allowedTypes().isEmpty() && !isAllowedFileType(detectedType, rules.allowedTypes())) {
            errors.add(String.format("File type '%s' is not allowed. Allowed types: %s", detectedType, String.join(", ", rules.allowedTypes())));
        }

        // Check blocked types
        if (!rules.blockedTypes().isEmpty() && isBlockedFileType(detectedType, rules.blockedTypes())) {
            errors.add(String.format("File type '%s' is explicitly blocked", detectedType));
        }

        // Path traversal check
        String absolutePath = file.getAbsolutePath();
        if (containsPathTraversal(absolutePath)) {
            errors.add("File path contains potential path traversal patterns");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * S25-4: Validate file with default rules
     */
    public ValidationResult validateFile(File file) {
        return validateFile(file, FileValidationRules.defaultRules());
    }

    /**
     * S25-4: Validate file from input stream and metadata
     */
    public ValidationResult validateFileContent(String fileName, long fileSize, String contentType, FileValidationRules rules) {
        List<String> errors = new ArrayList<>();

        if (fileName == null || fileName.isBlank()) {
            errors.add("File name cannot be empty");
        }

        if (fileSize < 0) {
            errors.add("File size cannot be negative");
        }

        if (fileSize > rules.maxSize()) {
            errors.add(String.format("File size %d bytes exceeds maximum allowed size %d bytes", fileSize, rules.maxSize()));
        }

        // Validate content type
        if (contentType != null && !rules.allowedTypes().isEmpty()) {
            if (!isAllowedFileType(contentType.toLowerCase(), rules.allowedTypes())) {
                errors.add(String.format("Content type '%s' is not allowed", contentType));
            }
        }

        // Check blocked content types
        if (contentType != null && !rules.blockedTypes().isEmpty()) {
            if (isBlockedFileType(contentType.toLowerCase(), rules.blockedTypes())) {
                errors.add(String.format("Content type '%s' is explicitly blocked", contentType));
            }
        }

        // Check for path traversal in filename
        if (fileName != null && containsPathTraversal(fileName)) {
            errors.add("File name contains potential path traversal patterns");
        }

        // Check for dangerous extensions
        if (fileName != null && isDangerousExtension(fileName)) {
            errors.add(String.format("File extension in '%s' is not allowed for security reasons", fileName));
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    // ==================== Input Sanitization ====================

    /**
     * S25-4: Sanitize input string to prevent XSS and injection attacks
     *
     * @param input The string to sanitize
     * @return Sanitized string safe for storage/display
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove script tags and content
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers (onclick, onload, etc.)
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");

        // Remove HTML tags if they appear to be malicious
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Encode special HTML characters
        sanitized = encodeHtmlCharacters(sanitized);

        // Remove SQL injection patterns
        sanitized = removeSqlInjectionPatterns(sanitized);

        // Remove path traversal patterns
        sanitized = removePathTraversalPatterns(sanitized);

        // Trim whitespace
        sanitized = sanitized.trim();

        return sanitized;
    }

    /**
     * S25-4: Deep sanitize for HTML content that should preserve some tags
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove dangerous event handlers while preserving safe tags
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");

        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");

        // Remove dangerous file:// and javascript: URLs
        sanitized = sanitized.replaceAll("javascript:", "");
        sanitized = sanitized.replaceAll("file:", "");

        return sanitized;
    }

    /**
     * S25-4: Sanitize for SQL context (parameterized queries should be used alongside)
     */
    public String sanitizeForSql(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Escape single quotes
        sanitized = sanitized.replace("'", "''");

        // Remove common SQL keywords in injection attempts
        sanitized = SQL_INJECTION_PATTERN.matcher(sanitized).replaceAll("");

        return sanitized;
    }

    /**
     * S25-4: Sanitize file path component
     */
    public String sanitizeFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String sanitized = path;

        // Remove path traversal patterns
        sanitized = removePathTraversalPatterns(sanitized);

        // Remove null bytes
        sanitized = sanitized.replace("\0", "");

        // Normalize path separators (for cross-platform)
        sanitized = sanitized.replace("\\", "/");

        // Remove duplicate slashes
        sanitized = sanitized.replaceAll("//+", "/");

        return sanitized;
    }

    // ==================== JSON Validation ====================

    /**
     * S25-4: Validate JSON string against schema
     *
     * @param json   The JSON string to validate
     * @param schema The schema to validate against
     * @return ValidationResult containing validation status and errors
     */
    public ValidationResult validateJson(String json, JsonSchema schema) {
        List<String> errors = new ArrayList<>();

        if (json == null || json.isBlank()) {
            if (schema.requireNonNull()) {
                errors.add("JSON input cannot be null or empty");
            }
            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
        }

        // Length check
        if (json.length() > schema.maxLength()) {
            errors.add(String.format("JSON length %d exceeds maximum allowed length %d", json.length(), schema.maxLength()));
        }

        // Basic JSON syntax validation
        if (!isValidJsonSyntax(json)) {
            errors.add("JSON syntax is invalid");
            return ValidationResult.failure(errors);
        }

        // Check depth (approximate by counting nesting)
        int depth = estimateJsonDepth(json);
        if (depth > schema.maxDepth()) {
            errors.add(String.format("JSON nesting depth %d exceeds maximum allowed depth %d", depth, schema.maxDepth()));
        }

        // Check required fields (if schema specifies them)
        if (schema.requiredFields() != null && !schema.requiredFields().isEmpty()) {
            List<String> missingFields = findMissingRequiredFields(json, schema.requiredFields());
            if (!missingFields.isEmpty()) {
                errors.add(String.format("JSON is missing required fields: %s", String.join(", ", missingFields)));
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * S25-4: Validate JSON with default schema
     */
    public ValidationResult validateJson(String json) {
        return validateJson(json, JsonSchema.defaultSchema());
    }

    /**
     * S25-4: Check if string is valid JSON
     */
    public boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        return isValidJsonSyntax(json);
    }

    // ==================== API Request Validation ====================

    /**
     * S25-4: Validate API request parameters
     */
    public ValidationResult validateApiRequest(String paramName, String paramValue, ValidationRules rules) {
        if (paramName == null || paramName.isBlank()) {
            return ValidationResult.failure("Parameter name cannot be empty");
        }

        ValidationResult stringValidation = validateString(paramValue, rules);
        if (!stringValidation.valid()) {
            List<String> errors = new ArrayList<>();
            errors.add(String.format("Validation failed for parameter '%s': %s", paramName, String.join(", ", stringValidation.errors())));
            return ValidationResult.failure(errors);
        }

        return ValidationResult.success();
    }

    /**
     * S25-4: Validate multiple API request parameters
     */
    public ValidationResult validateApiRequest(java.util.Map<String, String> params, java.util.Map<String, ValidationRules> rulesMap) {
        List<String> allErrors = new ArrayList<>();

        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            ValidationRules rules = rulesMap.getOrDefault(paramName, ValidationRules.defaultRules());

            ValidationResult result = validateApiRequest(paramName, paramValue, rules);
            if (!result.valid()) {
                allErrors.addAll(result.errors());
            }
        }

        if (allErrors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(allErrors);
    }

    /**
     * S25-4: Validate email format
     */
    public ValidationResult validateEmail(String email) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.isBlank()) {
            errors.add("Email cannot be empty");
            return ValidationResult.failure(errors);
        }

        // Basic email pattern
        Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        if (!emailPattern.matcher(email).matches()) {
            errors.add("Invalid email format");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    /**
     * S25-4: Validate URL format
     */
    public ValidationResult validateUrl(String url) {
        List<String> errors = new ArrayList<>();

        if (url == null || url.isBlank()) {
            errors.add("URL cannot be empty");
            return ValidationResult.failure(errors);
        }

        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String protocol = parsedUrl.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                errors.add("URL must use HTTP or HTTPS protocol");
            }
        } catch (Exception e) {
            errors.add("Invalid URL format");
        }

        // Check for path traversal
        if (containsPathTraversal(url)) {
            errors.add("URL contains path traversal patterns");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    // ==================== Helper Methods ====================

    /**
     * Find invalid characters not in allowed set
     */
    private String findInvalidChars(String input, String allowedChars) {
        StringBuilder invalid = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (allowedChars.indexOf(c) == -1) {
                invalid.append(c);
            }
        }
        return invalid.toString();
    }

    /**
     * Check if string contains HTML tags
     */
    private boolean containsHtml(String input) {
        return HTML_TAG_PATTERN.matcher(input).find();
    }

    /**
     * Check if string contains SQL injection patterns
     */
    private boolean containsSqlInjection(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Encode HTML special characters
     */
    private String encodeHtmlCharacters(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    /**
     * Remove SQL injection patterns
     */
    private String removeSqlInjectionPatterns(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return SQL_INJECTION_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Remove path traversal patterns
     */
    private String removePathTraversalPatterns(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return PATH_TRAVERSAL_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Check for path traversal
     */
    private boolean containsPathTraversal(String input) {
        return PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Detect file type from extension
     */
    private String detectFileType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else if (fileName.endsWith(".xml")) {
            return "application/xml";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".csv")) {
            return "text/csv";
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        }
        return "application/octet-stream";
    }

    /**
     * Check if file type is in allowed list
     */
    private boolean isAllowedFileType(String type, List<String> allowedTypes) {
        return allowedTypes.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(type) || type.matches(allowed.replace("*", ".*")));
    }

    /**
     * Check if file type is in blocked list
     */
    private boolean isBlockedFileType(String type, List<String> blockedTypes) {
        return blockedTypes.stream().anyMatch(blocked -> blocked.equalsIgnoreCase(type) || type.matches(blocked.replace("*", ".*")));
    }

    /**
     * Check for dangerous file extensions
     */
    private boolean isDangerousExtension(String fileName) {
        String[] dangerousExtensions = {".exe", ".bat", ".cmd", ".sh", ".ps1", ".vbs", ".js", ".jar", ".com", ".dll", ".sys"};
        String lowerName = fileName.toLowerCase();
        for (String ext : dangerousExtensions) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Basic JSON syntax validation
     */
    private boolean isValidJsonSyntax(String json) {
        try {
            int balance = 0;
            boolean inString = false;
            boolean escaped = false;

            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    continue;
                }

                if (inString) {
                    continue;
                }

                switch (c) {
                    case '{':
                    case '[':
                        balance++;
                        break;
                    case '}':
                    case ']':
                        balance--;
                        if (balance < 0) {
                            return false;
                        }
                        break;
                }
            }

            return balance == 0 && !inString;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Estimate JSON nesting depth
     */
    private int estimateJsonDepth(String json) {
        int maxDepth = 0;
        int currentDepth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == '{' || c == '[') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}' || c == ']') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * Find missing required fields in JSON
     */
    private List<String> findMissingRequiredFields(String json, List<String> requiredFields) {
        List<String> missing = new ArrayList<>();
        for (String field : requiredFields) {
            String fieldPattern = "\"" + Pattern.quote(field) + "\"\\s*:";
            if (!Pattern.compile(fieldPattern).matcher(json).find()) {
                missing.add(field);
            }
        }
        return missing;
    }
}
