package com.kindred.domain;

/**
 * A profile is only searchable once an admin has checked the license against the
 * state licensing board's public lookup and marked it VERIFIED.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
