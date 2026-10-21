package com.kindred.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * In production the built React app is served from src/main/resources/static. React Router owns
 * these URLs, so opening or refreshing one directly must return index.html.
 */
@Controller
public class SpaForwardingController {

    @GetMapping({"/search", "/saved", "/therapists/**", "/for-therapists", "/dashboard", "/admin"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
