package com.kindred.search;

import com.kindred.TestProfiles;
import com.kindred.auth.AuthService;
import com.kindred.search.SearchService.Result;
import com.kindred.therapist.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The location rules, tested against the real database query.
 * (Sample data is turned off so only the profiles created here exist besides other tests' profiles,
 * so assertions check for specific profile ids rather than counts.)
 */
@SpringBootTest(properties = "kindred.seed-demo-data=false")
class SearchRulesTest {

    @Autowired AuthService auth;
    @Autowired ProfileService profileService;
    @Autowired SearchService searchService;

    @Test
    void telehealthOnlyShowsTherapistsLicensedInTheUsersState() {
        TestProfiles ohioOnly = TestProfiles.valid();
        ohioOnly.licenseState = "OH";
        ohioOnly.statesLicensed = Set.of("OH");
        ohioOnly.offersInPerson = false;
        long ohioTherapist = createVerified(ohioOnly);

        assertThat(ids(search("MI", false, true))).doesNotContain(ohioTherapist);
        assertThat(ids(search("OH", false, true))).contains(ohioTherapist);
    }

    @Test
    void therapistLicensedInTwoStates_appearsInBoth() {
        TestProfiles twoStates = TestProfiles.valid();
        twoStates.statesLicensed = Set.of("MI", "IL");
        twoStates.offersInPerson = false;
        long id = createVerified(twoStates);

        assertThat(ids(search("MI", false, true))).contains(id);
        assertThat(ids(search("IL", false, true))).contains(id);
        assertThat(ids(search("CA", false, true))).doesNotContain(id);
    }

    @Test
    void inPersonOnlyShowsOfficesInTheUsersState() {
        TestProfiles detroitOffice = TestProfiles.valid();
        detroitOffice.offersTelehealth = false;
        long id = createVerified(detroitOffice);

        assertThat(ids(search("MI", true, false))).contains(id);
        assertThat(ids(search("OH", true, false))).doesNotContain(id);
        // Telehealth-only search must not return an in-person-only therapist
        assertThat(ids(search("MI", false, true))).doesNotContain(id);
    }

    @Test
    void unverifiedProfilesNeverAppear() {
        long accountId = auth.registerTherapist(uniqueEmail(), "a-long-password-1").account().id();
        long pendingId = profileService.save(accountId, TestProfiles.valid().build()).profile().id();

        assertThat(ids(search("MI", true, true))).doesNotContain(pendingId);

        profileService.approve(pendingId);
        assertThat(ids(search("MI", true, true))).contains(pendingId);
    }

    @Test
    void notAcceptingIsHiddenUnlessRequested() {
        TestProfiles full = TestProfiles.valid();
        full.acceptingNewClients = false;
        long id = createVerified(full);

        assertThat(ids(search("MI", true, true))).doesNotContain(id);

        SearchCriteria includeAll = new SearchCriteria("MI", true, true, Set.of(), Set.of(), Set.of(), null, null, true);
        assertThat(ids(searchService.search(includeAll, 0, 50).results())).contains(id);
    }

    // ---------- helpers ----------

    private long createVerified(TestProfiles profile) {
        long accountId = auth.registerTherapist(uniqueEmail(), "a-long-password-1").account().id();
        long profileId = profileService.save(accountId, profile.build()).profile().id();
        profileService.approve(profileId);
        return profileId;
    }

    private List<Result> search(String state, boolean inPerson, boolean telehealth) {
        SearchCriteria c = new SearchCriteria(state, inPerson, telehealth, Set.of(), Set.of(), Set.of(), null, null, false);
        return searchService.search(c, 0, 50).results();
    }

    private static List<Long> ids(List<Result> results) {
        return results.stream().map(r -> r.therapist().id()).toList();
    }

    private static String uniqueEmail() {
        return "t-" + UUID.randomUUID() + "@example.org";
    }
}
