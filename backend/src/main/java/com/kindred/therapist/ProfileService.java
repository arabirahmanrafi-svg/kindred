package com.kindred.therapist;

import com.kindred.domain.TherapistProfile;
import com.kindred.domain.VerificationStatus;
import com.kindred.error.BadRequestException;
import com.kindred.error.NotFoundException;
import com.kindred.options.Options;
import com.kindred.repo.AccountRepository;
import com.kindred.repo.TherapistProfileRepository;
import com.kindred.therapist.ProfileDtos.OwnProfile;
import com.kindred.therapist.ProfileDtos.ProfileRequest;
import com.kindred.therapist.ProfileDtos.PublicProfile;
import com.kindred.therapist.ProfileDtos.ReviewItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    static final String REVERIFY_NOTE = "Your license details changed, so an admin will verify your profile again.";

    private final TherapistProfileRepository profiles;
    private final AccountRepository accounts;

    public ProfileService(TherapistProfileRepository profiles, AccountRepository accounts) {
        this.profiles = profiles;
        this.accounts = accounts;
    }

    // ---------- Therapist ----------

    @Transactional(readOnly = true)
    public OwnProfile getOwn(Long accountId) {
        return toOwn(findOwn(accountId));
    }

    /**
     * Creates or updates the therapist's listing.
     *
     * Safety rule: if a VERIFIED therapist changes their name or license details, the profile goes
     * back to PENDING and disappears from search until an admin re-checks it. Otherwise someone could
     * get verified with a real license and then swap in different details.
     */
    @Transactional
    public OwnProfile save(Long accountId, ProfileRequest req) {
        Clean c = clean(req);
        validate(c);

        TherapistProfile p = profiles.findByAccountId(accountId)
                .orElseGet(() -> new TherapistProfile(accounts.getReferenceById(accountId)));

        boolean isNew = p.getId() == null;
        boolean identityChanged = !isNew && (
                !Objects.equals(p.getFullName(), c.fullName)
                        || !Objects.equals(p.getCredentials(), c.credentials)
                        || !Objects.equals(p.getLicenseType(), c.licenseType)
                        || !Objects.equals(p.getLicenseNumber(), c.licenseNumber)
                        || !Objects.equals(p.getLicenseState(), c.licenseState));

        p.setFullName(c.fullName);
        p.setCredentials(c.credentials);
        p.setLicenseType(c.licenseType);
        p.setLicenseNumber(c.licenseNumber);
        p.setLicenseState(c.licenseState);
        p.setBio(c.bio);
        p.replaceLanguages(c.languages);
        p.replaceCommunities(c.communities);
        p.replaceSpecialties(c.specialties);
        p.replaceInsurances(c.insurances);
        p.replaceStatesLicensed(c.statesLicensed);
        p.setOffersInPerson(c.offersInPerson);
        p.setOffersTelehealth(c.offersTelehealth);
        p.setPracticeCity(c.offersInPerson ? c.practiceCity : null);
        p.setPracticeState(c.offersInPerson ? c.practiceState : null);
        p.setFeeMin(c.feeMin);
        p.setFeeMax(c.feeMax);
        p.setSlidingScale(c.slidingScale);
        p.setContactEmail(c.contactEmail);
        p.setPhone(c.phone);
        p.setWebsiteUrl(c.websiteUrl);
        // Saving the profile counts as confirming availability.
        p.confirmAvailability(c.acceptingNewClients, Instant.now());

        if (p.getVerificationStatus() == VerificationStatus.VERIFIED && identityChanged) {
            p.sendBackForReview(REVERIFY_NOTE);
        } else if (p.getVerificationStatus() == VerificationStatus.REJECTED) {
            p.sendBackForReview(null); // resubmitted after fixing the problem
        }

        return toOwn(profiles.save(p));
    }

    @Transactional
    public OwnProfile confirmAvailability(Long accountId, boolean accepting) {
        TherapistProfile p = findOwn(accountId);
        p.confirmAvailability(accepting, Instant.now());
        return toOwn(p);
    }

    // ---------- Public ----------

    @Transactional(readOnly = true)
    public PublicProfile getPublic(Long id) {
        return profiles.findByIdAndVerificationStatus(id, VerificationStatus.VERIFIED)
                .map(PublicProfile::from)
                .orElseThrow(() -> new NotFoundException("This therapist profile isn't available"));
    }

    // ---------- Admin ----------

    @Transactional(readOnly = true)
    public List<ReviewItem> reviewQueue(VerificationStatus status) {
        return profiles.findForReview(status).stream()
                .map(p -> new ReviewItem(PublicProfile.from(p), p.getAccount().getEmail(),
                        p.getVerificationStatus(), p.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public void approve(Long profileId) {
        find(profileId).approve();
    }

    @Transactional
    public void reject(Long profileId, String reason) {
        find(profileId).reject(reason.trim());
    }

    // ---------- Helpers ----------

    private TherapistProfile findOwn(Long accountId) {
        return profiles.findByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("You haven't created your profile yet"));
    }

    private TherapistProfile find(Long id) {
        return profiles.findById(id).orElseThrow(() -> new NotFoundException("Profile " + id + " not found"));
    }

    private static OwnProfile toOwn(TherapistProfile p) {
        Instant now = Instant.now();
        long days = AvailabilityPolicy.daysSince(p.getAvailabilityConfirmedAt(), now);
        boolean remind = AvailabilityPolicy.freshness(p.getAvailabilityConfirmedAt(), now)
                != AvailabilityPolicy.Freshness.FRESH;
        return new OwnProfile(PublicProfile.from(p), p.getVerificationStatus(), p.getReviewNote(), days, remind);
    }

    /** Request values after trimming and normalizing, so validation and saving see the same data. */
    private record Clean(
            String fullName, String credentials, String licenseType, String licenseNumber, String licenseState,
            String bio, Set<String> languages, Set<String> communities, Set<String> specialties,
            Set<String> insurances, Set<String> statesLicensed, boolean offersInPerson, boolean offersTelehealth,
            String practiceCity, String practiceState, Integer feeMin, Integer feeMax, boolean slidingScale,
            boolean acceptingNewClients, String contactEmail, String phone, String websiteUrl) {
    }

    private static Clean clean(ProfileRequest r) {
        String licenseState = upper(r.licenseState());
        Set<String> states = r.statesLicensed().stream().map(ProfileService::upper)
                .filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
        if (licenseState != null) {
            states.add(licenseState); // you're always licensed in your primary license state
        }
        return new Clean(
                trim(r.fullName()), trim(r.credentials()), trim(r.licenseType()), trim(r.licenseNumber()),
                licenseState, trim(r.bio()),
                trimAll(r.languages()), trimAll(r.communities()), trimAll(r.specialties()), trimAll(r.insurances()),
                states, r.offersInPerson(), r.offersTelehealth(),
                trim(r.practiceCity()), upper(r.practiceState()), r.feeMin(), r.feeMax(), r.slidingScale(),
                r.acceptingNewClients(), lowerTrim(r.contactEmail()), trim(r.phone()), trim(r.websiteUrl()));
    }

    /** Rules that need more than one field, or the fixed option lists. */
    private static void validate(Clean c) {
        Map<String, String> errors = new LinkedHashMap<>();

        checkAllowed(errors, "languages", c.languages, Options.LANGUAGE_SET);
        checkAllowed(errors, "communities", c.communities, Options.COMMUNITY_SET);
        checkAllowed(errors, "specialties", c.specialties, Options.SPECIALTY_SET);
        checkAllowed(errors, "insurances", c.insurances, Options.INSURANCE_SET);
        checkAllowed(errors, "statesLicensed", c.statesLicensed, Options.STATE_SET);
        if (c.licenseState != null && !Options.STATE_SET.contains(c.licenseState)) {
            errors.put("licenseState", "is not a US state code");
        }

        if (!c.offersInPerson && !c.offersTelehealth) {
            errors.put("offersInPerson", "Choose in-person, telehealth, or both");
        }
        if (c.offersInPerson) {
            if (c.practiceCity == null) {
                errors.put("practiceCity", "is required for in-person sessions");
            }
            if (c.practiceState == null || !Options.STATE_SET.contains(c.practiceState)) {
                errors.put("practiceState", "is required for in-person sessions");
            } else if (!c.statesLicensed.contains(c.practiceState)) {
                errors.put("practiceState", "You must be licensed in the state where your office is");
            }
        }

        if (c.feeMin != null && c.feeMax != null && c.feeMin > c.feeMax) {
            errors.put("feeMax", "must be at least the minimum fee");
        }
        if (c.contactEmail == null && c.phone == null && c.websiteUrl == null) {
            errors.put("contactEmail", "Add at least one way for clients to reach you");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Please fix the highlighted fields", errors);
        }
    }

    private static void checkAllowed(Map<String, String> errors, String field, Set<String> values, Set<String> allowed) {
        List<String> unknown = values.stream().filter(v -> !allowed.contains(v)).toList();
        if (!unknown.isEmpty()) {
            errors.put(field, "Unknown option(s): " + String.join(", ", unknown));
        }
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String upper(String s) {
        String t = trim(s);
        return t == null ? null : t.toUpperCase(Locale.ROOT);
    }

    private static String lowerTrim(String s) {
        String t = trim(s);
        return t == null ? null : t.toLowerCase(Locale.ROOT);
    }

    private static Set<String> trimAll(Set<String> values) {
        return values.stream().map(ProfileService::trim).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
