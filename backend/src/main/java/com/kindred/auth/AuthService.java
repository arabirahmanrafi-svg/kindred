package com.kindred.auth;

import com.kindred.domain.Account;
import com.kindred.domain.AuthSession;
import com.kindred.domain.Role;
import com.kindred.error.ConflictException;
import com.kindred.error.ForbiddenException;
import com.kindred.error.UnauthorizedException;
import com.kindred.repo.AccountRepository;
import com.kindred.repo.AuthSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Token-based login without a full security framework, so every step is visible:
 *
 *   login  → check password with BCrypt → create random token → store SHA-256(token) → return token
 *   request→ "Authorization: Bearer <token>" → hash it → look up session → check expiry → who you are
 *   logout → delete the session row
 */
@Service
public class AuthService {

    private static final String BEARER = "Bearer ";

    private final AccountRepository accounts;
    private final AuthSessionRepository sessions;
    private final LoginThrottle throttle;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final Duration sessionLength;

    /**
     * Compared against when the email doesn't exist, so a wrong email takes as long as a wrong
     * password. Otherwise response timing would reveal which emails have accounts.
     */
    private final String dummyHash = passwords.encode("timing-equalizer-not-a-real-password");

    public AuthService(AccountRepository accounts, AuthSessionRepository sessions, LoginThrottle throttle,
                       @Value("${kindred.session-days:7}") int sessionDays) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.throttle = throttle;
        this.sessionLength = Duration.ofDays(sessionDays);
    }

    public record LoginResult(String token, CurrentAccount account) {
    }

    @Transactional
    public LoginResult registerTherapist(String email, String password) {
        String normalized = normalizeEmail(email);
        if (accounts.existsByEmail(normalized)) {
            throw new ConflictException("An account with this email already exists. Try logging in.");
        }
        Account account = accounts.save(new Account(normalized, passwords.encode(password), Role.THERAPIST));
        return startSession(account);
    }

    @Transactional
    public LoginResult login(String email, String password) {
        String normalized = normalizeEmail(email);

        Instant lockedUntil = throttle.lockedUntil(normalized);
        if (lockedUntil != null) {
            String until = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault()).format(lockedUntil);
            throw new UnauthorizedException("Too many wrong attempts. Try again after " + until + ".");
        }

        Account account = accounts.findByEmail(normalized).orElse(null);
        String hashToCheck = account == null ? dummyHash : account.getPasswordHash();
        boolean passwordOk = passwords.matches(password, hashToCheck);

        if (account == null || !passwordOk) {
            throttle.recordFailure(normalized);
            throw new UnauthorizedException("Email or password is incorrect");
        }
        throttle.recordSuccess(normalized);
        return startSession(account);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token != null) {
            sessions.deleteByHash(Tokens.sha256(token));
        }
    }

    /** Throws 401 unless the request carries a valid, unexpired session token. */
    @Transactional(readOnly = true)
    public CurrentAccount require(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            throw new UnauthorizedException("Please log in");
        }
        AuthSession session = sessions.findWithAccount(Tokens.sha256(token))
                .orElseThrow(() -> new UnauthorizedException("Your session has ended. Please log in again."));
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Your session has ended. Please log in again.");
        }
        Account a = session.getAccount();
        return new CurrentAccount(a.getId(), a.getEmail(), a.getRole());
    }

    /** 401 if not logged in, 403 if logged in with the wrong role. */
    public CurrentAccount requireRole(String authorizationHeader, Role role) {
        CurrentAccount current = require(authorizationHeader);
        if (current.role() != role) {
            throw new ForbiddenException("You don't have access to this page");
        }
        return current;
    }

    /** Creates the admin account on startup if it doesn't exist yet. */
    @Transactional
    public boolean ensureAdmin(String email, String password) {
        String normalized = normalizeEmail(email);
        if (accounts.existsByEmail(normalized)) {
            return false;
        }
        accounts.save(new Account(normalized, passwords.encode(password), Role.ADMIN));
        return true;
    }

    /** Hourly cleanup so the sessions table doesn't grow forever. */
    @Scheduled(cron = "0 15 * * * *")
    @Transactional
    public void deleteExpiredSessions() {
        sessions.deleteExpired(Instant.now());
    }

    private LoginResult startSession(Account account) {
        String token = Tokens.newToken();
        sessions.save(new AuthSession(Tokens.sha256(token), account, Instant.now().plus(sessionLength)));
        return new LoginResult(token, new CurrentAccount(account.getId(), account.getEmail(), account.getRole()));
    }

    private static String extractToken(String header) {
        if (header == null || !header.startsWith(BEARER)) {
            return null;
        }
        String token = header.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
