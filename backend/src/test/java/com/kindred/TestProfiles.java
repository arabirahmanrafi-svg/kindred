package com.kindred;

import com.kindred.therapist.ProfileDtos.ProfileRequest;

import java.util.Set;

/** Builds valid profile requests for tests. Change only the fields a test cares about. */
public final class TestProfiles {

    public String fullName = "Test Therapist";
    public String credentials = "LMSW";
    public String licenseType = "Licensed Master Social Worker";
    public String licenseNumber = "TEST-123";
    public String licenseState = "MI";
    public String bio = "Test bio";
    public Set<String> languages = Set.of("English");
    public Set<String> communities = Set.of("International students");
    public Set<String> specialties = Set.of("Anxiety");
    public Set<String> insurances = Set.of("Aetna");
    public Set<String> statesLicensed = Set.of("MI");
    public boolean offersInPerson = true;
    public boolean offersTelehealth = true;
    public String practiceCity = "Ypsilanti";
    public String practiceState = "MI";
    public Integer feeMin = 100;
    public Integer feeMax = 150;
    public boolean slidingScale = false;
    public boolean acceptingNewClients = true;
    public String contactEmail = "therapist@example.org";
    public String phone = null;
    public String websiteUrl = null;

    public static TestProfiles valid() {
        return new TestProfiles();
    }

    public ProfileRequest build() {
        return new ProfileRequest(fullName, credentials, licenseType, licenseNumber, licenseState, bio,
                languages, communities, specialties, insurances, statesLicensed,
                offersInPerson, offersTelehealth, practiceCity, practiceState,
                feeMin, feeMax, slidingScale, acceptingNewClients, contactEmail, phone, websiteUrl);
    }
}
