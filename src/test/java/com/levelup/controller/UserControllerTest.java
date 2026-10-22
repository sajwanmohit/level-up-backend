package com.levelup.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.levelup.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-api;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;

    @Test
    void createsAndPersistsUserWith201AndExactResponseShape() throws Exception {

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alex","email":"alex@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID id = UUID.fromString(response.get("id").asText());
        assertEquals("/api/v1/users/" + id, result.getResponse().getHeader("Location"));
        assertEquals("Alex", response.get("name").asText());
        assertEquals("alex@example.com", response.get("email").asText());
        assertEquals("Alex", users.findById(id).orElseThrow().getName());
        assertEquals("alex@example.com", users.findById(id).orElseThrow().getEmail());
    }

    @Test
    void rejectsMissingNameWithCommonValidationResponse() throws Exception {
        assertValidationFailure("""
                {"email":"alex@example.com"}
                """);
    }

    @Test
    void rejectsMissingEmailWithCommonValidationResponse() throws Exception {
        assertValidationFailure("""
                {"name":"Alex"}
                """);
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        assertValidationFailure("""
                {"name":"Alex","email":"not-an-email"}
                """);
    }

    @Test
    void acceptsMaximumNameAndEmailLengths() throws Exception {
        String name = "A".repeat(120);
        String email = "a".repeat(64) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.*", hasSize(3)));
    }

    @Test
    void rejectsNameAndEmailOverMaximumLengths() throws Exception {
        String overlongName = "A".repeat(121);
        assertValidationFailure("""
                {"name":"%s","email":"alex@example.com"}
                """.formatted(overlongName));

        String overlongEmail = "a".repeat(63) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63);
        assertValidationFailure("""
                {"name":"Alex","email":"%s"}
                """.formatted(overlongEmail));
    }

    @Test
    void rejectsDuplicateEmailUsingExistingConflictResponse() throws Exception {
        String request = """
                {"name":"Alex","email":"duplicate@example.com"}
                """;
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request conflicts with existing data"))
                .andExpect(jsonPath("$.path").value("/api/v1/users"))
                .andExpect(jsonPath("$.*", hasSize(5)));
    }

    @Test
    void documentsUserCreationInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/v1/users']['post']['summary']")
                        .value("Create a user"))
                .andExpect(jsonPath("$['paths']['/api/v1/users']['post']['requestBody']['content']['application/json']['schema']['$ref']")
                        .value(org.hamcrest.Matchers.endsWith("/UserRequest")))
                .andExpect(jsonPath("$.components.schemas.UserRequest.required",
                        hasItems("name", "email")))
                .andExpect(jsonPath("$.components.schemas.UserRequest.properties.name.maxLength")
                        .value(120))
                .andExpect(jsonPath("$.components.schemas.UserRequest.properties.email.maxLength")
                        .value(254))
                .andExpect(jsonPath("$['paths']['/api/v1/users']['post']['responses']['201']['content']['application/json']['schema']['$ref']")
                        .value(org.hamcrest.Matchers.endsWith("/UserResponse")))
                .andExpect(jsonPath("$['paths']['/api/v1/users']['post']['responses']['400']['content']['application/json']['schema']['$ref']")
                        .value(org.hamcrest.Matchers.endsWith("/ApiError")));
    }

    private void assertValidationFailure(String requestBody) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/users"))
                .andExpect(jsonPath("$.*", hasSize(5)));
    }
}
