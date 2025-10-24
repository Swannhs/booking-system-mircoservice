package com.bookingapi.service;

import com.bookingapi.event.BookingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingCreated(BookingCreatedEvent event) {
        try {
            kafkaTemplate.send("booking_created", event.getBookingId().toString(), event);
            log.info("Published booking created event: {}", event);
        } catch (Exception e) {
            log.error("Error publishing booking created event: {}", e.getMessage(), e);
            // TODO: Implement retry mechanism or dead letter queue
            throw new RuntimeException("Failed to publish booking created event", e);
        }
    }
}