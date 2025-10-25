package com.bookingapi.controller;

import com.bookingapi.entity.Booking;
import com.bookingapi.entity.Item;
import com.bookingapi.entity.User;
import com.bookingapi.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private UUID userId;
    private UUID itemId;
    private UUID bookingId;
    private Booking testBooking;
    private User testUser;
    private Item testItem;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testItem = Item.builder()
                .id(itemId)
                .name("Test Item")
                .pricePerDay(BigDecimal.valueOf(50.00))
                .isAvailable(true)
                .build();

        testBooking = Booking.builder()
                .id(bookingId)
                .user(testUser)
                .item(testItem)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(100.00))
                .status(Booking.BookingStatus.CONFIRMED)
                .notes("Test booking")
                .build();
    }

    @Test
    void createBooking_ShouldReturnCreatedBooking() throws Exception {
        // Given
        when(bookingService.createBooking(eq(userId), eq(itemId),
                any(LocalDateTime.class), any(LocalDateTime.class), eq("Test notes")))
                .thenReturn(testBooking);

        // When & Then
        mockMvc.perform(post("/api/bookings")
                .param("userId", userId.toString())
                .param("itemId", itemId.toString())
                .param("startDate", "2024-12-01T10:00:00")
                .param("endDate", "2024-12-03T10:00:00")
                .param("notes", "Test notes")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.item.id").value(itemId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void createBooking_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/bookings")
                .param("userId", "invalid-uuid")
                .param("itemId", itemId.toString())
                .param("startDate", "2024-12-01T10:00:00")
                .param("endDate", "2024-12-03T10:00:00")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBooking_ShouldReturnBooking_WhenExists() throws Exception {
        // Given
        when(bookingService.getBooking(bookingId)).thenReturn(testBooking);

        // When & Then
        mockMvc.perform(get("/api/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getBooking_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(bookingService.getBooking(nonExistentId))
                .thenThrow(new IllegalArgumentException("Booking not found"));

        // When & Then
        mockMvc.perform(get("/api/bookings/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBooking_ShouldReturnBadRequest_WhenInvalidId() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/bookings/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBooking_ShouldHandleServiceExceptions() throws Exception {
        // Given
        when(bookingService.createBooking(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("User not found"));

        // When & Then
        mockMvc.perform(post("/api/bookings")
                .param("userId", userId.toString())
                .param("itemId", itemId.toString())
                .param("startDate", "2024-12-01T10:00:00")
                .param("endDate", "2024-12-03T10:00:00")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}