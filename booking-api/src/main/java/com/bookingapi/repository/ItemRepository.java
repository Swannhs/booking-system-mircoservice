package com.bookingapi.repository;

import com.bookingapi.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findByCategory(String category);

    List<Item> findByIsAvailableTrue();

    List<Item> findByLocation(String location);

    @Query("SELECT i FROM Item i WHERE i.isAvailable = true AND i.id NOT IN " +
           "(SELECT b.item.id FROM Booking b WHERE " +
           "(b.startDate <= :endDate AND b.endDate >= :startDate) AND " +
           "b.status NOT IN ('CANCELLED', 'COMPLETED'))")
    List<Item> findAvailableItemsInDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    List<Item> findByPricePerDayLessThanEqualOrderByPricePerDayAsc(java.math.BigDecimal maxPrice);
}