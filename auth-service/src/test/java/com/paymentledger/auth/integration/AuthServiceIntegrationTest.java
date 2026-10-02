package com.paymentledger.auth.integration;

import com.paymentledger.auth.dto.LoginRequest;
import com.paymentledger.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
public class AuthServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void checkDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available, skipping tests");
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @Test
    void register_thenLogin_returnsValidJwt() {
        String email = "test1@example.com";
        String password = "password123";

        // Register
        RegisterRequest registerRequest = new RegisterRequest(email, password, "Test User 1");
        ResponseEntity<Void> registerResponse = restTemplate.postForEntity(getBaseUrl() + "/register", registerRequest, Void.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        // Login
        LoginRequest loginRequest = new LoginRequest(email, password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(getBaseUrl() + "/login", loginRequest, Map.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        
        Map<String, String> body = loginResponse.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("token"));
        assertNotNull(body.get("token"));
        assertFalse(body.get("token").isBlank());
    }

    @Test
    void register_withDuplicateEmail_returns409() {
        String email = "duplicate@example.com";
        String password = "password123";

        RegisterRequest registerRequest = new RegisterRequest(email, password, "Duplicate User");
        ResponseEntity<Void> r1 = restTemplate.postForEntity(getBaseUrl() + "/register", registerRequest, Void.class);
        assertEquals(HttpStatus.CREATED, r1.getStatusCode());

        ResponseEntity<Void> r2 = restTemplate.postForEntity(getBaseUrl() + "/register", registerRequest, Void.class);
        assertEquals(HttpStatus.CONFLICT, r2.getStatusCode());
    }

    @Test
    void login_withWrongPassword_returns401() {
        String email = "wrongpass@example.com";
        
        RegisterRequest registerRequest = new RegisterRequest(email, "correctpass", "Wrong Pass User");
        restTemplate.postForEntity(getBaseUrl() + "/register", registerRequest, Void.class);

        LoginRequest loginRequest = new LoginRequest(email, "wrongpass");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(getBaseUrl() + "/login", loginRequest, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, loginResponse.getStatusCode());
    }

    @Test
    void getMe_withValidToken_returnsUserProfile() {
        String email = "me@example.com";
        String password = "password123";

        // Register
        RegisterRequest registerRequest = new RegisterRequest(email, password, "Me User");
        restTemplate.postForEntity(getBaseUrl() + "/register", registerRequest, Void.class);

        // Login
        LoginRequest loginRequest = new LoginRequest(email, password);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(getBaseUrl() + "/login", loginRequest, Map.class);
        String token = (String) loginResponse.getBody().get("token");

        // Get Me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> meResponse = restTemplate.exchange(getBaseUrl() + "/me", HttpMethod.GET, entity, Map.class);
        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
        
        Map<String, Object> body = meResponse.getBody();
        assertNotNull(body);
        assertEquals(email, body.get("email"));
    }
}
