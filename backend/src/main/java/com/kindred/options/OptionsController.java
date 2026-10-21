package com.kindred.options;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
public class OptionsController {

    public record OptionsResponse(List<String> languages, List<String> communities, List<String> specialties,
                                  List<String> insurances, List<String> states) {
    }

    @GetMapping("/api/options")
    public ResponseEntity<OptionsResponse> options() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body(new OptionsResponse(Options.LANGUAGES, Options.COMMUNITIES, Options.SPECIALTIES,
                        Options.INSURANCES, Options.US_STATES));
    }
}
