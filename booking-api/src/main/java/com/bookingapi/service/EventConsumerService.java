package com.bookingapi.service;

import com.bookingapi.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumerService {

    @KafkaListener(topics = "user_registered", groupId = "booking-service")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user registered event: {}", event);

        // Process the user registration event
        // For example: prepare booking slots, send welcome notifications, etc.
        try {
            // Business logic for handling user registration
            log.info("Processing user registration for user ID: {}", event.getUserId());

            // TODO: Implement business logic such as:
            // - Initialize user booking preferences
            // - Send welcome email (via notification service)
            // - Set up default booking settings

        } catch (Exception e) {
            log.error("Error processing user registered event: {}", e.getMessage(), e);
            // TODO: Implement error handling strategy (retry, dead letter queue, etc.)
        }
    }
}