package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private DatabaseManager dbManager;
    private UserRepository userRepo;
    private java.nio.file.Path tempDbPath;

    @BeforeEach
    void setUp() throws Exception {
        tempDbPath = java.nio.file.Files.createTempFile("rtdas-user-repo-", ".sqlite");
        dbManager = new DatabaseManager("jdbc:sqlite:" + tempDbPath.toAbsolutePath());
        userRepo = new UserRepository(dbManager.getConnection());
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
        if (tempDbPath != null) {
            try {
                java.nio.file.Files.deleteIfExists(tempDbPath);
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void shouldFindDefaultAdminByUsername() {
        User admin = userRepo.findUserByUsername(Constants.DEFAULT_ADMIN_USERNAME);
        
        assertNotNull(admin, "Should find the default admin user");
        assertEquals(Constants.DEFAULT_ADMIN_USERNAME, admin.getUsername());
        assertEquals(Constants.ADMIN, admin.getRoleType());
    }
}
