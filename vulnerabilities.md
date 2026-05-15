# Security Vulnerabilities Report

This document identifies security vulnerabilities found in the auction application codebase, along with recommended fixes.

---

## Table of Contents

1. [Critical/High Severity](#criticalhigh-severity)
2. [Medium Severity](#medium-severity)
3. [Low/Medium Severity](#lowmedium-severity)

---

## Critical/High Severity

### 1. Weak Password Hashing (No Salt)

**Location:** `src/main/java/com/auction/server/util/SecurityUtil.java`

**Issue:** Passwords are hashed using plain SHA-256 without salt, making them vulnerable to rainbow table attacks and brute-force cracking.

```java
// Current vulnerable code (lines 17-28)
public static String hashPassword(String password) {
    try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        // ... hex encoding
        return sb.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 not available", e);
    }
}
```

**Impact:** If the database is compromised, all user passwords can be cracked quickly using pre-computed rainbow tables.

**Recommendation:**
- Use bcrypt, scrypt, or Argon2 for password hashing
- Add a unique random salt per user
- Use established libraries like `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`

**Fix Example:**
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

public static String hashPassword(String password) {
    return encoder.encode(password);
}

public static boolean verifyPassword(String rawPassword, String encodedHash) {
    return encoder.matches(rawPassword, encodedHash);
}
```

---

### 2. Hardcoded Default Credentials

**Location:** `src/main/java/com/auction/shared/Constants.java` (lines 43-44)

**Issue:** Default admin credentials are hardcoded in source code and publicly visible.

```java
// Vulnerable code
public static final String DEFAULT_ADMIN_USERNAME = "abelmekonen";
public static final String DEFAULT_ADMIN_PASSWORD = "demo123";
```

**Impact:** Anyone with access to the codebase knows the admin credentials. If deployed to production without changing these defaults, the system is immediately compromised.

**Recommendation:**
- Remove hardcoded credentials from source code
- Generate random admin password on first startup
- Store credentials in environment variables or secure configuration files
- Force password change on first login

**Fix Example:**
```java
// Remove these constants entirely
// Instead, in UserRepository.seedDefaultAdmin():
private void seedDefaultAdmin() {
    // Check if admin exists via env var or generate random
    String adminUsername = System.getenv("ADMIN_USERNAME");
    String adminPassword = System.getenv("ADMIN_PASSWORD");
    
    if (adminUsername == null || adminPassword == null) {
        // Generate random password and log it securely once
        adminPassword = generateSecureRandomPassword();
        logger.warn("Generated admin password (CHANGE IMMEDIATELY): " + adminPassword);
    }
    // ... rest of seeding logic
}
```

---

### 3. Path Traversal Vulnerability

**Location:** `src/main/java/com/auction/server/core/ImageStore.java` (lines 64-74)

**Issue:** Image loading does not validate that the path stays within the intended directory, allowing potential path traversal attacks.

```java
// Vulnerable code
private byte[] readBytes(String pathStr) {
    if (pathStr == null || pathStr.isEmpty()) return new byte[0];
    try {
        Path path = Paths.get(pathStr);  // No validation!
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
    } catch (IOException e) {
        System.err.println("Failed to read image: " + pathStr);
    }
    return new byte[0];
}
```

**Impact:** An attacker could potentially read arbitrary files from the server filesystem by manipulating image paths stored in the database.

**Recommendation:**
- Validate that resolved paths are within allowed directories
- Use `Path.normalize()` and check prefix
- Implement allowlist-based path validation

**Fix Example:**
```java
private static final Path IMAGES_DIR_PATH = Paths.get("data/images").toAbsolutePath().normalize();

private byte[] readBytes(String pathStr) {
    if (pathStr == null || pathStr.isEmpty()) return new byte[0];
    try {
        Path path = Paths.get(pathStr).toAbsolutePath().normalize();
        
        // Validate path is within allowed directory
        if (!path.startsWith(IMAGES_DIR_PATH) && !path.startsWith(THUMBS_DIR_PATH)) {
            logger.warn("Attempted path traversal: " + pathStr);
            return new byte[0];
        }
        
        if (Files.exists(path)) {
            return Files.readAllBytes(path);
        }
    } catch (IOException e) {
        logger.error("Failed to read image: " + pathStr, e);
    }
    return new byte[0];
}
```

---

### 4. Incomplete Authorization Checks

**Location:** `src/main/java/com/auction/server/service/AuctionServiceImpl.java` (lines 119-139)

**Issue:** Admin functions only verify username matches an admin role, but don't validate session tokens or authentication state. This allows any client to impersonate admin by providing admin username.

```java
// Vulnerable code - only checks username
@Override
public void createUser(String adminUsername, String newUsername, String password, String role)
        throws RemoteException, AuctionException {
    User admin = userRepo.findUserByUsername(adminUsername);
    if (admin == null || !Constants.ADMIN.equals(admin.getRoleType())) {
        throw new AuctionException("Only admins can create users");
    }
    // ... no session validation!
}

@Override
public List<User> getAllUsers(String adminUsername) throws RemoteException, AuctionException {
    User admin = userRepo.findUserByUsername(adminUsername);
    if (admin == null || !Constants.ADMIN.equals(admin.getRoleType())) {
        throw new AuctionException("Only admins can view all users");
    }
    return userRepo.findAllUsers();
}
```

**Impact:** Any authenticated user (or even unauthenticated if they can call RMI directly) can perform admin actions by simply providing an admin username.

**Recommendation:**
- Implement proper session management with session tokens
- Validate session tokens on every privileged operation
- Track logged-in sessions server-side
- Use RMI client authentication

**Fix Example:**
```java
// Add session management
private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

public SessionToken login(String username, String password) {
    User u = userRepo.findUserByUsername(username);
    if (u != null && verifyPassword(password, u.getPasswordHash())) {
        String tokenId = generateSecureToken();
        activeSessions.put(tokenId, new Session(u, Instant.now()));
        return new SessionToken(tokenId, u.getRoleType());
    }
    return null;
}

@Override
public void createUser(String sessionToken, String newUsername, String password, String role)
        throws RemoteException, AuctionException {
    Session session = validateSession(sessionToken);
    if (session == null || !Constants.ADMIN.equals(session.getUser().getRoleType())) {
        throw new AuctionException("Unauthorized: Admin privileges required");
    }
    // ... proceed with user creation
}
```

---

### 5. Password Hash Exposure in API

**Location:** `src/main/java/com/auction/server/repository/UserRepository.java` (lines 66-82)

**Issue:** The `findAllUsers()` method returns password hashes to the caller, which then exposes them through the RMI service.

```java
// Vulnerable code
public List<User> findAllUsers() {
    List<User> users = new ArrayList<>();
    try (var stmt = connection.createStatement();
         var rs = stmt.executeQuery("SELECT username, password_hash, role FROM users")) {
        while (rs.next()) {
            String u = rs.getString("username");
            String p = rs.getString("password_hash");  // Exposing hash!
            String r = rs.getString("role");
            if (Constants.ADMIN.equals(r)) users.add(new Admin(u, p));
            // ...
        }
    }
    return users;
}
```

**Impact:** Password hashes are transmitted over the network and exposed to clients, enabling offline brute-force attacks even without database access.

**Recommendation:**
- Never return password hashes in API responses
- Create separate DTOs (Data Transfer Objects) that exclude sensitive fields
- Only return necessary user information (username, role)

**Fix Example:**
```java
// Create a DTO class
public class UserDTO {
    private String username;
    private String role;
    // NO password field
}

// Modify repository to return DTOs
public List<UserDTO> findAllUsers() {
    List<UserDTO> users = new ArrayList<>();
    try (var stmt = connection.createStatement();
         var rs = stmt.executeQuery("SELECT username, role FROM users")) {  // No password_hash!
        while (rs.next()) {
            UserDTO dto = new UserDTO();
            dto.setUsername(rs.getString("username"));
            dto.setRole(rs.getString("role"));
            users.add(dto);
        }
    }
    return users;
}
```

---

## Medium Severity

### 6. Race Condition in Bidding Logic

**Location:** `src/main/java/com/auction/server/core/AuctionManager.java` (lines 43-67)

**Issue:** The `placeBid` method performs check-then-act operations without proper transaction isolation, creating race conditions where two concurrent bids could both succeed.

```java
// Vulnerable pattern
public void placeBid(int auctionId, String bidderUsername, double amount, double clientExpectedPrice) 
        throws AuctionException {
    
    AuctionItem item = auctionRepo.findAuctionById(auctionId);  // READ
    // ... multiple validations on item
    
    // Time gap where another thread could modify
    applySnipeProtection(item);
    
    auctionRepo.updateAuctionBid(auctionId, amount, bidderUsername);  // WRITE
    bidRepo.insertBid(bid);
}
```

**Impact:** Two users could simultaneously place bids, both passing validation, resulting in inconsistent state or financial loss.

**Recommendation:**
- Use database transactions with proper isolation levels
- Implement optimistic locking with version numbers
- Use SELECT FOR UPDATE for critical sections
- Add per-auction locks (already mentioned in comments but not fully implemented)

**Fix Example:**
```java
public synchronized void placeBid(int auctionId, String bidderUsername, double amount, double clientExpectedPrice) 
        throws AuctionException {
    // Or use database-level locking:
    // Connection.setAutoCommit(false);
    // SELECT ... FOR UPDATE
    // ... validations and updates
    // Connection.commit();
}

// Better: Use optimistic locking
public void placeBid(int auctionId, String bidderUsername, double amount, long expectedVersion) 
        throws AuctionException, OptimisticLockException {
    int updated = auctionRepo.updateAuctionBidWithVersion(auctionId, amount, bidderUsername, expectedVersion);
    if (updated == 0) {
        throw new OptimisticLockException("Concurrent modification detected");
    }
}
```

---

### 7. Missing Input Validation for Images

**Location:** `src/main/java/com/auction/server/core/ImageStore.java` and `src/main/java/com/auction/shared/Constants.java`

**Issue:** While `MAX_IMAGE_SIZE_BYTES` constant exists (2MB), there's no validation enforcing this limit before saving images to disk.

```java
// In Constants.java - constant exists
public static final long MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB

// In ImageStore.java - but never validated!
private String saveToDisk(int auctionId, int index, byte[] data, boolean generateThumb) {
    if (data == null || data.length == 0) return null;
    // No size check!
    // No content type validation!
    String filename = auctionId + "_" + index + ".jpg";
    // ...
}
```

**Impact:** Attackers could upload extremely large files causing disk exhaustion (DoS) or upload malicious files disguised as images.

**Recommendation:**
- Validate image size against MAX_IMAGE_SIZE_BYTES before processing
- Validate image content type (magic bytes, not just extension)
- Sanitize filenames
- Implement rate limiting on uploads

**Fix Example:**
```java
private String saveToDisk(int auctionId, int index, byte[] data, boolean generateThumb) {
    if (data == null || data.length == 0) return null;
    
    // Validate size
    if (data.length > Constants.MAX_IMAGE_SIZE_BYTES) {
        throw new IllegalArgumentException("Image exceeds maximum size of 2MB");
    }
    
    // Validate image format (check magic bytes)
    if (!isValidImageFormat(data)) {
        throw new IllegalArgumentException("Invalid image format");
    }
    
    // Sanitize filename
    String filename = sanitizeFilename(auctionId + "_" + index + ".jpg");
    // ...
}

private boolean isValidImageFormat(byte[] data) {
    // Check JPEG magic bytes: FF D8 FF
    return data.length >= 3 && 
           data[0] == (byte)0xFF && 
           data[1] == (byte)0xD8 && 
           data[2] == (byte)0xFF;
}
```

---

### 8. Insecure RMI Configuration

**Location:** `src/main/java/com/auction/server/service/AuctionServiceImpl.java` and RMI setup files

**Issue:** RMI registry has no authentication mechanism, allowing any client to connect and invoke methods.

**Impact:** Unauthorized remote code execution, data exfiltration, or service manipulation.

**Recommendation:**
- Enable RMI SSL/TLS encryption
- Implement custom socket factories with authentication
- Use Java Authentication and Authorization Service (JAAS)
- Consider migrating to REST/gRPC with proper auth

**Fix Example:**
```java
// Configure RMI with SSL
System.setProperty("javax.net.ssl.keyStore", "server.keystore");
System.setProperty("javax.net.ssl.keyStorePassword", "keystorePass");
System.setProperty("javax.net.ssl.trustStore", "server.truststore");
System.setProperty("javax.net.ssl.trustStorePassword", "truststorePass");

// Use custom socket factory with authentication
LocateRegistry.createRegistry(port, 
    new SslRMIClientSocketFactory(), 
    new SslRMIServerSocketFactory());
```

---

## Low/Medium Severity

### 9. No Rate Limiting

**Location:** Application-wide (no implementation found)

**Issue:** No rate limiting on login attempts, bidding, or other operations.

**Impact:** Vulnerable to brute-force attacks on passwords and denial-of-service attacks.

**Recommendation:**
- Implement rate limiting on authentication endpoints (e.g., max 5 attempts per minute)
- Add CAPTCHA after failed attempts
- Implement exponential backoff
- Use token bucket or sliding window algorithms

**Fix Example:**
```java
private final Map<String, RateLimiter> loginLimiters = new ConcurrentHashMap<>();

public User login(String username, String password) {
    RateLimiter limiter = loginLimiters.computeIfAbsent(username, k -> new RateLimiter(5, 60));
    
    if (!limiter.tryAcquire()) {
        throw new LockedException("Too many login attempts. Try again later.");
    }
    
    // ... proceed with authentication
}
```

---

### 10. Missing Audit Logging

**Location:** `src/main/java/com/auction/server/service/AuctionServiceImpl.java` (lines 113-114, 143-144, 150-151)

**Issue:** Critical actions (user creation, database backup, auction modifications) have TODO comments indicating missing audit logging.

```java
// TODO comments indicating missing functionality
@Override
public byte[] exportAuctionsToCSV(String sellerUsername) throws RemoteException {
    // TODO: This should probably go to a ReportingManager deep module
    return new byte[0];
}

@Override
public byte[] backupDatabase(String adminUsername) throws RemoteException, AuctionException {
    // TODO: delegate to a SystemManager
    return new byte[0];
}

@Override
public List<String> getAuditLogs(String adminUsername, int lastNLines)
        throws RemoteException, AuctionException {
    // TODO: delegate to SystemManager/AuditLog
    return Collections.emptyList();
}
```

**Impact:** No accountability or forensic capability in case of security incidents.

**Recommendation:**
- Implement comprehensive audit logging for all security-relevant events
- Log: authentication attempts, privilege escalations, data modifications, admin actions
- Include timestamp, user, action, IP address, and outcome
- Store logs in tamper-evident storage

**Fix Example:**
```java
private final AuditLogger auditLogger = new AuditLogger();

@Override
public void createUser(String sessionToken, String newUsername, String password, String role)
        throws RemoteException, AuctionException {
    Session session = validateSession(sessionToken);
    
    try {
        // ... user creation logic
        
        auditLogger.logSuccess(session.getUser().getUsername(), 
                              "USER_CREATED", 
                              "Created user: " + newUsername + " with role: " + role);
    } catch (Exception e) {
        auditLogger.logFailure(session.getUser().getUsername(), 
                              "USER_CREATE_FAILED", 
                              "Failed to create user: " + newUsername);
        throw e;
    }
}
```

---

### 11. Session Management Issues

**Location:** `src/main/java/com/auction/server/service/AuctionServiceImpl.java` (login method, lines 42-51)

**Issue:** Login returns User object directly with no session tracking. No session invalidation mechanism exists.

**Impact:** Easy session hijacking, no way to force logout, no session expiration.

**Recommendation:**
- Implement proper session tokens
- Add session expiration (absolute and idle timeout)
- Provide logout functionality to invalidate sessions
- Track active sessions per user

---

### 12. SQL Injection Risk (Potential)

**Location:** Throughout repository classes

**Note:** Current code uses prepared statements correctly, which is good. However, ensure this pattern is maintained as the codebase grows.

**Current Good Practice:**
```java
// Correct usage of prepared statements
String sql = "SELECT password_hash, role FROM users WHERE username = ?";
try (var pstmt = connection.prepareStatement(sql)) {
    pstmt.setString(1, username);
    // ...
}
```

**Recommendation:** Continue using prepared statements exclusively. Never concatenate user input into SQL queries.

---

## Summary Priority Matrix

| Priority | Vulnerability | Effort | Impact |
|----------|--------------|--------|--------|
| P0 | Hardcoded Credentials | Low | Critical |
| P0 | Weak Password Hashing | Medium | Critical |
| P0 | Incomplete Authorization | High | Critical |
| P1 | Password Hash Exposure | Medium | High |
| P1 | Path Traversal | Medium | High |
| P1 | Race Conditions | High | High |
| P2 | Missing Input Validation | Low | Medium |
| P2 | Insecure RMI | High | Medium |
| P2 | No Rate Limiting | Medium | Medium |
| P3 | Missing Audit Logs | Medium | Low |
| P3 | Session Management | High | Medium |

---

## Next Steps

1. **Immediate (P0):** Fix hardcoded credentials and implement proper password hashing
2. **Short-term (P1):** Implement session management and fix authorization
3. **Medium-term (P2):** Add input validation, rate limiting, and secure RMI
4. **Long-term (P3):** Complete audit logging and session improvements
