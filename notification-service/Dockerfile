FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl notification-service -am -DskipTests

# Extraction stage
FROM eclipse-temurin:21-jre AS extract
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Final stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
EXPOSE 8085
COPY notification-service/entrypoint.sh /entrypoint.sh
RUN sed -i 's/\r$//' /entrypoint.sh && chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
