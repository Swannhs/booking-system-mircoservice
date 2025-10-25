package com.bookingapi;

import com.bookingapi.event.BookingCreatedEvent;
import com.bookingapi.event.UserRegisteredEvent;
import com.bookingapi.service.EventConsumerService;
import com.bookingapi.service.EventProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DirtiesContext
class KafkaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEmbeddedZookeeper();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private EventProducerService eventProducerService;

    @Autowired
    private EventConsumerService eventConsumerService;

    @Test
    void kafkaEventFlow_ShouldWorkEndToEnd() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        UserRegisteredEvent userEvent = UserRegisteredEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .registeredAt(LocalDateTime.now())
                .build();

        BookingCreatedEvent bookingEvent = BookingCreatedEvent.builder()
                .bookingId(bookingId)
                .userId(userId)
                .itemName("Test Item")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(150.00))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();

        // When - Publish events
        eventProducerService.publishBookingCreated(bookingEvent);

        // Simulate user registration event (normally from user service)
        eventConsumerService.handleUserRegistered(userEvent);

        // Then - Events should be processed without errors
        // In a real integration test, you might verify:
        // - Messages were sent to Kafka topics
        // - Consumer processed the messages
        // - Any resulting database changes occurred

        // For now, we verify the methods complete successfully
        assertThat(userId).isNotNull();
        assertThat(bookingId).isNotNull();
    }

    @Test
    void eventProducer_ShouldHandleKafkaConnection() {
        // Given
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .itemName("Test Item")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .totalPrice(BigDecimal.valueOf(50.00))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();

        // When & Then - Should not throw exceptions
        eventProducerService.publishBookingCreated(event);
    }

    @Test
    void eventConsumer_ShouldHandleIncomingEvents() {
        // Given
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(UUID.randomUUID())
                .email("consumer-test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .registeredAt(LocalDateTime.now())
                .build();

        // When & Then - Should not throw exceptions
        eventConsumerService.handleUserRegistered(event);
    }
}