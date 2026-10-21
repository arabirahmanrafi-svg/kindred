package com.kindred.auth;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after 5 wrong passwords for an email, that email is locked
 * for 15 minutes.
 *
 * This lives in memory, which is fine for one server. With several servers you'd keep the
 * counters in Redis so every server sees the same numbers.
 */
@Component
public class LoginThrottle {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant lockedUntil) {
    }

    private final Map<String, Attempts> byEmail = new ConcurrentHashMap<>();

    /** @return the time the lock ends, or null if login may be attempted now */
    public Instant lockedUntil(String email) {
        Attempts a = byEmail.get(email);
        if (a == null || a.lockedUntil() == null) {
            return null;
        }
        if (Instant.now().isAfter(a.lockedUntil())) {
            byEmail.remove(email);
            return null;
        }
        return a.lockedUntil();
    }

    public void recordFailure(String email) {
        byEmail.compute(email, (key, old) -> {
            int failures = (old == null ? 0 : old.failures()) + 1;
            Instant lock = failures >= MAX_FAILURES ? Instant.now().plus(LOCK_DURATION) : null;
            return new Attempts(failures, lock);
        });
    }

    public void recordSuccess(String email) {
        byEmail.remove(email);
    }
}
