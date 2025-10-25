package com.bookingapi.service;

import com.bookingapi.event.BookingCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EventProducerService eventProducerService;

    private BookingCreatedEvent testEvent;
    private UUID bookingId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testEvent = BookingCreatedEvent.builder()
                .bookingId(bookingId)
                .userId(userId)
                .itemName("Test Item")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(150.00))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void publishBookingCreated_ShouldSendEventSuccessfully() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("booking_created"), eq(bookingId.toString()), eq(testEvent)))
                .thenReturn(future);

        // When
        eventProducerService.publishBookingCreated(testEvent);

        // Then
        verify(kafkaTemplate).send("booking_created", bookingId.toString(), testEvent);
    }

    @Test
    void publishBookingCreated_ShouldThrowException_WhenKafkaFails() {
        // Given
        RuntimeException kafkaException = new RuntimeException("Kafka connection failed");
        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(kafkaException);

        // When & Then
        assertThatThrownBy(() -> eventProducerService.publishBookingCreated(testEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to publish booking created event")
                .hasCause(kafkaException);

        verify(kafkaTemplate).send("booking_created", bookingId.toString(), testEvent);
    }

    @Test
    void publishBookingCreated_ShouldHandleNullEvent() {
        // Given
        BookingCreatedEvent nullEvent = null;

        // When & Then
        assertThatThrownBy(() -> eventProducerService.publishBookingCreated(nullEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to publish booking created event");

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publishBookingCreated_ShouldHandleEventWithNullBookingId() {
        // Given
        BookingCreatedEvent eventWithNullId = BookingCreatedEvent.builder()
                .bookingId(null)
                .userId(userId)
                .itemName("Test Item")
                .build();

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("booking_created"), isNull(), eq(eventWithNullId)))
                .thenReturn(future);

        // When
        eventProducerService.publishBookingCreated(eventWithNullId);

        // Then
        verify(kafkaTemplate).send("booking_created", null, eventWithNullId);
    }
}