package com.kindred.therapist;

import com.kindred.therapist.ProfileDtos.PublicProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicTherapistController {

    private final ProfileService profiles;

    public PublicTherapistController(ProfileService profiles) {
        this.profiles = profiles;
    }

    /** Only verified profiles are visible. Pending or rejected ones return 404. */
    @GetMapping("/api/therapists/{id}")
    public PublicProfile get(@PathVariable Long id) {
        return profiles.getPublic(id);
    }
}
