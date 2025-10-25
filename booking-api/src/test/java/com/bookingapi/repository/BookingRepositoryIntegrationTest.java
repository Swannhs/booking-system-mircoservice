package com.bookingapi.repository;

import com.bookingapi.entity.Booking;
import com.bookingapi.entity.Item;
import com.bookingapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User testUser;
    private Item testItem;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        testUser = userRepository.save(testUser);

        // Create test item
        testItem = Item.builder()
                .id(UUID.randomUUID())
                .name("Test Item")
                .description("A test item for booking")
                .pricePerDay(BigDecimal.valueOf(50.00))
                .isAvailable(true)
                .location("Test Location")
                .build();
        testItem = itemRepository.save(testItem);

        // Create test booking
        testBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .item(testItem)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(100.00))
                .status(Booking.BookingStatus.CONFIRMED)
                .notes("Test booking")
                .build();
        testBooking = bookingRepository.save(testBooking);
    }

    @Test
    void save_ShouldPersistBooking() {
        // Given
        Booking newBooking = Booking.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .item(testItem)
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(7))
                .totalPrice(BigDecimal.valueOf(200.00))
                .status(Booking.BookingStatus.PENDING)
                .build();

        // When
        Booking saved = bookingRepository.save(newBooking);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getItem()).isEqualTo(testItem);
        assertThat(saved.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(saved.getTotalPrice()).isEqualTo(BigDecimal.valueOf(200.00));
    }

    @Test
    void findById_ShouldReturnBooking_WhenExists() {
        // When
        Optional<Booking> found = bookingRepository.findById(testBooking.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(testBooking);
        assertThat(found.get().getUser()).isEqualTo(testUser);
        assertThat(found.get().getItem()).isEqualTo(testItem);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<Booking> found = bookingRepository.findById(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllBookings() {
        // When
        List<Booking> bookings = bookingRepository.findAll();

        // Then
        assertThat(bookings).isNotEmpty();
        assertThat(bookings).contains(testBooking);
    }

    @Test
    void findByUserId_ShouldReturnUserBookings() {
        // When
        List<Booking> userBookings = bookingRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userBookings).contains(testBooking);
        assertThat(userBookings).allMatch(booking -> booking.getUser().getId().equals(testUser.getId()));
    }

    @Test
    void findByItemId_ShouldReturnItemBookings() {
        // When
        List<Booking> itemBookings = bookingRepository.findByItemId(testItem.getId());

        // Then
        assertThat(itemBookings).contains(testBooking);
        assertThat(itemBookings).allMatch(booking -> booking.getItem().getId().equals(testItem.getId()));
    }

    @Test
    void findByStatus_ShouldReturnBookingsWithStatus() {
        // When
        List<Booking> confirmedBookings = bookingRepository.findByStatus(Booking.BookingStatus.CONFIRMED);

        // Then
        assertThat(confirmedBookings).contains(testBooking);
        assertThat(confirmedBookings).allMatch(booking -> booking.getStatus().equals(Booking.BookingStatus.CONFIRMED));
    }

    @Test
    void findConflictingBookings_ShouldReturnOverlappingBookings() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().plusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(4);

        // When
        List<Booking> conflicts = bookingRepository.findConflictingBookings(testItem.getId(), startDate, endDate);

        // Then
        assertThat(conflicts).contains(testBooking);
    }

    @Test
    void delete_ShouldRemoveBooking() {
        // When
        bookingRepository.delete(testBooking);
        Optional<Booking> found = bookingRepository.findById(testBooking.getId());

        // Then
        assertThat(found).isEmpty();
    }
}