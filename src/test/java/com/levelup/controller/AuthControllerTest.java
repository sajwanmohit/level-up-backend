package com.levelup.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.levelup.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-api;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "AUTH_TOKEN_EXPIRATION_SECONDS=900",
        "app.auth.jwt-signing-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtEncoder jwtEncoder;

    @Test
    void signsUpWithNormalizedEmailAndStoresOnlyPasswordHash() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alex","email":"  Alex@Example.com ","password":"securePass123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        var user = users.findByEmail("alex@example.com").orElseThrow();
        assertNotEquals("securePass123", user.getPasswordHash());
        assertTrue(passwordEncoder.matches("securePass123", user.getPasswordHash()));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alex","email":"ALEX@example.com","password":"securePass123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsOnlyTheContractFieldsAndTokenAuthenticatesRequests() throws Exception {
        Account account = createAccount("Alex", "alex-" + UUID.randomUUID() + "@example.com", "securePass123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"securePass123"}
                                """.formatted(account.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.user.*", hasSize(3)))
                .andExpect(jsonPath("$.user.id").value(account.id().toString()))
                .andExpect(jsonPath("$.user.name").value("Alex"))
                .andExpect(jsonPath("$.user.email").value(account.email()))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(response.has("password"));
        String token = response.get("accessToken").asText();
        mockMvc.perform(get("/api/v1/workouts")
                        .param("userId", account.id().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void signupRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alex","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void loginUsesTheSameFailureForUnknownEmailAndWrongPassword() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        createAccount("Alex", email, "securePass123");
        String request = """
                {"email":"%s","password":"wrongPass123"}
                """.formatted(email);
        MvcResult wrongPassword = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult unknownEmail = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing-%s","password":"wrongPass123"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized()).andReturn();

        JsonNode wrongPasswordResponse = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
        JsonNode unknownEmailResponse = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());
        assertEquals(wrongPasswordResponse.get("error").asText(), unknownEmailResponse.get("error").asText());
        assertEquals(wrongPasswordResponse.get("message").asText(), unknownEmailResponse.get("message").asText());
    }

    @Test
    void protectedEndpointsRejectMissingInvalidAndExpiredTokens() throws Exception {
        mockMvc.perform(get("/api/v1/workouts").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/workouts")
                        .param("userId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());

        Instant now = Instant.now();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("level-up-backend").subject(UUID.randomUUID().toString())
                        .issuedAt(now.minusSeconds(1200)).expiresAt(now.minusSeconds(600)).build()))
                .getTokenValue();
        mockMvc.perform(get("/api/v1/workouts")
                        .param("userId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCannotReadOrCreateWorkoutsForAnotherUser() throws Exception {
        Account owner = createAccount("Owner", "owner-" + UUID.randomUUID() + "@example.com", "securePass123");
        Account other = createAccount("Other", "other-" + UUID.randomUUID() + "@example.com", "securePass123");
        MvcResult workoutResult = mockMvc.perform(post("/api/v1/workouts")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","workoutDate":"2026-11-05"}
                                """.formatted(owner.id())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID workoutId = UUID.fromString(objectMapper.readTree(workoutResult.getResponse().getContentAsString())
                .get("id").asText());

        mockMvc.perform(get("/api/v1/workouts")
                        .param("userId", other.id().toString())
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/workouts")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","workoutDate":"2026-11-05"}
                                """.formatted(other.id())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/workouts/" + workoutId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/analytics/daily")
                        .param("workoutId", workoutId.toString())
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isForbidden());
    }

    private Account createAccount(String name, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"%s"}
                                """.formatted(name, email, password)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Account(UUID.fromString(response.get("user").get("id").asText()), email,
                response.get("accessToken").asText());
    }

    private record Account(UUID id, String email, String token) {}
}
