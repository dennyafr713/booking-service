# booking-service

API Sistem Booking Ruang Meeting.

Fitur utama:

- Login dengan email & password, mendapatkan **JWT token**.
- Admin dapat **menambah ruangan**.
- User dapat **mencari ruangan yang tersedia** pada rentang waktu tertentu.
- User dapat **membooking ruangan** — dijamin **tidak ada jadwal bentrok**, termasuk saat banyak request datang bersamaan.
- User dapat **membatalkan booking** miliknya (admin dapat membatalkan booking siapa pun).

---

## Daftar Isi

1. [Requirement](#1-requirement)
2. [Konfigurasi (.env)](#2-konfigurasi-env)
3. [Cara Menjalankan](#3-cara-menjalankan)
4. [Akun Bawaan](#4-akun-bawaan)
5. [Dokumentasi API (Swagger)](#5-dokumentasi-api-swagger)
6. [Daftar Endpoint & Contoh curl](#6-daftar-endpoint--contoh-curl)
7. [Format Error](#7-format-error)
8. [Menjalankan Test](#8-menjalankan-test)
9. [Struktur Project](#9-struktur-project)

---

## 1. Requirement

| Kebutuhan | Versi | Keterangan |
|---|---|---|
| Java (JDK) | 17 | Wajib jika menjalankan aplikasi tanpa Docker |
| Maven | 3.9+ | Untuk build & menjalankan test |
| Docker + Docker Compose | Terbaru | Untuk menjalankan database / seluruh aplikasi, **dan wajib untuk menjalankan test** |
| PostgreSQL | 16 | Tidak perlu di-install jika memakai Docker |

Teknologi yang dipakai: Spring Boot 4.2, Spring Security (JWT), Spring Data JPA, PostgreSQL, springdoc-openapi (Swagger), Testcontainers.

> **Catatan:** Database **harus PostgreSQL**. Aturan anti-bentrok memakai fitur khusus PostgreSQL
> (`EXCLUDE USING gist` dari extension `btree_gist`), sehingga tidak bisa diganti H2/MySQL.

---

## 2. Konfigurasi (.env)

Salin file contoh lalu sesuaikan bila perlu:

```bash
cp .env.example .env
```

| Variabel | Default | Keterangan |
|---|---|---|
| `APP_PORT` | `8080` | Port aplikasi |
| `SWAGGER_ENABLED` | `true` | Set `false` di production untuk mematikan Swagger |
| `DB_HOST` | `localhost` | Host PostgreSQL (hanya untuk jalan lokal) |
| `DB_PORT` | `5432` | Port PostgreSQL |
| `DB_NAME` | `booking` | Nama database |
| `DB_USERNAME` | `booking` | User database |
| `DB_PASSWORD` | `booking` | Password database |
| `JWT_SECRET` | – | Secret untuk tanda tangan JWT, **minimal 32 karakter** |
| `JWT_EXPIRATION_MINUTES` | `1440` | Masa berlaku token (menit), default 24 jam |

File `.env` otomatis dibaca oleh Spring Boot maupun Docker Compose. Environment variable dari OS selalu lebih diprioritaskan.

---

## 3. Cara Menjalankan

Semua URL API diawali dengan **`/api/v1`**, contoh: `http://localhost:8080/api/v1/auth/login`.

### Opsi A — Semua lewat Docker Compose (paling mudah)

Tidak perlu install Java/Maven, cukup Docker.

```bash
cp .env.example .env
docker compose up -d --build
```

Perintah di atas menjalankan 2 container:

- `booking-service-db` → PostgreSQL 16, tabel & data awal otomatis dibuat dari `src/main/resources/schema.sql`
- `booking-service-app` → aplikasi di `http://localhost:8080/api/v1`

Perintah lain yang berguna:

```bash
docker compose logs -f app   # melihat log aplikasi
docker compose down          # menghentikan semua container
docker compose down -v       # menghentikan + MENGHAPUS data database (reset total)
```

> `schema.sql` hanya dijalankan **saat database pertama kali dibuat**. Jika skema diubah,
> jalankan `docker compose down -v` lalu `docker compose up -d --build` lagi.

### Opsi B — Aplikasi jalan lokal, database di Docker (untuk development)

```bash
cp .env.example .env
docker compose up -d db      # hanya menjalankan PostgreSQL
mvn spring-boot:run          # menjalankan aplikasi
```

### Opsi C — Memakai PostgreSQL sendiri (tanpa Docker)

1. Buat database dan user sesuai isi `.env`.
2. Jalankan skema + data awal:
   ```bash
   psql -h localhost -U booking -d booking -f src/main/resources/schema.sql
   ```
3. Jalankan aplikasi:
   ```bash
   mvn spring-boot:run
   ```

Untuk membuat file JAR:

```bash
mvn clean package -DskipTests
java -jar target/booking-service.jar
```

---

## 4. Akun Bawaan

Dibuat otomatis oleh `schema.sql`. Password semua akun: **`password123`**

| Email | Role | Hak akses |
|---|---|---|
| `admin@test.com` | ADMIN | Semua endpoint, termasuk tambah ruangan & batalkan booking milik siapa pun |
| `user1@test.com` | USER | Cari ruangan, booking, batalkan booking milik sendiri |
| `user2@test.com` | USER | Sama seperti `user1` |

---

## 5. Dokumentasi API (Swagger)

Setelah aplikasi berjalan, buka:

- **Swagger UI:** http://localhost:8080/api/v1/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api/v1/v3/api-docs

Cara mencoba endpoint yang butuh login di Swagger:

1. Jalankan `POST /auth/login`, salin nilai `token` dari response.
2. Klik tombol **Authorize** (ikon gembok) di kanan atas.
3. Tempel token **tanpa** awalan `Bearer `, lalu klik **Authorize**.

---

## 6. Daftar Endpoint & Contoh curl

| Method | Endpoint | Akses | Fungsi |
|---|---|---|---|
| `POST` | `/auth/login` | Publik | Login, mendapatkan JWT |
| `POST` | `/rooms` | ADMIN | Menambah ruangan |
| `GET` | `/rooms/available` | Login | Mencari ruangan yang kosong |
| `POST` | `/bookings` | Login | Membooking ruangan |
| `PATCH` | `/bookings/{id}/cancel` | Login (pemilik / ADMIN) | Membatalkan booking |

**Hal penting:**

- Semua nama field JSON memakai **snake_case** (`room_id`, `start_time`, dst).
- Format waktu: **`yyyy-MM-ddTHH:mm:ss`** (contoh `2026-12-01T10:00:00`), semua waktu dalam **UTC**.
- Endpoint selain login wajib mengirim header `Authorization: Bearer <token>`.

Agar contoh di bawah mudah di-copy, siapkan variabel berikut terlebih dahulu:

```bash
BASE_URL=http://localhost:8080/api/v1
```

### 6.1 Login — `POST /auth/login`

```bash
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user1@test.com",
    "password": "password123"
  }'
```

Response `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...."
}
```

Simpan token ke variabel agar bisa dipakai di request berikutnya
(membutuhkan [`jq`](https://jqlang.org/); jika tidak ada, salin token secara manual):

```bash
# token user biasa
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@test.com","password":"password123"}' | jq -r .token)

# token admin (dibutuhkan untuk menambah ruangan)
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password123"}' | jq -r .token)
```

Kemungkinan error: `400 VALIDATION_ERROR` (email/password kosong), `401 INVALID_CREDENTIALS` (email atau password salah).

### 6.2 Tambah Ruangan — `POST /rooms` (khusus ADMIN)

```bash
curl -X POST "$BASE_URL/rooms" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Ruang Meeting Melati",
    "capacity": 10
  }'
```

Response `201 Created`:

```json
{
  "id": 1,
  "name": "Ruang Meeting Melati",
  "capacity": 10,
  "is_active": true
}
```

Aturan: `name` wajib diisi (maksimal 100 karakter), `capacity` wajib diisi (minimal 1).
Kemungkinan error: `400 VALIDATION_ERROR`, `401 UNAUTHORIZED`, `403 FORBIDDEN` (bukan admin).

### 6.3 Cari Ruangan Tersedia — `GET /rooms/available`

```bash
curl -G "$BASE_URL/rooms/available" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "start_time=2026-12-01T10:00:00" \
  --data-urlencode "end_time=2026-12-01T12:00:00"
```

Response `200 OK` — daftar ruangan aktif yang **tidak** punya booking `CONFIRMED` di rentang waktu tersebut:

```json
[
  {
    "id": 1,
    "name": "Ruang Meeting Melati",
    "capacity": 10
  }
]
```

Kemungkinan error: `400 VALIDATION_ERROR` (parameter kosong / format salah), `400 INVALID_TIME` (`end_time` tidak lebih besar dari `start_time`), `401 UNAUTHORIZED`.

### 6.4 Booking Ruangan — `POST /bookings`

```bash
curl -X POST "$BASE_URL/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "room_id": 1,
    "title": "Weekly Sync Tim Backend",
    "start_time": "2026-12-01T10:00:00",
    "end_time": "2026-12-01T12:00:00"
  }'
```

Response `201 Created`:

```json
{
  "id": 1,
  "room_id": 1,
  "user_id": 2,
  "title": "Weekly Sync Tim Backend",
  "start_time": "2026-12-01T10:00:00",
  "end_time": "2026-12-01T12:00:00",
  "status": "CONFIRMED"
}
```

Aturan booking:

- `room_id`, `title` (maksimal 150 karakter), `start_time`, `end_time` wajib diisi.
- `end_time` harus lebih besar dari `start_time`, dan `start_time` tidak boleh di masa lalu.
- Ruangan harus ada dan masih aktif.
- **Tidak boleh bentrok** dengan booking `CONFIRMED` lain di ruangan yang sama.
  Booking yang hanya **bersebelahan** tetap boleh, contoh: 10:00–12:00 dan 12:00–13:00 tidak dianggap bentrok.
- Aman terhadap request bersamaan: jika banyak orang membooking slot yang sama di waktu yang sama,
  **hanya satu yang berhasil**, sisanya mendapat `409 ROOM_CONFLICT`.

Contoh response jika jadwal bentrok, `409 Conflict`:

```json
{
  "error_code": "ROOM_CONFLICT",
  "message": "Ruangan sudah dibooking pada rentang waktu tersebut"
}
```

Kemungkinan error lain: `400 VALIDATION_ERROR`, `400 INVALID_TIME`, `400 ROOM_INACTIVE`, `404 ROOM_NOT_FOUND`, `401 UNAUTHORIZED`.

### 6.5 Batalkan Booking — `PATCH /bookings/{id}/cancel`

```bash
curl -X PATCH "$BASE_URL/bookings/1/cancel" \
  -H "Authorization: Bearer $TOKEN"
```

Response `200 OK`:

```json
{
  "id": 1,
  "status": "CANCELLED"
}
```

Setelah dibatalkan, slot waktu tersebut bisa dibooking lagi.
Kemungkinan error: `404 BOOKING_NOT_FOUND`, `403 FORBIDDEN` (bukan pemilik booking dan bukan admin),
`409 BOOKING_ALREADY_CANCELLED`, `401 UNAUTHORIZED`.

---

## 7. Format Error

Semua error memakai format yang sama:

```json
{
  "error_code": "VALIDATION_ERROR",
  "message": "title: judul tidak boleh kosong"
}
```

| HTTP | `error_code` | Arti |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Field / parameter wajib kosong atau tidak valid |
| 400 | `INVALID_REQUEST_BODY` | Body bukan JSON yang valid / format field salah |
| 400 | `INVALID_TIME` | Waktu tidak valid (selesai ≤ mulai, atau mulai di masa lalu) |
| 400 | `ROOM_INACTIVE` | Ruangan tidak aktif |
| 401 | `UNAUTHORIZED` | Token tidak ada, tidak valid, atau sudah expired |
| 401 | `INVALID_CREDENTIALS` | Email atau password salah |
| 403 | `FORBIDDEN` | Tidak punya hak akses untuk aksi tersebut |
| 404 | `ROOM_NOT_FOUND` | Ruangan tidak ditemukan |
| 404 | `BOOKING_NOT_FOUND` | Booking tidak ditemukan |
| 404 | `NOT_FOUND` | Endpoint tidak ditemukan |
| 405 | `METHOD_NOT_ALLOWED` | HTTP method tidak didukung |
| 409 | `ROOM_CONFLICT` | Jadwal bentrok dengan booking lain |
| 409 | `BOOKING_ALREADY_CANCELLED` | Booking sudah dibatalkan sebelumnya |
| 500 | `INTERNAL_SERVER_ERROR` | Kesalahan tak terduga di server |

---

## 8. Menjalankan Test

> **Docker harus dalam keadaan berjalan.** Test memakai [Testcontainers](https://testcontainers.com/)
> yang otomatis menyalakan PostgreSQL 16 sementara, jadi tidak perlu menyiapkan database sendiri.

```bash
mvn test                                              # semua test
mvn test -Dtest=BookingConcurrencyIntegrationTest     # hanya test bentrok & concurrency
```

`BookingConcurrencyIntegrationTest` mengirim request HTTP sungguhan ke `POST /api/v1/bookings` dan menguji:

| Skenario | Hasil yang diharapkan |
|---|---|
| Booking yang tumpang-tindih (slot sama, overlap di awal/akhir, di dalam, membungkus) | `409 ROOM_CONFLICT` |
| Booking yang hanya bersebelahan (akhir = mulai) | `201 Created` |
| Booking dibatalkan, lalu slotnya dibooking ulang | `201 Created` |
| **20 request paralel** untuk slot yang sama | Tepat **1** × `201`, **19** × `409`, di database hanya ada 1 booking |
| 20 request paralel dengan rentang waktu yang saling tumpang-tindih | Tidak ada double booking di database |
| 10 request paralel untuk slot yang berbeda-beda | Semuanya `201` |

Request paralel dilepas bersamaan memakai `CountDownLatch` agar benar-benar saling berebut di server.

---

## 9. Struktur Project

```
src/main/java/com/booking/api
├── config/        # Konfigurasi OpenAPI / Swagger
├── controller/    # REST controller (Auth, Room, Booking)
├── enums/         # Role, BookingStatus
├── exception/     # Error code & global exception handler
├── model/         # Entity JPA & DTO request/response
├── repository/    # Spring Data JPA repository
├── security/      # Konfigurasi Spring Security & JWT
└── service/       # Business logic
src/main/resources
├── application.yaml
└── schema.sql     # Skema tabel + data awal (akun bawaan)
src/test/java/com/booking/api
├── BookingConcurrencyIntegrationTest.java   # Test bentrok jadwal & concurrent request
└── TestcontainersConfiguration.java         # PostgreSQL untuk test (Testcontainers)
queries.sql        # Contoh query: rekap jam pemakaian ruangan & pencarian ruangan tersedia
```
