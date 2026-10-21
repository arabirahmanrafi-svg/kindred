package com.kindred.options;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fixed vocabularies for profile fields and search filters.
 *
 * Using fixed lists (instead of free text) is what makes matching reliable: a therapist who
 * types "bangla" and a user who searches "Bengali" would otherwise never meet.
 * The frontend loads these from GET /api/options, so both sides always agree.
 */
public final class Options {

    private Options() {
    }

    public static final List<String> LANGUAGES = List.of(
            "English", "Spanish", "Arabic", "Bengali", "Hindi", "Urdu", "Punjabi", "Gujarati", "Tamil",
            "Telugu", "Nepali", "Mandarin", "Cantonese", "Korean", "Japanese", "Vietnamese", "Tagalog",
            "Farsi", "Turkish", "French", "Haitian Creole", "Portuguese", "Russian", "Ukrainian",
            "Polish", "Swahili", "Amharic", "Somali", "Yoruba", "Igbo", "American Sign Language");

    /** Communities a therapist has experience serving. Chosen by the therapist; never inferred. */
    public static final List<String> COMMUNITIES = List.of(
            "South Asian", "East Asian", "Southeast Asian", "Middle Eastern & North African",
            "Black / African American", "African", "Caribbean", "Latino/a/x", "Native / Indigenous",
            "Immigrants & refugees", "International students", "Children of immigrants",
            "First-generation college students", "Muslim", "Christian", "Jewish", "Hindu", "Sikh",
            "LGBTQ+ affirming", "Veterans", "People with disabilities");

    public static final List<String> SPECIALTIES = List.of(
            "Anxiety", "Depression", "Stress & burnout", "Academic pressure", "Loneliness & isolation",
            "Trauma & PTSD", "Grief & loss", "Relationships", "Family conflict",
            "Identity & acculturation", "Homesickness & adjustment", "Self-esteem", "ADHD",
            "Sleep", "Anger", "Eating concerns", "Substance use", "Life transitions");

    public static final List<String> INSURANCES = List.of(
            "Blue Cross Blue Shield", "Aetna", "Cigna", "UnitedHealthcare", "Priority Health",
            "Health Alliance Plan (HAP)", "Humana", "Medicaid", "Medicare", "Tricare",
            "Student health insurance plan");

    public static final List<String> US_STATES = List.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN",
            "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH",
            "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT",
            "VT", "VA", "WA", "WV", "WI", "WY");

    public static final Set<String> LANGUAGE_SET = new LinkedHashSet<>(LANGUAGES);
    public static final Set<String> COMMUNITY_SET = new LinkedHashSet<>(COMMUNITIES);
    public static final Set<String> SPECIALTY_SET = new LinkedHashSet<>(SPECIALTIES);
    public static final Set<String> INSURANCE_SET = new LinkedHashSet<>(INSURANCES);
    public static final Set<String> STATE_SET = new LinkedHashSet<>(US_STATES);
}
