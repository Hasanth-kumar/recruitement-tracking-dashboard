package com.rts.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.RtsApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RtsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(IntegrationTestSeedConfig.class)
class SprintOneIntegrationIT {

    private static final Path UPLOAD_DIR;

    static {
        try {
            UPLOAD_DIR = Files.createTempDirectory("rts-it-uploads");
            UPLOAD_DIR.toFile().deleteOnExit();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String recruiterToken;

    @DynamicPropertySource
    static void configureTestOverrides(DynamicPropertyRegistry registry) {
        registry.add("rts.storage.upload-dir", () -> UPLOAD_DIR.toAbsolutePath().toString());
    }

    @BeforeEach
    void obtainRecruiterToken() throws Exception {
        String loginBody = """
                {"usernameOrEmail":"recruiter","password":"Recruiter@123"}
                """;
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginBody, jsonHeaders()), String.class);
        JsonNode json = objectMapper.readTree(response.getBody());
        recruiterToken = json.path("data").path("accessToken").asText();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearerJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(recruiterToken);
        return headers;
    }

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(recruiterToken);
        return headers;
    }

    private static byte[] minimalPdf() {
        return "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n".getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] onePixelPng() {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
        );
    }

    @Test
    void sprintOne_login_candidates_documents_stage_history_softDelete() throws Exception {
        String loginBody = """
                {"usernameOrEmail":"admin","password":"Admin@123"}
                """;

        ResponseEntity<String> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginBody, jsonHeaders()),
                String.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode loginJson = objectMapper.readTree(loginResponse.getBody());
        assertThat(loginJson.path("success").asBoolean()).isTrue();
        assertThat(loginJson.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(loginJson.path("data").path("user").path("role").asText()).isEqualTo("ADMIN");

        String uniqueEmail = "it-" + UUID.randomUUID() + "@test.local";
        String createBody = """
                {"name":"Integration Candidate","email":"%s","phone":"+12345678901","position":"QA Engineer"}
                """.formatted(uniqueEmail);

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/candidates",
                HttpMethod.POST,
                new HttpEntity<>(createBody, bearerJsonHeaders()),
                String.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode created = objectMapper.readTree(createResponse.getBody());
        String candidateId = created.path("data").path("id").asText();
        assertThat(candidateId).isNotBlank();

        ResponseEntity<String> searchResponse = restTemplate.exchange(
                "/api/candidates?search=Integration",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class
        );
        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode searchJson = objectMapper.readTree(searchResponse.getBody());
        assertThat(searchJson.path("data").path("totalElements").asLong()).isGreaterThanOrEqualTo(1L);

        String updateBody = """
                {"name":"Integration Candidate Updated","email":"%s","phone":"+12345678901","position":"QA Engineer"}
                """.formatted(uniqueEmail);

        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId,
                HttpMethod.PUT,
                new HttpEntity<>(updateBody, bearerJsonHeaders()),
                String.class
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        MultiValueMap<String, Object> resumeParts = new LinkedMultiValueMap<>();
        resumeParts.add("file", new ByteArrayResource(minimalPdf()) {
            @Override
            public String getFilename() {
                return "resume.pdf";
            }
        });

        ResponseEntity<String> resumeResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId + "/resume",
                HttpMethod.POST,
                new HttpEntity<>(resumeParts, bearerHeaders()),
                String.class
        );
        assertThat(resumeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        MultiValueMap<String, Object> photoParts = new LinkedMultiValueMap<>();
        photoParts.add("file", new ByteArrayResource(onePixelPng()) {
            @Override
            public String getFilename() {
                return "photo.png";
            }
        });

        ResponseEntity<String> photoResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId + "/photo",
                HttpMethod.POST,
                new HttpEntity<>(photoParts, bearerHeaders()),
                String.class
        );
        assertThat(photoResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String stageBody = "{\"stage\":\"SHORTLISTED\"}";
        ResponseEntity<String> stageResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId + "/stage",
                HttpMethod.PUT,
                new HttpEntity<>(stageBody, bearerJsonHeaders()),
                String.class
        );
        assertThat(stageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> historyResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId + "/stage-history",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class
        );
        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode historyJson = objectMapper.readTree(historyResponse.getBody());
        assertThat(historyJson.path("data").isArray()).isTrue();
        assertThat(historyJson.path("data").size()).isGreaterThanOrEqualTo(1);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders()),
                String.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> missingResponse = restTemplate.exchange(
                "/api/candidates/" + candidateId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                String.class
        );
        assertThat(missingResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
