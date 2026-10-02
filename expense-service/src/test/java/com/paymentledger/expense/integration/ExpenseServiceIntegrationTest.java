package com.paymentledger.expense.integration;

import com.paymentledger.expense.dto.CreateExpenseRequest;
import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.PaymentMethod;
import com.paymentledger.expense.repository.ExpenseRepository;
import com.paymentledger.expense.repository.OutboxEventRepository;
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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
public class ExpenseServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TestJwtHelper jwtHelper;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("expense_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeAll
    static void checkDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available, skipping tests");
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/expenses";
    }

    private HttpHeaders getAuthHeaders() {
        String token = jwtHelper.generateToken("user@example.com", UUID.randomUUID());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void createExpense_savesExpenseAndOutboxEventInSameTransaction() {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("100.50"),
                "USD",
                ExpenseCategory.FOOD,
                "Dinner",
                PaymentMethod.CREDIT_CARD
        );

        HttpEntity<CreateExpenseRequest> entity = new HttpEntity<>(request, getAuthHeaders());

        ResponseEntity<Void> response = restTemplate.postForEntity(getBaseUrl(), entity, Void.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertEquals(1, expenseRepository.findAll().size());
        
        var outboxEvents = outboxEventRepository.findUnpublishedEvents();
        assertFalse(outboxEvents.isEmpty());
        assertTrue(outboxEvents.stream().anyMatch(e -> "EXPENSE_CREATED".equals(e.getEventType())));
    }

    @Test
    void getExpenses_returnsPagedResult() {
        // Create 2 expenses
        CreateExpenseRequest req1 = new CreateExpenseRequest(new BigDecimal("10.00"), "USD", ExpenseCategory.FOOD, "Snack", PaymentMethod.CASH);
        CreateExpenseRequest req2 = new CreateExpenseRequest(new BigDecimal("20.00"), "USD", ExpenseCategory.TRAVEL, "Bus", PaymentMethod.CASH);

        HttpHeaders headers = getAuthHeaders();
        restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(req1, headers), Void.class);
        restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(req2, headers), Void.class);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(getBaseUrl() + "?page=0&size=10", HttpMethod.GET, entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("content"));
    }

    @Test
    void getExpenseSummary_returnsCorrectTotals() {
        expenseRepository.deleteAll(); // Clean up previous tests
        
        CreateExpenseRequest req1 = new CreateExpenseRequest(new BigDecimal("100.00"), "USD", ExpenseCategory.FOOD, "Lunch", PaymentMethod.CASH);
        CreateExpenseRequest req2 = new CreateExpenseRequest(new BigDecimal("200.00"), "USD", ExpenseCategory.FOOD, "Dinner", PaymentMethod.CASH);

        HttpHeaders headers = getAuthHeaders();
        restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(req1, headers), Void.class);
        restTemplate.postForEntity(getBaseUrl(), new HttpEntity<>(req2, headers), Void.class);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(getBaseUrl() + "/summary", HttpMethod.GET, entity, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(300.0, ((Number) body.get("totalAmount")).doubleValue());
        assertEquals(2, ((Number) body.get("totalCount")).intValue());
    }
}
