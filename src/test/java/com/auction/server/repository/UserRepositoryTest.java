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
    void shouldFindDefaultAdminByUsername() {
        User admin = userRepo.findUserByUsername(Constants.DEFAULT_ADMIN_USERNAME);
        
        assertNotNull(admin, "Should find the default admin user");
        assertEquals(Constants.DEFAULT_ADMIN_USERNAME, admin.getUsername());
        assertEquals(Constants.ADMIN, admin.getRoleType());
    }
}
