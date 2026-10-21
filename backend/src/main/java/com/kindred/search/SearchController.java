package com.kindred.search;

import com.kindred.error.BadRequestException;
import com.kindred.options.Options;
import com.kindred.search.SearchService.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * GET /api/search?state=MI&format=telehealth&languages=Bengali&specialties=Anxiety&insurance=Aetna
 *
 * Privacy: nothing about the search is stored. No account, no saved history, no logging of filters.
 */
@RestController
public class SearchController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    @GetMapping("/api/search")
    public SearchResponse search(
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "any") String format,
            @RequestParam(required = false) List<String> languages,
            @RequestParam(required = false) List<String> communities,
            @RequestParam(required = false) List<String> specialties,
            @RequestParam(required = false) String insurance,
            @RequestParam(required = false) Integer maxFee,
            @RequestParam(defaultValue = "false") boolean includeNotAccepting,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String st = state.trim().toUpperCase(Locale.ROOT);
        if (!st.isEmpty() && !Options.STATE_SET.contains(st)) {
            throw new BadRequestException("Unknown state", Map.of("state", "is not a US state code"));
        }
        boolean inPerson;
        boolean telehealth;
        switch (format.toLowerCase(Locale.ROOT)) {
            case "in_person" -> { inPerson = true; telehealth = false; }
            case "telehealth" -> { inPerson = false; telehealth = true; }
            case "any" -> { inPerson = true; telehealth = true; }
            default -> throw new BadRequestException("Unknown format",
                    Map.of("format", "must be any, in_person or telehealth"));
        }
        String ins = insurance == null || insurance.isBlank() ? null : insurance.trim();
        if (ins != null && !Options.INSURANCE_SET.contains(ins)) {
            throw new BadRequestException("Unknown insurance", Map.of("insurance", "is not a supported option"));
        }

        SearchCriteria criteria = new SearchCriteria(st, inPerson, telehealth,
                known(languages, Options.LANGUAGE_SET),
                known(communities, Options.COMMUNITY_SET),
                known(specialties, Options.SPECIALTY_SET),
                ins,
                maxFee == null || maxFee <= 0 ? null : maxFee,
                includeNotAccepting);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return search.search(criteria, safePage, safeSize);
    }

    /** Keeps only values from the fixed lists. Unknown values are ignored rather than failing the search. */
    private static Set<String> known(List<String> values, Set<String> allowed) {
        Set<String> out = new TreeSet<>();
        if (values != null) {
            values.stream().map(String::trim).filter(allowed::contains).forEach(out::add);
        }
        return out;
    }
}
