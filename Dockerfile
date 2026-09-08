# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dependencies are resolved first so code changes do not invalidate the layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /var/lib/core-service/storage \
    && chown -R spring:spring /var/lib/core-service

COPY --from=build /workspace/target/*.jar app.jar
RUN chown spring:spring /app/app.jar

USER spring:spring
EXPOSE 8082

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
