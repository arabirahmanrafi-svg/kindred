package com.kindred.auth;

import com.kindred.repo.TherapistProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final TherapistProfileRepository profiles;

    public AuthController(AuthService auth, TherapistProfileRepository profiles) {
        this.auth = auth;
        this.profiles = profiles;
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 100, message = "must be at least 10 characters") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, MeResponse me) {
    }

    public record MeResponse(String email, String role, boolean hasProfile) {
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        AuthService.LoginResult result = auth.registerTherapist(req.email(), req.password());
        return new AuthResponse(result.token(), me(result.account()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        AuthService.LoginResult result = auth.login(req.email(), req.password());
        return new AuthResponse(result.token(), me(result.account()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz) {
        auth.logout(authz);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authz) {
        return me(auth.require(authz));
    }

    private MeResponse me(CurrentAccount account) {
        return new MeResponse(account.email(), account.role().name(),
                profiles.findByAccountId(account.id()).isPresent());
    }
}
