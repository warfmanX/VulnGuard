# ==== Stage 1: Build ====
FROM maven:3-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies first (better layer caching)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# ==== Stage 2: Run ====
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the fat jar from the builder stage
COPY --from=builder /app/target/vulnguard-0.0.1-SNAPSHOT.jar app.jar

# Expose application port
EXPOSE 8080

# Optional: JVM options hook
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]