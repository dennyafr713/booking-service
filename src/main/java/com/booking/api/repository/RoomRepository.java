package com.booking.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.booking.api.model.entity.Room;

import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT r.*
            FROM rooms r
            WHERE r.is_active = TRUE
              AND NOT EXISTS (
                    SELECT 1
                    FROM bookings b
                    WHERE b.room_id    = r.id
                      AND b.status     = 'CONFIRMED'
                      AND b.start_time < :end
                      AND b.end_time   > :start
                  )
            ORDER BY r.id
            """, nativeQuery = true)
    List<Room> findAvailable(@Param("start") LocalDateTime startUtc, @Param("end") LocalDateTime endUtc);
}
