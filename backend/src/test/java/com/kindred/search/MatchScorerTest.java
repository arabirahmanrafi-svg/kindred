package com.kindred.search;

import com.kindred.domain.Account;
import com.kindred.domain.Role;
import com.kindred.domain.TherapistProfile;
import com.kindred.search.MatchScorer.Match;
import com.kindred.therapist.AvailabilityPolicy.Freshness;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests: no database, no Spring. */
class MatchScorerTest {

    private final Instant now = Instant.now();

    @Test
    void insuranceIsAHardFilter() {
        TherapistProfile t = therapist(Set.of("English"), Set.of("Aetna"));
        assertThat(MatchScorer.score(t, criteria().insurance("Cigna").build(), now)).isEmpty();
        assertThat(MatchScorer.score(t, criteria().insurance("Aetna").build(), now)).isPresent();
    }

    @Test
    void budgetFilter_allowsSlidingScaleAndUnlistedFees() {
        TherapistProfile expensive = therapist(Set.of("English"), Set.of());
        expensive.setFeeMin(200);
        TherapistProfile sliding = therapist(Set.of("English"), Set.of());
        sliding.setFeeMin(200);
        sliding.setSlidingScale(true);
        TherapistProfile unlisted = therapist(Set.of("English"), Set.of());
        unlisted.setFeeMin(null);

        SearchCriteria budget80 = criteria().maxFee(80).build();
        assertThat(MatchScorer.score(expensive, budget80, now)).isEmpty();
        assertThat(MatchScorer.score(sliding, budget80, now)).isPresent();
        assertThat(MatchScorer.score(unlisted, budget80, now)).isPresent();
    }

    @Test
    void languageMatch_outranksSpecialtyMatch() {
        TherapistProfile speaksBengali = therapist(Set.of("English", "Bengali"), Set.of());
        TherapistProfile anxietyOnly = therapist(Set.of("English"), Set.of());
        anxietyOnly.replaceSpecialties(Set.of("Anxiety"));
        speaksBengali.replaceSpecialties(Set.of("Grief & loss"));

        SearchCriteria c = criteria().languages("Bengali").specialties("Anxiety").build();
        int bengaliScore = MatchScorer.score(speaksBengali, c, now).orElseThrow().score();
        int anxietyScore = MatchScorer.score(anxietyOnly, c, now).orElseThrow().score();

        assertThat(bengaliScore).isGreaterThan(anxietyScore);
    }

    @Test
    void explainsMatchesAndWhatIsMissing() {
        TherapistProfile t = therapist(Set.of("English", "Bengali"), Set.of("Aetna"));
        t.replaceCommunities(Set.of("South Asian"));

        Match m = MatchScorer.score(t, criteria().languages("Bengali", "Korean").communities("South Asian")
                .insurance("Aetna").build(), now).orElseThrow();

        assertThat(m.reasons()).contains("Speaks Bengali", "Experience with South Asian clients", "Accepts Aetna");
        assertThat(m.missing()).containsExactly("Language: Korean");
        assertThat(m.matchedPreferences()).isEqualTo(2);
        assertThat(m.requestedPreferences()).isEqualTo(3);
    }

    @Test
    void recentlyConfirmedAvailability_ranksAboveStale() {
        TherapistProfile fresh = therapist(Set.of("English"), Set.of());
        TherapistProfile stale = therapist(Set.of("English"), Set.of());
        stale.confirmAvailability(true, now.minus(Duration.ofDays(90)));

        Match freshMatch = MatchScorer.score(fresh, criteria().build(), now).orElseThrow();
        Match staleMatch = MatchScorer.score(stale, criteria().build(), now).orElseThrow();

        assertThat(freshMatch.freshness()).isEqualTo(Freshness.FRESH);
        assertThat(staleMatch.freshness()).isEqualTo(Freshness.STALE);
        assertThat(freshMatch.score()).isGreaterThan(staleMatch.score());
    }

    @Test
    void notAcceptingNewClients_isPushedDown() {
        TherapistProfile open = therapist(Set.of("English"), Set.of());
        TherapistProfile full = therapist(Set.of("English", "Bengali"), Set.of());
        full.confirmAvailability(false, now);

        SearchCriteria c = criteria().languages("Bengali").includeNotAccepting().build();
        // Even with a language match, a full therapist ranks below an open one without it.
        assertThat(MatchScorer.score(full, c, now).orElseThrow().score())
                .isLessThan(MatchScorer.score(open, c, now).orElseThrow().score());
    }

    @Test
    void telehealthReason_mentionsTheLicensedState() {
        TherapistProfile t = therapist(Set.of("English"), Set.of());
        t.setOffersInPerson(false);
        Optional<Match> m = MatchScorer.score(t, criteria().state("MI").build(), now);
        assertThat(m.orElseThrow().reasons()).contains("Telehealth (licensed in MI)");
    }

    // ---------- helpers ----------

    private TherapistProfile therapist(Set<String> languages, Set<String> insurances) {
        TherapistProfile t = new TherapistProfile(new Account("t@example.org", "x", Role.THERAPIST));
        t.setFullName("T");
        t.replaceLanguages(languages);
        t.replaceInsurances(insurances);
        t.replaceStatesLicensed(Set.of("MI"));
        t.setOffersInPerson(true);
        t.setOffersTelehealth(true);
        t.setPracticeCity("Ypsilanti");
        t.setPracticeState("MI");
        t.setFeeMin(100);
        t.confirmAvailability(true, now);
        return t;
    }

    private static CriteriaBuilder criteria() {
        return new CriteriaBuilder();
    }

    private static final class CriteriaBuilder {
        private String state = "";
        private Set<String> languages = Set.of();
        private Set<String> communities = Set.of();
        private Set<String> specialties = Set.of();
        private String insurance;
        private Integer maxFee;
        private boolean includeNotAccepting;

        CriteriaBuilder state(String s) { state = s; return this; }
        CriteriaBuilder languages(String... v) { languages = Set.of(v); return this; }
        CriteriaBuilder communities(String... v) { communities = Set.of(v); return this; }
        CriteriaBuilder specialties(String... v) { specialties = Set.of(v); return this; }
        CriteriaBuilder insurance(String v) { insurance = v; return this; }
        CriteriaBuilder maxFee(int v) { maxFee = v; return this; }
        CriteriaBuilder includeNotAccepting() { includeNotAccepting = true; return this; }

        SearchCriteria build() {
            return new SearchCriteria(state, true, true, languages, communities, specialties,
                    insurance, maxFee, includeNotAccepting);
        }
    }
}
