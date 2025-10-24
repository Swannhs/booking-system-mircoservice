package com.bookingapi.service;

import com.bookingapi.entity.Booking;
import com.bookingapi.entity.Item;
import com.bookingapi.entity.User;
import com.bookingapi.event.BookingCreatedEvent;
import com.bookingapi.repository.BookingRepository;
import com.bookingapi.repository.ItemRepository;
import com.bookingapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final EventProducerService eventProducerService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public Booking createBooking(UUID userId, UUID itemId, LocalDateTime startDate,
                                 LocalDateTime endDate, String notes) {
        log.info("Creating booking for user {}: item {} from {} to {}",
                userId, itemId, startDate, endDate);

        try {
            // Validate input dates
            if (startDate.isAfter(endDate) || startDate.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Invalid booking dates");
            }

            // Fetch user and item
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

            // Check if item is available
            if (!item.getIsAvailable()) {
                throw new IllegalArgumentException("Item is not available");
            }

            // Check for booking conflicts
            boolean isBooked = bookingRepository.isItemBookedInDateRange(
                itemId, startDate, endDate);
            if (isBooked) {
                throw new IllegalArgumentException("Item is already booked for these dates");
            }

            // Calculate total price
            long days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
            BigDecimal totalPrice = item.getPricePerDay().multiply(BigDecimal.valueOf(days));

            // Create booking entity
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setItem(item);
            booking.setStartDate(startDate);
            booking.setEndDate(endDate);
            booking.setTotalPrice(totalPrice);
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking.setNotes(notes);

            // Save booking
            Booking savedBooking = bookingRepository.save(booking);

            // Create and publish event
            BookingCreatedEvent event = new BookingCreatedEvent(
                savedBooking.getId(),
                userId,
                item.getName(),
                startDate,
                endDate,
                totalPrice,
                "CONFIRMED",
                LocalDateTime.now()
            );

            eventProducerService.publishBookingCreated(event);

            log.info("Booking created successfully with ID: {}", savedBooking.getId());
            return savedBooking;

        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create booking", e);
        }
    }

    public Booking getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }
}