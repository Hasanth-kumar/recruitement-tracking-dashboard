# RTS Backend — Day 1 Scaffold

This scaffold matches the updated local-only plan:
- Spring Boot 3.x / Java 17
- MySQL 8.0 only
- JPA `ddl-auto` manages schema
- No Flyway
- JWT security baseline started
- Admin seed is handled on startup

## One-time local MySQL setup

```sql
CREATE DATABASE IF NOT EXISTS rts_dev;
CREATE DATABASE IF NOT EXISTS rts_test;
```

## Run

```bash
mvn spring-boot:run
```

Use:
- `application.yml` for local development
- `application-test.yml` for tests
- `application-prod.yml` for production validation mode
