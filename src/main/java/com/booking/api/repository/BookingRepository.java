package com.booking.api.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.booking.api.enums.BookingStatus;
import com.booking.api.model.entity.Booking;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.room.id   = :roomId
              AND b.status    = :status
              AND b.startTime < :end
              AND b.endTime   > :start
            """)
    boolean existsOverlapping(@Param("roomId") Long roomId,
            @Param("status") BookingStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
