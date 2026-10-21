package com.kindred.therapist;

import java.time.Duration;
import java.time.Instant;

/**
 * Directories fail people when listings say "accepting new clients" but the therapist filled up
 * months ago. Someone who is struggling may contact five therapists and hear nothing back.
 *
 * So availability expires:
 *   0–30 days since confirmed  → FRESH   (ranked a little higher)
 *   31–60 days                 → AGING   (shown, therapist is reminded to confirm)
 *   over 60 days               → the nightly job marks the listing "not accepting" until confirmed
 */
public final class AvailabilityPolicy {

    public static final int FRESH_DAYS = 30;
    public static final int PAUSE_AFTER_DAYS = 60;

    public enum Freshness { FRESH, AGING, STALE }

    private AvailabilityPolicy() {
    }

    public static long daysSince(Instant confirmedAt, Instant now) {
        return Math.max(0, Duration.between(confirmedAt, now).toDays());
    }

    public static Freshness freshness(Instant confirmedAt, Instant now) {
        long days = daysSince(confirmedAt, now);
        if (days <= FRESH_DAYS) {
            return Freshness.FRESH;
        }
        return days <= PAUSE_AFTER_DAYS ? Freshness.AGING : Freshness.STALE;
    }
}
