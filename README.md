# College ERP Core Service

Multi-tenant **College ERP + Learning Management System + real-time communication platform**, built as a
**modular monolith**: one Spring Boot application, one deployable unit, one primary database, with strict
internal module boundaries.

Many colleges share one deployment. Each college is a tenant, and a user of one college can never read or
write another college's data.

---

## Architecture

```
                    ┌──────────────────────────────────────┐
                    │      core-service (ONE Spring Boot   │
                    │        application / container)      │
                    │                                      │
   HTTP  ──────────▶│  REST  /api/v1/**                    │
   STOMP ──────────▶│  WebSocket  /ws                      │
                    │                                      │
                    │  auth · college · user · role ·      │
                    │  permission · module · academic ·    │
                    │  student · teacher · classroom ·     │
                    │  attendance · syllabus · assignment ·│
                    │  material · announcement · chat ·    │
                    │  meeting · notification · file ·     │
                    │  dashboard · audit                   │
                    └───────┬───────────┬──────────┬───────┘
                            │           │          │
                            ▼           ▼          ▼
                         MySQL        Redis      Kafka
                    (source of truth) (cache,   (async
                                       tokens,   fan-out,
                                       limits)   optional)
```

This is **not** microservices. There is no gateway, no service discovery, no inter-service HTTP and no
database per module. Modules talk to each other through:

| Mechanism | Used for |
|---|---|
| Published service interfaces | Synchronous cross-module reads and authorization checks |
| Spring application events | Decoupled reactions (notifications, audit) after a transaction commits |
| Kafka (optional) | Asynchronous fan-out inside the same application, off the request thread |

### Module boundaries

Each module owns its tables and exposes a narrow interface to the rest of the application. Other modules
depend on the interface, never on the entities or repositories behind it.

| Interface | Owner | Consumers |
|---|---|---|
| `ClassroomAccessService` | `classroom` | assignment, attendance, material, syllabus, chat, meeting, announcement, dashboard |
| `AcademicLookupService` | `academic` | attendance, syllabus, teacher, material |
| `StudentLookupService` | `student` | dashboard, attendance |
| `FileStorageService` | `file` | material, assignment, chat |
| `NotificationDispatcher` | `notification` | event listeners only |
| `AuditRecorder` | `audit` | any module recording a sensitive action |
| `ModuleAccessGuard` | `module` | every feature module |

### Package layout

```
com.CoreService.CoreService
├── common                 shared infrastructure only
│   ├── config             JPA auditing, async, cache, OpenAPI, Redis, BCrypt
│   ├── context            CollegeContext, UserContext (ThreadLocal)
│   ├── dto                JwtDto
│   ├── entity             BaseEntity, TenantAwareEntity, AuditableEntity
│   ├── event              DomainEvent
│   ├── exception          exception hierarchy + GlobalExceptionHandler
│   ├── jwt                JwtService, JwtFilter
│   ├── redis              RedisService
│   ├── response           ApiResponse, PageResponse, BasicResponse
│   ├── security           SecurityConfig, TenantGuard, Permissions, ModuleCodes,
│   │                      RoleNames, DefaultRolePermissions, token blacklist
│   ├── web                RateLimitFilter
│   └── websocket          STOMP config, authentication, destination authorization
│
├── auth        College     user        role        Permission   module
├── academic    student     teacher     classroom   attendance
├── syllabus    assignment  material    announcement
├── chat        meeting     notification
└── file        dashboard   audit
```

Feature modules follow `controller / service / repository / entity / dto / mapper / enums / event`.
The `College`, `Permission` and `module` packages predate this convention and keep capitalised
sub-packages (`Controller`, `Entities`, `Services`, `Repository`); they behave identically.

---

## Multi-tenancy

Every tenant-scoped table has a `college_id` column, and every tenant-scoped entity extends
`TenantAwareEntity`.

```
Bearer token
     │
     ▼
JwtFilter ──▶ validates signature, expiry and revocation
     │
     ├──▶ UserContext.userId
     └──▶ CollegeContext.collegeId        ← taken from the SIGNED TOKEN
                │
                ▼
        Controller ──▶ Service ──▶ Repository (always filters on college_id)
```

Rules enforced in code:

1. **The tenant is never taken from client input.** `CollegeContext` is populated from the signed JWT.
   The one exception is `MAIN_ADMIN`, the platform operator, who belongs to no college and may target
   one explicitly with the `X-College-Id` header. For every other user that header is ignored.
2. Every repository method that touches tenant data filters on `college_id`.
3. `TenantGuard.assertSameCollege(...)` is the single place ownership is checked, and it reports a
   cross-tenant hit as **404 Not Found** rather than 403, so a caller cannot use the response to discover
   that a resource exists in another college.
4. `ThreadLocal` contexts are cleared in a `finally` block on every request and every STOMP frame.

Before an operation runs, the stack answers:

```
authenticated? ──▶ which college? ──▶ is the module enabled? ──▶ has the permission?
      ──▶ does the resource belong to this college? ──▶ is the caller a member/owner?
```

---

## Security

| Concern | Implementation |
|---|---|
| Authentication | JWT access token (`Authorization: Bearer …`) + opaque refresh token in MySQL |
| Password hashing | BCrypt |
| Access token claims | `sub` (userId), `collegeId`, `roles`, `permissions`, `modules`, `jti` |
| Logout | Refresh token deleted **and** access token `jti` blacklisted in Redis until it expires |
| Refresh | Rotating: the presented token is consumed and a new pair issued |
| Authorization | Dynamic RBAC — `Role` → `RolePermission` → `Permission`, asserted with `@PreAuthorize` |
| Module gating | `ModuleAccessGuard.assertModuleEnabled(...)` per college |
| Classroom gating | `ClassroomAccessService.assertPermission(...)` per classroom |
| WebSocket | STOMP `CONNECT` frames authenticated; `SUBSCRIBE` frames authorized deny-by-default |
| Rate limiting | Redis fixed window, stricter budget on `/api/v1/auth/**` |
| Error responses | No stack traces, no internal messages |

`@EnableMethodSecurity` is on `SecurityConfig` — without it every `@PreAuthorize` in the application would
be silently ignored.

### Password rules

Changing or resetting a password revokes all of that user's refresh tokens. `POST /auth/forgot-password`
always reports success so user ids cannot be enumerated.

---

## Real-time communication

STOMP over WebSocket at **`/ws`**.

| Direction | Destination | Notes |
|---|---|---|
| client → server | `/app/chat/send`, `/app/chat/join` | sender identity comes from the session, never the payload |
| client → server | `/app/meeting/signal` | WebRTC signaling relay |
| server → client | `/topic/classroom/{classroomId}` | classroom members only |
| server → client | `/topic/chat/{chatRoomId}` | room participants only |
| server → client | `/topic/meeting/{meetingId}` | meeting participants only |
| server → client | `/topic/college/{collegeId}` | own college only |
| server → client | `/user/queue/notifications` | own session only |
| server → client | `/user/queue/signal` | own session only |

The handshake itself is open; nothing can be sent or received until a `CONNECT` frame carries a valid
access token. Subscriptions are authorized by `WebSocketDestinationAuthorizer`, which **denies any
destination no module has claimed** — each module contributes a `StompSubscriptionRule` for the
destinations it owns.

### Video conference

The backend manages meetings, participants, permissions, lifecycle and **WebRTC signaling only**. It never
carries audio or video. `VideoConferenceProvider` abstracts the media layer so Jitsi, LiveKit or Agora can
replace the built-in WebRTC signaling without touching business code.

---

## Technology

Java 21 · Spring Boot 4.1 · Spring Security · Spring Data JPA / Hibernate · MySQL 8 · Redis 7 ·
Apache Kafka · Spring WebSocket + STOMP · JJWT · Flyway · springdoc-openapi · Lombok ·
Jakarta Validation · Maven · Docker · JUnit 5 · Mockito

> **Build note.** The build targets Java 21 but also compiles on newer JDKs. JDK 23+ no longer runs
> annotation processors found on the classpath implicitly, so Lombok is declared explicitly in
> `maven-compiler-plugin.annotationProcessorPaths`. Removing that makes every Lombok-generated getter
> disappear at compile time.

---

## Getting started

### Clone and run (MySQL + Redis already installed)

**Need on the laptop:** JDK 21+, MySQL on `localhost:3306`, Redis on `localhost:6379`.

```bash
git clone <your-repo-url>
cd core-service
```

If your MySQL password is not `root`, set it before starting:

```powershell
$env:DATABASE_PASSWORD = "your-mysql-password"
```

**Windows — one command:**

```bat
run.bat
```

**Or:**

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw spring-boot:run
```

Then open:

- API — <http://localhost:8082>
- Swagger — <http://localhost:8082/swagger-ui.html>

**First login** (created automatically on an empty DB):

| Field | Value |
|---|---|
| userId | `MainAdmin001` |
| password | `Admin@123` |

```bash
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userId":"MainAdmin001","password":"Admin@123"}'
```

Then, as `MAIN_ADMIN`: create a college, enable its modules, and create its `COLLEGE_ADMIN`. Use
`X-College-Id: <collegeId>` to act inside a specific college.

### Optional: everything in Docker

```bash
docker compose up --build
```

### Optional: infra in Docker, app from IDE

```bash
docker compose up -d mysql redis
./mvnw spring-boot:run
```

### Tests

```bash
./mvnw test
```

Tests run against in-memory H2 in MySQL compatibility mode with Flyway disabled, so no Docker is
needed. The tenant-isolation suite (`TenantGuardTest`, `WebSocketDestinationAuthorizerTest`) asserts the
core guarantee that a college A user cannot reach college B data.

---

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `DATABASE_URL` | `jdbc:mysql://localhost:3306/collegeerp?...` | JDBC URL |
| `DATABASE_USERNAME` | `root` | Database user |
| `DATABASE_PASSWORD` | `root` | Database password |
| `MYSQL_PORT` | `3306` | Host port for the MySQL container |
| `DATABASE_POOL_SIZE` | `20` | Hikari maximum pool size |
| `JWT_SECRET` | local-dev default | HMAC signing key — change for production |
| `JWT_ACCESS_EXPIRATION` | `900000` | Access token lifetime (ms, 15 min) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token lifetime (ms, 7 days) |
| `BOOTSTRAP_ADMIN_USER_ID` | `MainAdmin001` | Platform operator user id |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@college-erp.local` | Platform operator email |
| `BOOTSTRAP_ADMIN_PASSWORD` | `Admin@123` | Platform operator password |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `KAFKA_ENABLED` | `false` | Turn on asynchronous Kafka processing |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `STORAGE_PROVIDER` | `local` | File storage backend |
| `STORAGE_LOCAL_PATH` | `./storage` | Local storage root |
| `STORAGE_MAX_FILE_SIZE` | `26214400` | Upload limit in bytes (25 MB) |
| `PUSH_ENABLED` | `false` | Enable a push notification provider |
| `MEETING_PROVIDER` | `internal-webrtc` | Video conference provider |
| `RATE_LIMIT_ENABLED` | `true` | Request throttling |
| `RATE_LIMIT_RPM` | `300` | Requests per minute per caller |
| `RATE_LIMIT_AUTH_RPM` | `20` | Requests per minute on `/api/v1/auth/**` |

Local defaults are for development only — change `JWT_SECRET` and `BOOTSTRAP_ADMIN_PASSWORD` before any shared or production deploy.

---

## Database

MySQL 8 is the single source of truth. Redis holds only derived or short-lived data (cache, OTPs,
refresh-token bookkeeping, revoked access tokens, rate-limit counters).

- UUID primary keys everywhere except `users` (natural key `user_id`) and `colleges` (`college_id`).
- Every tenant table carries `college_id`, indexed, plus indexes on `user_id`, `classroom_id`,
  `student_user_id`, `email` and `created_at` where they are queried.
- Foreign keys and unique constraints are declared in the migrations, not just in JPA annotations.
- `spring.jpa.hibernate.ddl-auto=validate` — **Hibernate never creates or alters the schema.**
- Tables are `InnoDB` with `utf8mb4` / `utf8mb4_unicode_ci`.

Two MySQL-specific mapping choices, both set in `application.properties`:

| Setting | Why |
|---|---|
| `hibernate.type.preferred_uuid_jdbc_type=CHAR` | MySQL has no UUID type. The default would be `binary(16)`, which is compact but means you cannot select a row by pasting an id from a log or an API response. Keys are `varchar(36)` instead. |
| `@JdbcTypeCode(SqlTypes.VARCHAR)` on enum columns | Hibernate maps `@Enumerated(STRING)` to MySQL's native `ENUM` type, which bakes the constant list into the column — adding one `NotificationType` would need an `ALTER TABLE`. The annotation keeps them `varchar`. |

### Migrations

Flyway owns the schema. Scripts live in `src/main/resources/db/migration` and are applied in order:

| Script | Tables |
|---|---|
| `V1__core_schema.sql` | `colleges`, `users`, `auth_refresh_tokens`, `roles`, `permissions`, `user_roles`, `role_permissions`, `system_modules`, `college_modules` |
| `V2__academic_schema.sql` | `academic_departments`, `academic_sessions`, `academic_programs`, `academic_semesters`, `academic_subjects`, `students`, `teachers`, `teacher_subjects` |
| `V3__classroom_schema.sql` | `classrooms`, `classroom_members`, `classroom_permissions` |
| `V4__attendance_schema.sql` | `attendance_sessions`, `attendance_records` |
| `V5__notification_announcement_audit_schema.sql` | `notifications`, `device_tokens`, `announcements`, `audit_logs` |
| `V6__learning_schema.sql` | `stored_files`, `syllabi`, `syllabus_units`, `syllabus_topics`, `assignments`, `assignment_attachments`, `assignment_submissions`, `submission_attachments`, `study_materials` |
| `V7__communication_schema.sql` | `chat_rooms`, `chat_participants`, `chat_messages`, `meetings`, `meeting_participants` |

Never edit an applied migration — add a new one.

Because the application runs with `ddl-auto=validate`, a migration that drifts from the JPA mappings is a
startup failure. `MysqlSchemaExportTest` guards that: it exports the MySQL DDL implied by the entities and
diffs every table, column and column type against these scripts, so drift fails the build instead of the
deployment — and it needs no database to do it.

Some schema conventions worth knowing:

- Two columns are renamed because Hibernate emits identifiers unquoted and the natural name is reserved
  in MySQL: `AcademicSession.current` maps to **`is_current`**, and `Notification.read` maps to
  **`is_read`**.
- `created_by`, `announcements.created_by` and `audit_logs.user_id` deliberately have **no** foreign key.
  Authorship and an audit trail have to outlive the account that produced them. Rows that are meaningless
  without their user (notifications, device tokens, memberships, submissions) do cascade.
- `file_id` columns in `study_materials`, `assignment_attachments`, `submission_attachments` and
  `chat_messages` are soft references. The file module owns file lifecycle, one stored file may be
  referenced from several places, and deleting content must not delete the bytes.

Reference data (permission catalogue, ERP module catalogue, the `MAIN_ADMIN` role and the bootstrap admin)
is seeded idempotently by `CommandLineRunner` initializers at startup, so the catalogue can evolve in code.

---

## API

Base path `/api/v1`. Interactive documentation:

- Swagger UI — <http://localhost:8082/swagger-ui.html>
- OpenAPI JSON — <http://localhost:8082/v3/api-docs>

### Response envelope

```json
{ "success": true, "message": "Operation successful", "data": { } }
```

Paged:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "last": true }
}
```

Errors never include a stack trace:

```json
{
  "success": false,
  "message": "Resource not found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "timestamp": "2026-01-01T10:15:30Z",
  "path": "/api/v1/classrooms/…"
}
```

| `errorCode` | HTTP |
|---|---|
| `VALIDATION_FAILED` | 400 |
| `BUSINESS_RULE_VIOLATION` | 400 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN`, `MODULE_DISABLED` | 403 |
| `RESOURCE_NOT_FOUND` | 404 |
| `DUPLICATE_RESOURCE` | 409 |
| `RATE_LIMIT_EXCEEDED` | 429 |
| `INTERNAL_ERROR` | 500 |

### Endpoint groups

| Prefix | Module |
|---|---|
| `/api/v1/auth` | login, refresh, logout, forgot/verify/reset/change password |
| `/api/v1/college` | college registration and settings |
| `/api/v1/users` | user accounts |
| `/api/v1/roles`, `/api/v1/permissions` | dynamic RBAC |
| `/api/v1/modules`, `/api/v1/college-modules` | per-college module toggles |
| `/api/v1/academic` | departments, programs, sessions, semesters, subjects |
| `/api/v1/students`, `/api/v1/teachers` | academic person records |
| `/api/v1/classrooms` | classrooms, members, classroom permissions |
| `/api/v1/attendance` | sessions, marking, reports |
| `/api/v1/syllabus` | syllabi, units, topics, progress |
| `/api/v1/assignments` | assignments, submissions, grading |
| `/api/v1/materials` | study material |
| `/api/v1/announcements` | announcements |
| `/api/v1/chat` | rooms, history, permissions |
| `/api/v1/meetings` | scheduling, lifecycle, join |
| `/api/v1/notifications` | notification history, device tokens |
| `/api/v1/files` | upload, download, delete |
| `/api/v1/dashboard` | student, teacher and college-admin dashboards |
| `/api/v1/audit` | audit log search |

---

## Events

```
AssignmentService
      │  publishes AssignmentPublishedEvent
      ▼
Spring ApplicationEventPublisher
      │
      ├──▶ NotificationEventListener   (@TransactionalEventListener + @Async)
      └──▶ AuditEventListener          (@TransactionalEventListener + @Async)
```

Listeners fire **after commit**, so a failed notification can never roll back business data, and they read
the tenant from the event payload because `ThreadLocal` context does not exist on the async thread. Audit
writes use `REQUIRES_NEW` for the same reason.

With `KAFKA_ENABLED=true`, notification events additionally go through the `erp.notifications` topic;
consumers deduplicate on `DomainEvent.eventKey()` via Redis so redelivery is harmless. **Kafka here is
intra-application asynchronous processing — it is not inter-service messaging, and the application is fully
functional with Kafka switched off.**

---

## Known limitations

Extension points that are deliberately wired but not implemented, and behaviour worth knowing before you
rely on it:

| Area | State |
|---|---|
| Password reset email | The OTP is generated and held in Redis for 5 minutes, but no mail transport is configured — nothing is delivered yet. Wire an `EmailSender` into `AuthService.generateOtp`. |
| Push notifications | `PushNotificationProvider` is an interface with a no-op implementation. Firebase is not a dependency; adding `FirebasePushNotificationProvider` and setting `PUSH_ENABLED=true` is all that is needed. |
| File storage | Only `LocalFileStorageProvider` exists. MinIO and S3 slot in behind `FileStorageProvider` with no change to business modules. |
| Video conference | Only the built-in WebRTC signaling provider exists. Setting `MEETING_PROVIDER` to anything else leaves no `VideoConferenceProvider` bean and the context fails to start — an intentional signal rather than a silent fallback. |
| `DEPARTMENT` announcements | No department-membership lookup is published by any module, so a department-targeted announcement currently reaches the whole college. It therefore requires `COLLEGE_ADMIN`, exactly like a college-wide one, so it cannot be used to widen an audience. The department id is stored, so narrowing it later needs no data migration. |
| Teacher dashboard | Classroom names are resolved through `ClassroomAccessService` rather than joined in SQL — one call per classroom the teacher owns. Counts and aggregates are single queries. |
| Integration tests | The suite is unit tests, a full application-context load, and the schema diff described above. There are no Testcontainers tests, so nothing exercises real MySQL, Redis or Kafka; the development environment used to build this had no Docker daemon available. The first `docker compose up` is therefore the first time Flyway actually runs. |

## Conventions

- Controllers receive a validated DTO, call a service and return `ApiResponse`. No business logic.
- Services hold business rules, authorization and transactions. Constructor injection only.
- Repositories hold queries. No business logic.
- JPA entities never cross the HTTP boundary; `password` is never serialized.
- New permission codes follow `NOUN_VERB` (`ASSIGNMENT_GRADE`). The original user/role/college codes use
  `VERB_NOUN` (`CREATE_USER`) and are kept for compatibility.
