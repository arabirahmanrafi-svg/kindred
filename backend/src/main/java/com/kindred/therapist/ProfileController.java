package com.kindred.therapist;

import com.kindred.auth.AuthService;
import com.kindred.auth.CurrentAccount;
import com.kindred.domain.Role;
import com.kindred.therapist.ProfileDtos.AvailabilityRequest;
import com.kindred.therapist.ProfileDtos.OwnProfile;
import com.kindred.therapist.ProfileDtos.ProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A therapist managing their own listing. */
@RestController
@RequestMapping("/api/me/profile")
public class ProfileController {

    private final AuthService auth;
    private final ProfileService profiles;

    public ProfileController(AuthService auth, ProfileService profiles) {
        this.auth = auth;
        this.profiles = profiles;
    }

    @GetMapping
    public OwnProfile get(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz) {
        CurrentAccount me = auth.requireRole(authz, Role.THERAPIST);
        return profiles.getOwn(me.id());
    }

    @PutMapping
    public OwnProfile save(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz,
                           @Valid @RequestBody ProfileRequest request) {
        CurrentAccount me = auth.requireRole(authz, Role.THERAPIST);
        return profiles.save(me.id(), request);
    }

    @PostMapping("/availability")
    public OwnProfile confirmAvailability(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz,
                                          @RequestBody AvailabilityRequest request) {
        CurrentAccount me = auth.requireRole(authz, Role.THERAPIST);
        return profiles.confirmAvailability(me.id(), request.acceptingNewClients());
    }
}
