package com.bookingapi.repository;

import com.bookingapi.entity.Booking;
import com.bookingapi.entity.Item;
import com.bookingapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUser(User user);

    List<Booking> findByItem(Item item);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<Booking> findActiveBookingsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.item.id = :itemId AND " +
           "(b.startDate <= :endDate AND b.endDate >= :startDate) AND " +
           "b.status NOT IN ('CANCELLED', 'COMPLETED')")
    boolean isItemBookedInDateRange(@Param("itemId") UUID itemId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT b FROM Booking b WHERE " +
           "(b.startDate BETWEEN :startDate AND :endDate OR " +
           "b.endDate BETWEEN :startDate AND :endDate OR " +
           "b.startDate <= :startDate AND b.endDate >= :endDate) AND " +
           "b.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<Booking> findOverlappingBookings(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    List<Booking> findByUserId(UUID id);

    List<Booking> findByItemId(UUID id);

    @Query("SELECT b FROM Booking b WHERE b.item.id = :itemId AND " +
           "(b.startDate <= :endDate AND b.endDate >= :startDate) AND " +
           "b.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<Booking> findConflictingBookings(@Param("itemId") UUID itemId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
}