FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl payment-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
EXPOSE 8087
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
