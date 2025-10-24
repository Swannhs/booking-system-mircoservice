package com.bookingapi.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreatedEvent {
    private UUID bookingId;
    private UUID userId;
    private String itemName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
}