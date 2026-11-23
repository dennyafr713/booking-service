-- ---------------------------------------------------------------------
-- Query rekap Jam Pemakaian Ruangan (bulan berjalan, hanya CONFIRMED)
-- ---------------------------------------------------------------------
WITH current_month AS (
    SELECT date_trunc('month', NOW() AT TIME ZONE 'UTC')                      AS month_start,
           date_trunc('month', NOW() AT TIME ZONE 'UTC') + INTERVAL '1 month' AS month_end
)
SELECT r.id   AS room_id,
       r.name AS name,
       ROUND(
           COALESCE(
               SUM(CASE WHEN b.id IS NOT NULL THEN
                       EXTRACT(EPOCH FROM (LEAST(b.end_time, cm.month_end)
                                         - GREATEST(b.start_time, cm.month_start)))
                   END),
               0
           ) / 3600.0,
           2
       )      AS total_hours
FROM rooms r
CROSS JOIN current_month cm
LEFT JOIN bookings b
       ON b.room_id    = r.id
      AND b.status     = 'CONFIRMED'
      AND b.start_time < cm.month_end
      AND b.end_time   > cm.month_start
GROUP BY r.id, r.name
ORDER BY r.id;

-- ---------------------------------------------------------------------
-- Query pencarian Ruangan Tersedia
-- ---------------------------------------------------------------------
SELECT r.id,
       r.name,
       r.capacity
FROM rooms r
WHERE r.is_active = TRUE
  AND NOT EXISTS (
        SELECT 1
        FROM bookings b
        WHERE b.room_id    = r.id
          AND b.status     = 'CONFIRMED'
          AND b.start_time < TIMESTAMP '2026-12-01 12:00:00'
          AND b.end_time   > TIMESTAMP '2026-12-01 10:00:00'
      )
ORDER BY r.id;
