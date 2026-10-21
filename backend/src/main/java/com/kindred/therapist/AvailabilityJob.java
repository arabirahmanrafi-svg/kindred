package com.kindred.therapist;

import com.kindred.repo.TherapistProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Every night: listings unconfirmed for over 60 days stop showing as "accepting new clients". */
@Component
public class AvailabilityJob {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityJob.class);

    private final TherapistProfileRepository profiles;

    public AvailabilityJob(TherapistProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Scheduled(cron = "0 10 3 * * *")
    @Transactional
    public int pauseStaleListings() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(AvailabilityPolicy.PAUSE_AFTER_DAYS));
        int paused = profiles.pauseUnconfirmedSince(cutoff);
        if (paused > 0) {
            log.info("Paused {} listing(s) whose availability wasn't confirmed in {} days",
                    paused, AvailabilityPolicy.PAUSE_AFTER_DAYS);
        }
        return paused;
    }
}
