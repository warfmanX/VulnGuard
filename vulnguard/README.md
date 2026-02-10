# VulnGuard

VulnGuard is a Spring Boot 3.x (Java 17) application for tracking and analyzing security vulnerabilities across system assets.

## Tech Stack

- Spring Boot 3.x
- Spring Web (REST)
- Spring Data JPA
- PostgreSQL
- Thymeleaf
- Jsoup (vulnerability feed scraping)

## Domain Model

- **Vulnerability**: ID, title, description, severity score (0.0–10.0), published date.
- **SystemAsset**: ID, hostname, IP address, OS, importance level.
- **ScanReport**: ID, status, timestamp, linked to a single asset and many vulnerabilities.

Relationships:

- One `SystemAsset` → many `ScanReport`.
- Many `ScanReport` ↔ many `Vulnerability`.

## Running Locally

1. Ensure PostgreSQL is running and create a database:

   ```sql
   CREATE DATABASE vulnguard;
   ```

2. Adjust credentials in `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/vulnguard
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   ```

3. Build and run:

   ```bash
   mvn spring-boot:run
   ```

4. Access the dashboard:

   - `http://localhost:8080/dashboard`

## Key REST Endpoints

- **Vulnerabilities**
  - `GET /api/v1/vulnerabilities`
  - `GET /api/v1/vulnerabilities/{id}`
  - `POST /api/v1/vulnerabilities`
  - `PUT /api/v1/vulnerabilities/{id}`
  - `DELETE /api/v1/vulnerabilities/{id}`
  - `POST /api/v1/vulnerabilities/scrape-latest` – uses Jsoup to scrape a simplified vulnerability feed and persist results.

- **System Assets**
  - `GET /api/v1/assets`
  - `GET /api/v1/assets/{id}`
  - `POST /api/v1/assets`
  - `PUT /api/v1/assets/{id}`
  - `DELETE /api/v1/assets/{id}`

- **Scan Reports**
  - `GET /api/v1/scan-reports`
  - `GET /api/v1/scan-reports/{id}`
  - `POST /api/v1/scan-reports`
  - `PUT /api/v1/scan-reports/{id}`
  - `DELETE /api/v1/scan-reports/{id}`

## Error Handling

- 404 responses use a structured JSON body when entities are not found.
- Validation errors on DTOs return 400 with field-level messages.

