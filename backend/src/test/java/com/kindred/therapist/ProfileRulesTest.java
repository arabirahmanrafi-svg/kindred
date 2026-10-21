package com.kindred.therapist;

import com.kindred.TestProfiles;
import com.kindred.auth.AuthService;
import com.kindred.domain.TherapistProfile;
import com.kindred.domain.VerificationStatus;
import com.kindred.error.BadRequestException;
import com.kindred.error.NotFoundException;
import com.kindred.repo.TherapistProfileRepository;
import com.kindred.therapist.ProfileDtos.OwnProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "kindred.seed-demo-data=false")
class ProfileRulesTest {

    @Autowired AuthService auth;
    @Autowired ProfileService profileService;
    @Autowired TherapistProfileRepository repo;
    @Autowired AvailabilityJob availabilityJob;
    @Autowired TransactionTemplate tx;

    @Test
    void newProfileIsPendingAndHiddenUntilApproved() {
        long account = newAccount();
        OwnProfile own = profileService.save(account, TestProfiles.valid().build());

        assertThat(own.status()).isEqualTo(VerificationStatus.PENDING);
        assertThatThrownBy(() -> profileService.getPublic(own.profile().id())).isInstanceOf(NotFoundException.class);

        profileService.approve(own.profile().id());
        assertThat(profileService.getPublic(own.profile().id()).fullName()).isEqualTo("Test Therapist");
    }

    @Test
    void editingBio_keepsVerification_butChangingLicense_requiresReverification() {
        long account = newAccount();
        long id = profileService.save(account, TestProfiles.valid().build()).profile().id();
        profileService.approve(id);

        TestProfiles edited = TestProfiles.valid();
        edited.bio = "Updated bio";
        assertThat(profileService.save(account, edited.build()).status()).isEqualTo(VerificationStatus.VERIFIED);

        edited.licenseNumber = "DIFFERENT-999";
        OwnProfile after = profileService.save(account, edited.build());
        assertThat(after.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(after.reviewNote()).isEqualTo(ProfileService.REVERIFY_NOTE);
        assertThatThrownBy(() -> profileService.getPublic(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectedProfile_goesBackToPendingWhenResubmitted() {
        long account = newAccount();
        long id = profileService.save(account, TestProfiles.valid().build()).profile().id();
        profileService.reject(id, "License number not found on the state lookup");

        OwnProfile rejected = profileService.getOwn(account);
        assertThat(rejected.status()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(rejected.reviewNote()).contains("not found");

        TestProfiles fixed = TestProfiles.valid();
        fixed.licenseNumber = "CORRECTED-1";
        assertThat(profileService.save(account, fixed.build()).status()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    void officeMustBeInALicensedState() {
        TestProfiles p = TestProfiles.valid();
        p.practiceState = "OH"; // licensed only in MI
        assertThatThrownBy(() -> profileService.save(newAccount(), p.build()))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getFields()).containsKey("practiceState"));
    }

    @Test
    void onlyOptionsFromTheFixedListsAreAccepted() {
        TestProfiles p = TestProfiles.valid();
        p.languages = Set.of("English", "Klingon");
        assertThatThrownBy(() -> profileService.save(newAccount(), p.build()))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getFields().get("languages")).contains("Klingon"));
    }

    @Test
    void needsAtLeastOneContactMethod() {
        TestProfiles p = TestProfiles.valid();
        p.contactEmail = null;
        assertThatThrownBy(() -> profileService.save(newAccount(), p.build()))
                .isInstanceOfSatisfying(BadRequestException.class,
                        e -> assertThat(e.getFields()).containsKey("contactEmail"));
    }

    @Test
    void licenseStateIsAlwaysAddedToLicensedStates() {
        TestProfiles p = TestProfiles.valid();
        p.statesLicensed = Set.of();
        assertThat(profileService.save(newAccount(), p.build()).profile().statesLicensed()).containsExactly("MI");
    }

    @Test
    void nightlyJob_pausesListingsNotConfirmedFor60Days() {
        long account = newAccount();
        long id = profileService.save(account, TestProfiles.valid().build()).profile().id();
        profileService.approve(id);

        tx.executeWithoutResult(s -> {
            TherapistProfile p = repo.findById(id).orElseThrow();
            p.confirmAvailability(true, Instant.now().minus(Duration.ofDays(61)));
        });

        assertThat(availabilityJob.pauseStaleListings()).isGreaterThanOrEqualTo(1);
        assertThat(repo.findById(id).orElseThrow().isAcceptingNewClients()).isFalse();

        // The therapist confirms again and is back
        OwnProfile confirmed = profileService.confirmAvailability(account, true);
        assertThat(confirmed.profile().acceptingNewClients()).isTrue();
        assertThat(confirmed.shouldConfirmAvailability()).isFalse();
    }

    private long newAccount() {
        return auth.registerTherapist("t-" + UUID.randomUUID() + "@example.org", "a-long-password-1").account().id();
    }
}
