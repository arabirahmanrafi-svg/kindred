package com.kindred.search;

import com.kindred.domain.TherapistProfile;
import com.kindred.domain.VerificationStatus;
import com.kindred.repo.TherapistProfileRepository;
import com.kindred.search.MatchScorer.Match;
import com.kindred.therapist.ProfileDtos.PublicProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Search in two stages:
 *   1. The database returns candidates that pass the legal/location rules (indexed query).
 *   2. MatchScorer applies the remaining filters, scores each candidate, and explains the match.
 *
 * Scoring happens in Java because it's easy to read, test and change. That's fine for a
 * directory of a few thousand therapists. At much larger scale you'd move search to a
 * dedicated engine such as OpenSearch, which can score and paginate inside the index.
 */
@Service
public class SearchService {

    private final TherapistProfileRepository profiles;

    public SearchService(TherapistProfileRepository profiles) {
        this.profiles = profiles;
    }

    public record Result(PublicProfile therapist, Match match) {
    }

    public record SearchResponse(List<Result> results, int total, int page, int pageSize, int totalPages) {
    }

    private record Scored(TherapistProfile profile, Match match) {
    }

    @Transactional(readOnly = true)
    public SearchResponse search(SearchCriteria criteria, int page, int pageSize) {
        Instant now = Instant.now();

        List<TherapistProfile> candidates = profiles.findCandidates(
                VerificationStatus.VERIFIED, criteria.includeNotAccepting(),
                criteria.wantInPerson(), criteria.wantTelehealth(), criteria.state());

        List<Scored> ranked = candidates.stream()
                .map(p -> MatchScorer.score(p, criteria, now).map(m -> new Scored(p, m)).orElse(null))
                .filter(s -> s != null)
                .sorted(Comparator
                        .comparingInt((Scored s) -> s.match().score()).reversed()
                        .thenComparing(s -> s.profile().getAvailabilityConfirmedAt(), Comparator.reverseOrder())
                        .thenComparing(s -> s.profile().getId()))
                .toList();

        int total = ranked.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int from = Math.min(page * pageSize, total);
        int to = Math.min(from + pageSize, total);

        List<Result> pageResults = ranked.subList(from, to).stream()
                .map(s -> new Result(PublicProfile.from(s.profile()), s.match()))
                .toList();

        return new SearchResponse(pageResults, total, page, pageSize, totalPages);
    }
}
