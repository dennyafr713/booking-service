package com.booking.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BookingConcurrencyIntegrationTest {

    private static final int PARALLEL_REQUESTS = 20;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String adminToken;
    private String user1Token;
    private String user2Token;

    private LocalDateTime baseDay;

    @BeforeAll
    void login() throws Exception {
        adminToken = login("admin@test.com");
        user1Token = login("user1@test.com");
        user2Token = login("user2@test.com");
        baseDay = LocalDateTime.now(ZoneOffset.UTC).plusDays(7).truncatedTo(ChronoUnit.DAYS);
    }

    // ------------------------------------------------------------------
    // 1. Jadwal bentrok (sequential)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Booking yang bentrok dengan booking CONFIRMED ditolak 409, yang bersebelahan tetap diterima")
    void overlappingBookingIsRejected() throws Exception {
        long roomId = createRoom();

        // booking awal 10:00 - 12:00
        ApiResponse first = book(user1Token, roomId, at(10, 0), at(12, 0));
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.body().get("status").asString()).isEqualTo("CONFIRMED");

        // semua variasi tumpang-tindih harus ditolak, walaupun oleh user lain
        Map<String, LocalDateTime[]> overlapping = Map.of(
                "slot persis sama", new LocalDateTime[] { at(10, 0), at(12, 0) },
                "overlap di awal", new LocalDateTime[] { at(9, 0), at(10, 30) },
                "overlap di akhir", new LocalDateTime[] { at(11, 30), at(13, 0) },
                "berada di dalam", new LocalDateTime[] { at(10, 30), at(11, 30) },
                "membungkus seluruhnya", new LocalDateTime[] { at(9, 0), at(13, 0) });

        for (var entry : overlapping.entrySet()) {
            ApiResponse res = book(user2Token, roomId, entry.getValue()[0], entry.getValue()[1]);
            assertThat(res.status()).as(entry.getKey()).isEqualTo(409);
            assertThat(res.body().get("error_code").asString()).as(entry.getKey()).isEqualTo("ROOM_CONFLICT");
        }

        // bersebelahan (end == start) tidak dianggap bentrok, rentang [start, end)
        assertThat(book(user2Token, roomId, at(9, 0), at(10, 0)).status()).isEqualTo(201);
        assertThat(book(user2Token, roomId, at(12, 0), at(13, 0)).status()).isEqualTo(201);

        assertThat(confirmedCount(roomId)).isEqualTo(3);
        assertThat(overlappingPairs(roomId)).isZero();
    }

    @Test
    @DisplayName("Slot yang booking-nya sudah dibatalkan bisa dibooking ulang")
    void cancelledBookingFreesTheSlot() throws Exception {
        long roomId = createRoom();

        ApiResponse first = book(user1Token, roomId, at(14, 0), at(15, 0));
        assertThat(first.status()).isEqualTo(201);
        assertThat(book(user2Token, roomId, at(14, 0), at(15, 0)).status()).isEqualTo(409);

        ApiResponse cancel = send("PATCH", "/bookings/" + first.body().get("id").asLong() + "/cancel",
                user1Token, null);
        assertThat(cancel.status()).isEqualTo(200);
        assertThat(cancel.body().get("status").asString()).isEqualTo("CANCELLED");

        assertThat(book(user2Token, roomId, at(14, 0), at(15, 0)).status()).isEqualTo(201);
    }

    // ------------------------------------------------------------------
    // 2. Concurrent requests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("20 request paralel untuk slot yang sama: tepat 1 sukses (201), sisanya 409")
    void concurrentRequestsForSameSlot_onlyOneSucceeds() throws Exception {
        long roomId = createRoom();

        List<ApiResponse> responses = runConcurrently(PARALLEL_REQUESTS,
                i -> () -> book(i % 2 == 0 ? user1Token : user2Token, roomId, at(10, 0), at(11, 0)));

        Map<Integer, Long> byStatus = countByStatus(responses);
        assertThat(byStatus).as("distribusi status HTTP: %s", byStatus)
                .containsOnlyKeys(201, 409)
                .containsEntry(201, 1L)
                .containsEntry(409, (long) PARALLEL_REQUESTS - 1);

        assertThat(responses).filteredOn(r -> r.status() == 409)
                .allSatisfy(r -> assertThat(r.body().get("error_code").asString()).isEqualTo("ROOM_CONFLICT"));

        assertThat(confirmedCount(roomId)).as("booking CONFIRMED di database").isEqualTo(1);
    }

    @Test
    @DisplayName("Request paralel dengan rentang waktu tumpang-tindih tidak pernah menghasilkan double booking")
    void concurrentOverlappingRanges_neverDoubleBook() throws Exception {
        long roomId = createRoom();

        // tiap request: durasi 1 jam, mulai bergeser 15 menit -> 08:00, 08:15, ...,
        // 12:45
        // sehingga hampir setiap pasang request saling tumpang-tindih sebagian
        List<ApiResponse> responses = runConcurrently(PARALLEL_REQUESTS, i -> () -> {
            LocalDateTime start = at(8, 0).plusMinutes(15L * i);
            return book(i % 2 == 0 ? user1Token : user2Token, roomId, start, start.plusHours(1));
        });

        Map<Integer, Long> byStatus = countByStatus(responses);
        assertThat(byStatus).as("distribusi status HTTP: %s", byStatus).containsOnlyKeys(201, 409);

        long created = byStatus.getOrDefault(201, 0L);
        assertThat(created).isPositive();
        assertThat(confirmedCount(roomId)).isEqualTo(created);
        assertThat(overlappingPairs(roomId)).as("pasangan booking CONFIRMED yang bentrok").isZero();
    }

    @Test
    @DisplayName("Request paralel untuk slot yang berbeda semuanya sukses")
    void concurrentRequestsForDifferentSlots_allSucceed() throws Exception {
        long roomId = createRoom();
        int requests = 10;

        // slot 1 jam berurutan tanpa overlap: 08-09, 09-10, ..., 17-18
        List<ApiResponse> responses = runConcurrently(requests, i -> () -> book(
                i % 2 == 0 ? user1Token : user2Token, roomId, at(8 + i, 0), at(9 + i, 0)));

        assertThat(responses).as("status: %s", countByStatus(responses))
                .allSatisfy(r -> assertThat(r.status()).isEqualTo(201));
        assertThat(confirmedCount(roomId)).isEqualTo(requests);
    }

    // ------------------------------------------------------------------
    // helper
    // ------------------------------------------------------------------

    private record ApiResponse(int status, JsonNode body) {
    }

    /**
     * Menjalankan {@code count} request di thread berbeda dan melepas semuanya
     * secara bersamaan (CountDownLatch) agar benar-benar berebut di server.
     */
    private List<ApiResponse> runConcurrently(int count, Function<Integer, Callable<ApiResponse>> requestFactory)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ApiResponse>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Callable<ApiResponse> request = requestFactory.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return request.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).as("semua thread siap").isTrue();
            start.countDown(); // lepaskan semua request bersamaan

            List<ApiResponse> responses = new ArrayList<>();
            for (Future<ApiResponse> f : futures) {
                responses.add(f.get(30, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    private static Map<Integer, Long> countByStatus(List<ApiResponse> responses) {
        return responses.stream().collect(Collectors.groupingBy(ApiResponse::status, Collectors.counting()));
    }

    private LocalDateTime at(int hour, int minute) {
        return baseDay.withHour(hour).withMinute(minute);
    }

    private String login(String email) throws Exception {
        ApiResponse res = send("POST", "/auth/login", null,
                Map.of("email", email, "password", "password123"));
        assertThat(res.status()).as("login %s", email).isEqualTo(200);
        return res.body().get("token").asString();
    }

    private long createRoom() throws Exception {
        ApiResponse res = send("POST", "/rooms", adminToken,
                Map.of("name", "Test Room " + UUID.randomUUID(), "capacity", 10));
        assertThat(res.status()).isEqualTo(201);
        return res.body().get("id").asLong();
    }

    private ApiResponse book(String token, long roomId, LocalDateTime start, LocalDateTime end) throws Exception {
        return send("POST", "/bookings", token, Map.of(
                "room_id", roomId,
                "title", "Meeting " + start.format(ISO),
                "start_time", start.format(ISO),
                "end_time", end.format(ISO)));
    }

    private ApiResponse send(String method, String path, String token, Object body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1" + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .method(method, body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> res = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        JsonNode json = res.body() == null || res.body().isBlank()
                ? objectMapper.nullNode()
                : objectMapper.readTree(res.body());
        return new ApiResponse(res.statusCode(), json);
    }

    private long confirmedCount(long roomId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE room_id = ? AND status = 'CONFIRMED'",
                Long.class, roomId);
    }

    /**
     * Jumlah pasangan booking CONFIRMED di ruangan yang rentang waktunya saling
     * tumpang-tindih.
     */
    private long overlappingPairs(long roomId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM bookings a
                JOIN bookings b
                  ON a.room_id = b.room_id
                 AND a.id < b.id
                 AND a.start_time < b.end_time
                 AND a.end_time   > b.start_time
                WHERE a.room_id = ?
                  AND a.status = 'CONFIRMED'
                  AND b.status = 'CONFIRMED'
                """, Long.class, roomId);
    }
}
