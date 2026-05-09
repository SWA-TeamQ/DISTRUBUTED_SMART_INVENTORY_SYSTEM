package com.auction.server.repository;

import com.auction.shared.Constants;
import com.auction.shared.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private DatabaseManager dbManager;
    private UserRepository userRepo;

    @BeforeEach
    void setUp() {
        dbManager = new DatabaseManager("jdbc:sqlite::memory:");
        userRepo = new UserRepository(dbManager.getConnection());
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
    }

    @Test
    void shouldCreateSchemaAndSeedDefaultAdminOnInitialization() {
        List<User> users = userRepo.findAllUsers();
        assertEquals(1, users.size(), "Should have exactly one user (default admin)");
        
        User admin = users.get(0);
        assertEquals(Constants.DEFAULT_ADMIN_USERNAME, admin.getUsername(), "Username should match default admin");
        assertEquals(Constants.ADMIN, admin.getRoleType(), "Role should be ADMIN");
        assertNotNull(admin.getPasswordHash());
        assertFalse(admin.getPasswordHash().isEmpty());
    }
}
