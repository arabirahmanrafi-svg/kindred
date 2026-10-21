package com.kindred.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A therapist's public listing.
 *
 * The multi-value fields (languages, communities, ...) are stored in their own small tables.
 * {@code @BatchSize} makes Hibernate load them for 50 profiles in one query instead of one
 * query per profile (the classic "N+1 queries" problem).
 */
@Entity
@Table(name = "therapist_profiles", indexes = {
        @Index(name = "idx_profile_search", columnList = "verification_status, accepting_new_clients")
})
public class TherapistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    // ---------- Identity and license ----------

    @Column(nullable = false, length = 120)
    private String fullName;

    /** Short letters shown after the name, e.g. "LMSW" or "PhD, LP". */
    @Column(nullable = false, length = 60)
    private String credentials;

    @Column(nullable = false, length = 80)
    private String licenseType;

    @Column(nullable = false, length = 40)
    private String licenseNumber;

    @Column(nullable = false, length = 2)
    private String licenseState;

    @Column(length = 3000)
    private String bio;

    // ---------- What they offer ----------

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "therapist_languages", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "language", length = 40)
    @BatchSize(size = 50)
    private Set<String> languages = new HashSet<>();

    /** Communities they have experience serving, chosen by the therapist from a fixed list. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "therapist_communities", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "community", length = 60)
    @BatchSize(size = 50)
    private Set<String> communities = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "therapist_specialties", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "specialty", length = 60)
    @BatchSize(size = 50)
    private Set<String> specialties = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "therapist_insurances", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "insurance", length = 60)
    @BatchSize(size = 50)
    private Set<String> insurances = new HashSet<>();

    /** States they hold a license in. Telehealth is only legal for clients located in these states. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "therapist_license_states", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "state_code", length = 2)
    @BatchSize(size = 50)
    private Set<String> statesLicensed = new HashSet<>();

    @Column(nullable = false)
    private boolean offersInPerson;

    @Column(nullable = false)
    private boolean offersTelehealth;

    @Column(length = 80)
    private String practiceCity;

    @Column(length = 2)
    private String practiceState;

    /** Self-pay fee range per session in US dollars. Null if not listed. */
    private Integer feeMin;
    private Integer feeMax;

    @Column(nullable = false)
    private boolean slidingScale;

    // ---------- Availability ----------

    @Column(nullable = false)
    private boolean acceptingNewClients;

    /** When the therapist last confirmed their availability. Old confirmations rank lower. */
    @Column(nullable = false)
    private Instant availabilityConfirmedAt;

    // ---------- Contact ----------

    @Column(length = 254)
    private String contactEmail;

    @Column(length = 30)
    private String phone;

    @Column(length = 300)
    private String websiteUrl;

    // ---------- Review ----------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    private Instant verifiedAt;

    /** Admin's note to the therapist, e.g. why a profile was rejected. */
    @Column(length = 500)
    private String reviewNote;

    /** Sample data for development. Shown with a clear "sample profile" label. */
    @Column(nullable = false)
    private boolean demo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected TherapistProfile() {
    }

    public TherapistProfile(Account account) {
        this.account = account;
        this.verificationStatus = VerificationStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.availabilityConfirmedAt = this.createdAt;
    }

    // ---------- Review actions ----------

    public void approve() {
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.verifiedAt = Instant.now();
        this.reviewNote = null;
        touch();
    }

    public void reject(String note) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedAt = null;
        this.reviewNote = note;
        touch();
    }

    public void sendBackForReview(String note) {
        this.verificationStatus = VerificationStatus.PENDING;
        this.verifiedAt = null;
        this.reviewNote = note;
        touch();
    }

    public void confirmAvailability(boolean accepting, Instant when) {
        this.acceptingNewClients = accepting;
        this.availabilityConfirmedAt = when;
        touch();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    // ---------- Getters and setters ----------

    public Long getId() { return id; }
    public Account getAccount() { return account; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCredentials() { return credentials; }
    public void setCredentials(String credentials) { this.credentials = credentials; }
    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getLicenseState() { return licenseState; }
    public void setLicenseState(String licenseState) { this.licenseState = licenseState; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Set<String> getLanguages() { return languages; }
    public Set<String> getCommunities() { return communities; }
    public Set<String> getSpecialties() { return specialties; }
    public Set<String> getInsurances() { return insurances; }
    public Set<String> getStatesLicensed() { return statesLicensed; }

    public void replaceLanguages(Set<String> values) { languages.clear(); languages.addAll(values); }
    public void replaceCommunities(Set<String> values) { communities.clear(); communities.addAll(values); }
    public void replaceSpecialties(Set<String> values) { specialties.clear(); specialties.addAll(values); }
    public void replaceInsurances(Set<String> values) { insurances.clear(); insurances.addAll(values); }
    public void replaceStatesLicensed(Set<String> values) { statesLicensed.clear(); statesLicensed.addAll(values); }

    public boolean isOffersInPerson() { return offersInPerson; }
    public void setOffersInPerson(boolean offersInPerson) { this.offersInPerson = offersInPerson; }
    public boolean isOffersTelehealth() { return offersTelehealth; }
    public void setOffersTelehealth(boolean offersTelehealth) { this.offersTelehealth = offersTelehealth; }
    public String getPracticeCity() { return practiceCity; }
    public void setPracticeCity(String practiceCity) { this.practiceCity = practiceCity; }
    public String getPracticeState() { return practiceState; }
    public void setPracticeState(String practiceState) { this.practiceState = practiceState; }
    public Integer getFeeMin() { return feeMin; }
    public void setFeeMin(Integer feeMin) { this.feeMin = feeMin; }
    public Integer getFeeMax() { return feeMax; }
    public void setFeeMax(Integer feeMax) { this.feeMax = feeMax; }
    public boolean isSlidingScale() { return slidingScale; }
    public void setSlidingScale(boolean slidingScale) { this.slidingScale = slidingScale; }

    public boolean isAcceptingNewClients() { return acceptingNewClients; }
    public Instant getAvailabilityConfirmedAt() { return availabilityConfirmedAt; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getReviewNote() { return reviewNote; }

    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
