package com.bookingapi.service;

import com.bookingapi.entity.Booking;
import com.bookingapi.entity.Item;
import com.bookingapi.entity.User;
import com.bookingapi.event.BookingCreatedEvent;
import com.bookingapi.repository.BookingRepository;
import com.bookingapi.repository.ItemRepository;
import com.bookingapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private EventProducerService eventProducerService;

    @InjectMocks
    private BookingService bookingService;

    private UUID userId;
    private UUID itemId;
    private User user;
    private Item item;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        item = Item.builder()
                .id(itemId)
                .name("Test Item")
                .pricePerDay(BigDecimal.valueOf(50.00))
                .isAvailable(true)
                .build();

        startDate = LocalDateTime.now().plusDays(1);
        endDate = LocalDateTime.now().plusDays(3);
    }

    @Test
    void createBooking_ShouldCreateBookingSuccessfully() {
        // Given
        String notes = "Test booking";
        Booking savedBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(user)
                .item(item)
                .startDate(startDate)
                .endDate(endDate)
                .totalPrice(BigDecimal.valueOf(100.00))
                .status(Booking.BookingStatus.CONFIRMED)
                .notes(notes)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        doNothing().when(eventProducerService).publishBookingCreated(any(BookingCreatedEvent.class));

        // When
        Booking result = bookingService.createBooking(userId, itemId, startDate, endDate, notes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getItem()).isEqualTo(item);
        assertThat(result.getStartDate()).isEqualTo(startDate);
        assertThat(result.getEndDate()).isEqualTo(endDate);
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        assertThat(result.getNotes()).isEqualTo(notes);

        verify(bookingRepository).save(any(Booking.class));
        verify(eventProducerService).publishBookingCreated(any(BookingCreatedEvent.class));
    }

    @Test
    void createBooking_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(userId, itemId, startDate, endDate, "notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventProducerService, never()).publishBookingCreated(any(BookingCreatedEvent.class));
    }

    @Test
    void createBooking_ShouldThrowException_WhenItemNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(userId, itemId, startDate, endDate, "notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item not found");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventProducerService, never()).publishBookingCreated(any(BookingCreatedEvent.class));
    }

    @Test
    void createBooking_ShouldThrowException_WhenItemNotAvailable() {
        // Given
        item.setIsAvailable(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(userId, itemId, startDate, endDate, "notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item is not available for booking");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventProducerService, never()).publishBookingCreated(any(BookingCreatedEvent.class));
    }

    @Test
    void createBooking_ShouldThrowException_WhenEndDateBeforeStartDate() {
        // Given
        LocalDateTime invalidEndDate = startDate.minusDays(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        // When & Then
        assertThatThrownBy(() -> bookingService.createBooking(userId, itemId, startDate, invalidEndDate, "notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date must be after start date");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(eventProducerService, never()).publishBookingCreated(any(BookingCreatedEvent.class));
    }

    @Test
    void getBooking_ShouldReturnBooking_WhenExists() {
        // Given
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder().id(bookingId).build();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // When
        Booking result = bookingService.getBooking(bookingId);

        // Then
        assertThat(result).isEqualTo(booking);
        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getBooking_ShouldThrowException_WhenNotFound() {
        // Given
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.getBooking(bookingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking not found");

        verify(bookingRepository).findById(bookingId);
    }
}