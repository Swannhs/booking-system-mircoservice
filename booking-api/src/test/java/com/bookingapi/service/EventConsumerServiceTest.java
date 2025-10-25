package com.bookingapi.service;

import com.bookingapi.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EventConsumerServiceTest {

    @InjectMocks
    private EventConsumerService eventConsumerService;

    @Test
    void handleUserRegistered_ShouldProcessEventSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .registeredAt(LocalDateTime.now())
                .build();

        // When
        eventConsumerService.handleUserRegistered(event);

        // Then
        // The method should complete without throwing exceptions
        // In a real scenario, you might verify logging or service calls
        // For now, we just ensure the method executes successfully
    }

    @Test
    void handleUserRegistered_ShouldHandleNullValues() {
        // Given
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(null)
                .email(null)
                .firstName(null)
                .lastName(null)
                .registeredAt(null)
                .build();

        // When & Then
        // Should not throw NullPointerException
        eventConsumerService.handleUserRegistered(event);
    }

    @Test
    void handleUserRegistered_ShouldHandleEmptyStrings() {
        // Given
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(UUID.randomUUID())
                .email("")
                .firstName("")
                .lastName("")
                .registeredAt(LocalDateTime.now())
                .build();

        // When & Then
        // Should handle empty strings gracefully
        eventConsumerService.handleUserRegistered(event);
    }
}