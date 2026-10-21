package com.kindred.config;

import com.kindred.auth.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Creates the admin account from configuration the first time the app starts. */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService auth;
    private final String email;
    private final String password;

    public AdminBootstrap(AuthService auth,
                          @Value("${kindred.admin.email:}") String email,
                          @Value("${kindred.admin.password:}") String password) {
        this.auth = auth;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("No admin configured (set ADMIN_EMAIL and ADMIN_PASSWORD). Nobody can verify therapists.");
            return;
        }
        if (password.length() < 12) {
            log.warn("Admin password is shorter than 12 characters. Use a longer one in production.");
        }
        if (auth.ensureAdmin(email, password)) {
            log.info("Created admin account {}", email);
        }
    }
}
