package com.kindred.repo;

import com.kindred.domain.TherapistProfile;
import com.kindred.domain.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TherapistProfileRepository extends JpaRepository<TherapistProfile, Long> {

    Optional<TherapistProfile> findByAccountId(Long accountId);

    Optional<TherapistProfile> findByIdAndVerificationStatus(Long id, VerificationStatus status);

    @Query("select p from TherapistProfile p join fetch p.account where p.verificationStatus = :status order by p.updatedAt asc")
    List<TherapistProfile> findForReview(@Param("status") VerificationStatus status);

    /**
     * The database does the "hard" filtering that every result must pass:
     *  - verified profiles only
     *  - accepting new clients (unless the user asks to include everyone)
     *  - the location rule:
     *      in person → the practice is in the user's state
     *      telehealth → the therapist is LICENSED in the user's state
     *        (a therapist may only treat a client who is located in a state where they hold a license)
     *
     * An empty string for state means "any state". Softer preferences (language, community,
     * specialty) are scored afterwards in MatchScorer so partial matches can still be shown.
     */
    @Query("""
            select distinct t from TherapistProfile t
             where t.verificationStatus = :verified
               and (:includeNotAccepting = true or t.acceptingNewClients = true)
               and (
                    (:wantInPerson = true and t.offersInPerson = true
                        and (:state = '' or t.practiceState = :state))
                 or (:wantTelehealth = true and t.offersTelehealth = true
                        and (:state = '' or :state member of t.statesLicensed))
               )
            """)
    List<TherapistProfile> findCandidates(@Param("verified") VerificationStatus verified,
                                          @Param("includeNotAccepting") boolean includeNotAccepting,
                                          @Param("wantInPerson") boolean wantInPerson,
                                          @Param("wantTelehealth") boolean wantTelehealth,
                                          @Param("state") String state);

    /** Used by the nightly job: listings nobody has confirmed in a long time stop showing as "accepting". */
    @Modifying
    @Query("""
            update TherapistProfile t set t.acceptingNewClients = false
             where t.acceptingNewClients = true and t.availabilityConfirmedAt < :cutoff
            """)
    int pauseUnconfirmedSince(@Param("cutoff") Instant cutoff);

    long countByDemoTrue();
}
