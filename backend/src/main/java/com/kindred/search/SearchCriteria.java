package com.kindred.search;

import java.util.Set;

/**
 * What the person is looking for.
 *
 * @param state               the state the person is located in ("" = any). Decides who can legally see them.
 * @param wantInPerson        include in-person therapists
 * @param wantTelehealth      include telehealth therapists
 * @param languages           preferred languages (soft: scored, not required)
 * @param communities         communities they want the therapist to understand (soft)
 * @param specialties         what they want help with (soft)
 * @param insurance           insurer the therapist must accept (hard), or null
 * @param maxFee              highest self-pay fee they can afford (hard), or null
 * @param includeNotAccepting also show therapists who aren't taking new clients
 */
public record SearchCriteria(
        String state,
        boolean wantInPerson,
        boolean wantTelehealth,
        Set<String> languages,
        Set<String> communities,
        Set<String> specialties,
        String insurance,
        Integer maxFee,
        boolean includeNotAccepting
) {
    public int requestedPreferenceCount() {
        return languages.size() + communities.size() + specialties.size();
    }
}
