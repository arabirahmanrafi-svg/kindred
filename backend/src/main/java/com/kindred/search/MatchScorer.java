package com.kindred.search;

import com.kindred.domain.TherapistProfile;
import com.kindred.therapist.AvailabilityPolicy;
import com.kindred.therapist.AvailabilityPolicy.Freshness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether a therapist fits a search, how well, and WHY.
 *
 * Two kinds of criteria:
 *  - Hard (must pass, or the therapist is not shown): insurance and budget.
 *    Location/licensing rules are hard too, but the database already applied them.
 *  - Soft (scored): language, community, specialty. A therapist who matches 2 of 3 is still
 *    useful to show, ranked below one who matches all 3.
 *
 * Every result carries plain-language reasons ("Speaks Bengali", "Accepts Aetna"), so people
 * can see why a match was suggested instead of trusting a black-box ranking.
 *
 * This class has no database or Spring dependencies, so it is fully unit-testable.
 */
public final class MatchScorer {

    static final int LANGUAGE_POINTS = 30;   // speaking your language is often essential
    static final int COMMUNITY_POINTS = 15;
    static final int SPECIALTY_POINTS = 10;
    static final int FRESH_BONUS = 5;
    static final int STALE_PENALTY = 10;
    static final int NOT_ACCEPTING_PENALTY = 40;

    public record Match(
            int score,
            int matchedPreferences,
            int requestedPreferences,
            List<String> reasons,
            List<String> missing,
            Freshness freshness
    ) {
    }

    private MatchScorer() {
    }

    public static Optional<Match> score(TherapistProfile t, SearchCriteria c, Instant now) {
        // ----- Hard filters -----
        if (c.insurance() != null && !t.getInsurances().contains(c.insurance())) {
            return Optional.empty();
        }
        if (c.maxFee() != null && !affordable(t, c.maxFee())) {
            return Optional.empty();
        }

        List<String> reasons = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int score = 0;
        int matched = 0;

        // ----- Where / how -----
        reasons.addAll(formatReasons(t, c));

        // ----- Soft preferences -----
        List<String> langs = intersect(c.languages(), t.getLanguages());
        score += langs.size() * LANGUAGE_POINTS;
        matched += langs.size();
        if (!langs.isEmpty()) {
            reasons.add("Speaks " + String.join(", ", langs));
        }
        missing.addAll(prefixed("Language: ", difference(c.languages(), t.getLanguages())));

        List<String> comms = intersect(c.communities(), t.getCommunities());
        score += comms.size() * COMMUNITY_POINTS;
        matched += comms.size();
        if (!comms.isEmpty()) {
            reasons.add("Experience with " + String.join(", ", comms) + " clients");
        }
        missing.addAll(prefixed("Community: ", difference(c.communities(), t.getCommunities())));

        List<String> specs = intersect(c.specialties(), t.getSpecialties());
        score += specs.size() * SPECIALTY_POINTS;
        matched += specs.size();
        if (!specs.isEmpty()) {
            reasons.add("Helps with " + String.join(", ", specs));
        }
        missing.addAll(prefixed("Focus: ", difference(c.specialties(), t.getSpecialties())));

        // ----- Cost -----
        if (c.insurance() != null) {
            reasons.add("Accepts " + c.insurance());
        }
        if (c.maxFee() != null) {
            if (t.isSlidingScale()) {
                reasons.add("Offers sliding-scale fees");
            } else if (t.getFeeMin() != null) {
                reasons.add("Fees from $" + t.getFeeMin());
            } else {
                missing.add("Fee: not listed, ask when you reach out");
            }
        }

        // ----- Availability -----
        Freshness freshness = AvailabilityPolicy.freshness(t.getAvailabilityConfirmedAt(), now);
        if (!t.isAcceptingNewClients()) {
            score -= NOT_ACCEPTING_PENALTY;
        } else if (freshness == Freshness.FRESH) {
            score += FRESH_BONUS;
        } else if (freshness == Freshness.STALE) {
            score -= STALE_PENALTY;
        }

        return Optional.of(new Match(score, matched, c.requestedPreferenceCount(), reasons, missing, freshness));
    }

    /** Sliding scale counts as affordable. A therapist with no listed fee is shown, not hidden. */
    static boolean affordable(TherapistProfile t, int maxFee) {
        return t.isSlidingScale() || t.getFeeMin() == null || t.getFeeMin() <= maxFee;
    }

    private static List<String> formatReasons(TherapistProfile t, SearchCriteria c) {
        List<String> out = new ArrayList<>();
        boolean anyState = c.state().isEmpty();
        if (c.wantInPerson() && t.isOffersInPerson()
                && (anyState || c.state().equals(t.getPracticeState()))) {
            out.add("In person in " + t.getPracticeCity() + ", " + t.getPracticeState());
        }
        if (c.wantTelehealth() && t.isOffersTelehealth()
                && (anyState || t.getStatesLicensed().contains(c.state()))) {
            out.add(anyState ? "Offers telehealth" : "Telehealth (licensed in " + c.state() + ")");
        }
        return out;
    }

    private static List<String> intersect(Set<String> wanted, Set<String> offered) {
        return wanted.stream().filter(offered::contains).sorted().toList();
    }

    private static List<String> difference(Set<String> wanted, Set<String> offered) {
        return wanted.stream().filter(w -> !offered.contains(w)).sorted().toList();
    }

    private static List<String> prefixed(String prefix, List<String> values) {
        return values.stream().map(v -> prefix + v).toList();
    }
}
