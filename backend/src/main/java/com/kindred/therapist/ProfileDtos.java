package com.kindred.therapist;

import com.kindred.domain.TherapistProfile;
import com.kindred.domain.VerificationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    /** What a therapist submits when creating or editing their listing. */
    public record ProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 60) String credentials,
            @NotBlank @Size(max = 80) String licenseType,
            @NotBlank @Size(max = 40) String licenseNumber,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "must be a 2-letter state code") String licenseState,
            @Size(max = 3000) String bio,
            @NotNull @Size(min = 1, max = 10, message = "choose between 1 and 10") Set<String> languages,
            @NotNull @Size(max = 10, message = "choose up to 10") Set<String> communities,
            @NotNull @Size(min = 1, max = 12, message = "choose between 1 and 12") Set<String> specialties,
            @NotNull @Size(max = 15) Set<String> insurances,
            @NotNull Set<String> statesLicensed,
            boolean offersInPerson,
            boolean offersTelehealth,
            @Size(max = 80) String practiceCity,
            String practiceState,
            @Min(0) @Max(2000) Integer feeMin,
            @Min(0) @Max(2000) Integer feeMax,
            boolean slidingScale,
            boolean acceptingNewClients,
            @Email @Size(max = 254) String contactEmail,
            @Size(max = 30) @Pattern(regexp = "^[0-9+()\\-. ]*$", message = "may only contain digits, spaces and + ( ) - .")
            String phone,
            // Only http(s) links. Blocks "javascript:" links, which would run code when a visitor clicks them.
            @Size(max = 300) @Pattern(regexp = "^(https?://\\S+)?$", message = "must start with https:// or http://")
            String websiteUrl
    ) {
    }

    /** What the public sees. Never includes the account email or admin notes. */
    public record PublicProfile(
            Long id,
            String fullName,
            String credentials,
            String licenseType,
            String licenseNumber,
            String licenseState,
            String bio,
            List<String> languages,
            List<String> communities,
            List<String> specialties,
            List<String> insurances,
            List<String> statesLicensed,
            boolean offersInPerson,
            boolean offersTelehealth,
            String practiceCity,
            String practiceState,
            Integer feeMin,
            Integer feeMax,
            boolean slidingScale,
            boolean acceptingNewClients,
            Instant availabilityConfirmedAt,
            String contactEmail,
            String phone,
            String websiteUrl,
            boolean demo,
            Instant verifiedAt
    ) {
        public static PublicProfile from(TherapistProfile p) {
            return new PublicProfile(
                    p.getId(), p.getFullName(), p.getCredentials(), p.getLicenseType(), p.getLicenseNumber(),
                    p.getLicenseState(), p.getBio(),
                    sorted(p.getLanguages()), sorted(p.getCommunities()), sorted(p.getSpecialties()),
                    sorted(p.getInsurances()), sorted(p.getStatesLicensed()),
                    p.isOffersInPerson(), p.isOffersTelehealth(), p.getPracticeCity(), p.getPracticeState(),
                    p.getFeeMin(), p.getFeeMax(), p.isSlidingScale(),
                    p.isAcceptingNewClients(), p.getAvailabilityConfirmedAt(),
                    p.getContactEmail(), p.getPhone(), p.getWebsiteUrl(), p.isDemo(), p.getVerifiedAt());
        }
    }

    /** The therapist's own view of their listing, including review status. */
    public record OwnProfile(
            PublicProfile profile,
            VerificationStatus status,
            String reviewNote,
            long daysSinceAvailabilityConfirmed,
            boolean shouldConfirmAvailability
    ) {
    }

    /** One row in the admin review queue. */
    public record ReviewItem(
            PublicProfile profile,
            String accountEmail,
            VerificationStatus status,
            Instant submittedAt
    ) {
    }

    public record AvailabilityRequest(boolean acceptingNewClients) {
    }

    public record RejectRequest(@NotBlank @Size(max = 500) String reason) {
    }

    private static List<String> sorted(Collection<String> values) {
        return values.stream().sorted().toList();
    }
}
