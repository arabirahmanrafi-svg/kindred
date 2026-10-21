package com.kindred.therapist;

import com.kindred.auth.AuthService;
import com.kindred.domain.Role;
import com.kindred.domain.VerificationStatus;
import com.kindred.therapist.ProfileDtos.RejectRequest;
import com.kindred.therapist.ProfileDtos.ReviewItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * License verification queue. The admin looks each license up on the state licensing board's
 * public website, then approves or rejects with a reason the therapist can see.
 */
@RestController
@RequestMapping("/api/admin/profiles")
public class AdminController {

    private final AuthService auth;
    private final ProfileService profiles;

    public AdminController(AuthService auth, ProfileService profiles) {
        this.auth = auth;
        this.profiles = profiles;
    }

    @GetMapping
    public List<ReviewItem> list(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz,
                                 @RequestParam(defaultValue = "PENDING") VerificationStatus status) {
        auth.requireRole(authz, Role.ADMIN);
        return profiles.reviewQueue(status);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz,
                                        @PathVariable Long id) {
        auth.requireRole(authz, Role.ADMIN);
        profiles.approve(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz,
                                       @PathVariable Long id,
                                       @Valid @RequestBody RejectRequest request) {
        auth.requireRole(authz, Role.ADMIN);
        profiles.reject(id, request.reason());
        return ResponseEntity.noContent().build();
    }
}
