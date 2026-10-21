package com.kindred.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kindred.TestProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through HTTP: sign up → create profile → admin approves → shows in search. */
@SpringBootTest(properties = "kindred.seed-demo-data=false")
@AutoConfigureMockMvc
class ApiFlowTest {

    private static final String ADMIN_EMAIL = "admin@kindred.local";
    private static final String ADMIN_PASSWORD = "ChangeMe-DevOnly-2026";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void therapistSignupToSearchResult() throws Exception {
        String therapistToken = register(uniqueEmail(), "a-long-password-1");

        mvc.perform(withAuth(get("/api/me/profile"), therapistToken)).andExpect(status().isNotFound());

        TestProfiles p = TestProfiles.valid();
        p.fullName = "Flow Test Therapist";
        p.languages = java.util.Set.of("English", "Bengali");
        JsonNode own = body(mvc.perform(withAuth(put("/api/me/profile"), therapistToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p.build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING")));
        long profileId = own.at("/profile/id").asLong();

        // Not public yet
        mvc.perform(get("/api/therapists/" + profileId)).andExpect(status().isNotFound());

        // Therapists can't open the admin queue; anonymous users aren't logged in
        mvc.perform(withAuth(get("/api/admin/profiles"), therapistToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/profiles")).andExpect(status().isUnauthorized());

        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        mvc.perform(withAuth(post("/api/admin/profiles/" + profileId + "/approve"), adminToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/therapists/" + profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Flow Test Therapist"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("reviewNote"))));

        JsonNode results = body(mvc.perform(get("/api/search")
                        .param("state", "MI").param("languages", "Bengali").param("size", "50"))
                .andExpect(status().isOk())).get("results");
        JsonNode mine = null;
        for (JsonNode r : results) {
            if (r.at("/therapist/id").asLong() == profileId) {
                mine = r;
            }
        }
        org.assertj.core.api.Assertions.assertThat(mine).as("approved therapist appears in search").isNotNull();
        org.assertj.core.api.Assertions.assertThat(mine.at("/match/reasons").toString()).contains("Speaks Bengali");
    }

    @Test
    void unsafeWebsiteLinkIsRejected() throws Exception {
        String token = register(uniqueEmail(), "a-long-password-1");
        TestProfiles p = TestProfiles.valid();
        p.websiteUrl = "javascript:alert(1)";

        mvc.perform(withAuth(put("/api/me/profile"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p.build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.websiteUrl").exists());
    }

    @Test
    void fiveWrongPasswords_lockTheAccountTemporarily() throws Exception {
        String email = uniqueEmail();
        register(email, "the-right-password");

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized());
        }
        // Even the right password is refused while locked
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"the-right-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(containsString("Too many")));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        String token = register(uniqueEmail(), "a-long-password-1");
        mvc.perform(withAuth(get("/api/auth/me"), token)).andExpect(status().isOk());
        mvc.perform(withAuth(post("/api/auth/logout"), token)).andExpect(status().isNoContent());
        mvc.perform(withAuth(get("/api/auth/me"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void searchRejectsUnknownState() throws Exception {
        mvc.perform(get("/api/search").param("state", "ZZ")).andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private String register(String email, String password) throws Exception {
        return body(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())).get("token").asText();
    }

    private String login(String email, String password) throws Exception {
        return body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())).get("token").asText();
    }

    private static MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private static org.springframework.test.web.servlet.result.ContentResultMatchers content() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content();
    }

    private static String uniqueEmail() {
        return "t-" + UUID.randomUUID() + "@example.org";
    }
}
