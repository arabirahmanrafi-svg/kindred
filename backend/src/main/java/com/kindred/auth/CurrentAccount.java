package com.kindred.auth;

import com.kindred.domain.Role;

/** The logged-in user for one request. */
public record CurrentAccount(Long id, String email, Role role) {
}
