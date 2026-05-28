package com.auction.server.core;

import com.auction.server.repository.UserRepository;
import com.auction.shared.Constants;
import com.auction.shared.exceptions.AuctionException;
import com.auction.shared.exceptions.UnauthorizedException;
import com.auction.shared.models.User;
import com.auction.server.core.logging.AsyncLogger;
import com.auction.server.core.logging.LogCategory;
import com.auction.server.core.logging.EventType;
import com.auction.server.util.SecurityUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final UserRepository userRepo;

    private static class SessionInfo {
        final SessionContext context;
        Instant expiresAt;

        SessionInfo(SessionContext context, Instant expiresAt) {
            this.context = context;
            this.expiresAt = expiresAt;
        }
    }

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public SessionManager(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public String login(String username, String password) throws AuctionException {
        User u = userRepo.findUserByUsername(username);
        if (u != null) {
            String hash = SecurityUtil.hashPassword(password);
            if (u.getPasswordHash().equals(hash)) {
                String token = java.util.UUID.randomUUID().toString();
                SessionContext ctx = new SessionContext(username, u.getRoleType());
                sessions.put(token, new SessionInfo(ctx, Instant.now().plus(Duration.ofMinutes(Constants.SESSION_TTL_MINUTES))));
                AsyncLogger.log(LogCategory.SECURITY, EventType.LOGIN, "User=" + username);
                return token;
            }
        }
        AsyncLogger.log(LogCategory.SECURITY, EventType.LOGIN_FAILED, "Username=" + username);
        throw new AuctionException("Invalid username or password");
    }

    public void register(String username, String password, String role) throws AuctionException {
        if (userRepo.findUserByUsername(username) != null) {
            throw new AuctionException("Username already exists");
        }
        String hash = SecurityUtil.hashPassword(password);
        userRepo.insertUser(username, hash, role);
        AsyncLogger.log(LogCategory.SECURITY, EventType.USER_REGISTERED, "New User Registered: " + username + " Role=" + role);
    }

    public SessionContext validateSession(String token) throws AuctionException {
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("Session token is missing");
        }
        SessionInfo sessionInfo = sessions.get(token);
        if (sessionInfo == null) {
            throw new UnauthorizedException("Invalid session token");
        }
        if (Instant.now().isAfter(sessionInfo.expiresAt)) {
            sessions.remove(token);
            throw new UnauthorizedException("Session token has expired");
        }
        // Slide token expiration window
        sessionInfo.expiresAt = Instant.now().plus(Duration.ofMinutes(Constants.SESSION_TTL_MINUTES));
        return sessionInfo.context;
    }

    public SessionContext validateRole(String token, String... allowedRoles) throws AuctionException {
        SessionContext context = validateSession(token);
        boolean allowed = false;
        for (String role : allowedRoles) {
            if (role.equals(context.role())) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new UnauthorizedException("Access denied: insufficient permissions");
        }
        return context;
    }

    public void logout(String token) {
        SessionInfo sessionInfo = sessions.remove(token);
        if (sessionInfo != null) {
            AsyncLogger.log(LogCategory.SECURITY, EventType.LOGOUT, "User=" + sessionInfo.context.username());
        }
    }
}
