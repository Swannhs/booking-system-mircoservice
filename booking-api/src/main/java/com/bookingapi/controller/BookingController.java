package com.bookingapi.controller;

import com.bookingapi.entity.Booking;
import com.bookingapi.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestParam UUID userId,
            @RequestParam UUID itemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "") String notes) {

        try {
            log.info("Creating booking for user {} and item {} from {} to {}",
                    userId, itemId, startDate, endDate);

            Booking booking = bookingService.createBooking(userId, itemId, startDate, endDate, notes);

            log.info("Booking created successfully with ID: {}", booking.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid booking request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable UUID id) {
        try {
            log.info("Retrieving booking with ID: {}", id);

            Booking booking = bookingService.getBooking(id);

            log.info("Booking retrieved successfully: {}", booking.getId());
            return ResponseEntity.ok(booking);

        } catch (IllegalArgumentException e) {
            log.warn("Booking not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error retrieving booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        return error;
    }
}