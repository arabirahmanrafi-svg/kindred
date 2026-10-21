package com.kindred.config;

import com.kindred.domain.Account;
import com.kindred.domain.Role;
import com.kindred.domain.TherapistProfile;
import com.kindred.repo.AccountRepository;
import com.kindred.repo.TherapistProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Fills a development database with SAMPLE therapists so search has something to show.
 *
 * Every sample is fictional: invented names, "SAMPLE-" license numbers, 555 phone numbers and
 * example emails. They carry demo=true, which the UI shows as a "Sample profile" label.
 * Controlled by kindred.seed-demo-data, which is off in the mysql (production) profile.
 */
@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final AccountRepository accounts;
    private final TherapistProfileRepository profiles;
    private int counter = 0;

    public DemoDataSeeder(@Value("${kindred.seed-demo-data:false}") boolean enabled,
                          AccountRepository accounts, TherapistProfileRepository profiles) {
        this.enabled = enabled;
        this.accounts = accounts;
        this.profiles = profiles;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || profiles.countByDemoTrue() > 0) {
            return;
        }

        seed("Nadia Chowdhury", "PhD, LP", "Licensed Psychologist", "MI", Set.of("MI", "OH"),
                "I work with South Asian and immigrant families navigating anxiety, family expectations and the pressure of living between two cultures. Sessions in English, Bengali or Hindi.",
                Set.of("English", "Bengali", "Hindi"),
                Set.of("South Asian", "Immigrants & refugees", "International students", "Muslim", "Children of immigrants"),
                Set.of("Anxiety", "Depression", "Identity & acculturation", "Family conflict", "Academic pressure"),
                Set.of("Blue Cross Blue Shield", "Aetna", "Student health insurance plan"),
                true, "Ann Arbor", "MI", true, 120, 180, false, true, 5);

        seed("Samir Haddad", "LMSW", "Licensed Master Social Worker", "MI", Set.of("MI"),
                "Arabic-speaking therapist supporting refugees and families after loss, displacement and trauma.",
                Set.of("Arabic", "English"),
                Set.of("Middle Eastern & North African", "Muslim", "Immigrants & refugees"),
                Set.of("Trauma & PTSD", "Anxiety", "Family conflict", "Grief & loss"),
                Set.of("Medicaid", "Blue Cross Blue Shield", "Health Alliance Plan (HAP)"),
                true, "Dearborn", "MI", true, null, null, true, true, 12);

        seed("Priya Venkataraman", "LPC", "Licensed Professional Counselor", "MI", Set.of("MI"),
                "Telehealth counseling for first-generation students and young professionals dealing with burnout, perfectionism and self-worth.",
                Set.of("English", "Tamil", "Telugu"),
                Set.of("South Asian", "First-generation college students", "Children of immigrants", "LGBTQ+ affirming"),
                Set.of("Stress & burnout", "Academic pressure", "Self-esteem", "Relationships"),
                Set.of("Priority Health", "Aetna", "Cigna"),
                false, null, null, true, 100, 140, false, true, 20);

        seed("Marisol Gutiérrez", "LMFT", "Licensed Marriage and Family Therapist", "MI", Set.of("MI"),
                "Bilingual family therapy that honors culture and faith. Sliding-scale spots available.",
                Set.of("Spanish", "English"),
                Set.of("Latino/a/x", "Immigrants & refugees", "Children of immigrants", "Christian"),
                Set.of("Family conflict", "Relationships", "Depression", "Life transitions"),
                Set.of("Medicaid", "UnitedHealthcare"),
                true, "Ypsilanti", "MI", false, null, null, true, true, 3);

        seed("Kwame Asante", "LMSW", "Licensed Master Social Worker", "MI", Set.of("MI"),
                "Helping international and African students with homesickness, loneliness and finding their footing in a new country.",
                Set.of("English", "French"),
                Set.of("African", "Black / African American", "International students", "Immigrants & refugees"),
                Set.of("Homesickness & adjustment", "Loneliness & isolation", "Depression", "Identity & acculturation"),
                Set.of("Blue Cross Blue Shield", "Student health insurance plan"),
                true, "Ypsilanti", "MI", true, 80, 120, false, true, 45);

        seed("Mei-Ling Zhou", "PhD", "Licensed Psychologist", "IL", Set.of("IL", "MI"),
                "Licensed in Illinois and Michigan. Supporting East Asian and international students with anxiety, sleep problems and academic stress.",
                Set.of("Mandarin", "Cantonese", "English"),
                Set.of("East Asian", "International students", "Children of immigrants"),
                Set.of("Anxiety", "Academic pressure", "Sleep", "Stress & burnout"),
                Set.of("Aetna", "Cigna", "UnitedHealthcare"),
                false, null, null, true, 150, 200, false, true, 8);

        seed("Arjun Mehta", "LPC", "Licensed Professional Counselor", "OH", Set.of("OH"),
                "Ohio-licensed counselor for adults with ADHD, anxiety and big life changes.",
                Set.of("Hindi", "Gujarati", "English"),
                Set.of("South Asian", "Hindu", "First-generation college students"),
                Set.of("Anxiety", "ADHD", "Life transitions"),
                Set.of("Blue Cross Blue Shield", "Humana"),
                true, "Columbus", "OH", true, 110, 150, false, true, 15);

        seed("Fatima Yusuf", "LMSW", "Licensed Master Social Worker", "MI", Set.of("MI"),
                "Trauma-informed care for Somali and East African communities. Currently full, with a waitlist.",
                Set.of("English", "Somali", "Arabic"),
                Set.of("African", "Muslim", "Immigrants & refugees", "People with disabilities"),
                Set.of("Trauma & PTSD", "Grief & loss", "Anxiety"),
                Set.of("Medicaid", "Medicare"),
                false, null, null, true, null, null, true, false, 10);

        seed("Daniel Kim", "LMFT", "Licensed Marriage and Family Therapist", "MI", Set.of("MI"),
                "Korean-American therapist working with couples, families and second-generation identity.",
                Set.of("Korean", "English"),
                Set.of("East Asian", "LGBTQ+ affirming", "Christian", "Children of immigrants"),
                Set.of("Relationships", "Family conflict", "Self-esteem", "Anger"),
                Set.of("Priority Health", "Blue Cross Blue Shield"),
                true, "Ann Arbor", "MI", false, 130, 170, false, true, 2);

        seed("Rasheeda Coleman", "LPC", "Licensed Professional Counselor", "MI", Set.of("MI"),
                "Affirming, culturally grounded counseling. Fluent in American Sign Language.",
                Set.of("English", "American Sign Language"),
                Set.of("Black / African American", "First-generation college students", "LGBTQ+ affirming", "People with disabilities"),
                Set.of("Depression", "Stress & burnout", "Trauma & PTSD", "Self-esteem"),
                Set.of("Blue Cross Blue Shield", "Health Alliance Plan (HAP)", "Medicaid", "Student health insurance plan"),
                true, "Detroit", "MI", true, null, null, true, true, 25);

        seed("Thanh Nguyen", "LMSW", "Licensed Master Social Worker", "MI", Set.of("MI"),
                "Vietnamese-speaking therapist for veterans and refugee families. (Sample of a listing whose availability hasn't been confirmed in a while.)",
                Set.of("Vietnamese", "English"),
                Set.of("Southeast Asian", "Immigrants & refugees", "Veterans"),
                Set.of("Trauma & PTSD", "Grief & loss", "Sleep", "Substance use"),
                Set.of("Tricare", "Blue Cross Blue Shield", "Medicare"),
                false, null, null, true, 90, 130, false, true, 70);

        seed("Leila Karimi", "PsyD", "Licensed Psychologist", "MI", Set.of("MI"),
                "Persian-speaking psychologist. Identity, relationships and anxiety for immigrants and international students.",
                Set.of("Farsi", "English"),
                Set.of("Middle Eastern & North African", "Immigrants & refugees", "LGBTQ+ affirming", "International students"),
                Set.of("Anxiety", "Identity & acculturation", "Relationships", "Eating concerns"),
                Set.of("Aetna", "Cigna"),
                true, "Ann Arbor", "MI", true, 160, 220, false, true, 1);

        log.info("Seeded {} SAMPLE therapist profiles (development only)", counter);
    }

    @SuppressWarnings("java:S107") // a long parameter list is clearest for seed rows
    private void seed(String name, String credentials, String licenseType, String licenseState, Set<String> states,
                      String bio, Set<String> languages, Set<String> communities, Set<String> specialties,
                      Set<String> insurances, boolean inPerson, String city, String practiceState,
                      boolean telehealth, Integer feeMin, Integer feeMax, boolean sliding, boolean accepting,
                      int daysSinceConfirmed) {
        counter++;
        String n = String.format("%02d", counter);
        // Random "!" hash: BCrypt can never match it, so nobody can log in as a sample account.
        Account account = accounts.save(new Account("sample" + n + "@kindred.local", "!" + UUID.randomUUID(), Role.THERAPIST));

        TherapistProfile p = new TherapistProfile(account);
        p.setFullName(name);
        p.setCredentials(credentials);
        p.setLicenseType(licenseType);
        p.setLicenseNumber("SAMPLE-" + (1000 + counter));
        p.setLicenseState(licenseState);
        p.setBio(bio);
        p.replaceLanguages(languages);
        p.replaceCommunities(communities);
        p.replaceSpecialties(specialties);
        p.replaceInsurances(insurances);
        p.replaceStatesLicensed(states);
        p.setOffersInPerson(inPerson);
        p.setPracticeCity(city);
        p.setPracticeState(practiceState);
        p.setOffersTelehealth(telehealth);
        p.setFeeMin(feeMin);
        p.setFeeMax(feeMax);
        p.setSlidingScale(sliding);
        p.setContactEmail("sample" + n + "@example.org");
        p.setPhone("(555) 010-" + String.format("%04d", counter));
        p.setDemo(true);
        p.confirmAvailability(accepting, Instant.now().minus(Duration.ofDays(daysSinceConfirmed)));
        p.approve();
        profiles.save(p);
    }
}
