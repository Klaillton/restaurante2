# syntax=docker/dockerfile:1
# Multi-stage: compila com Maven + JDK 21 e corre num JRE 21 sem root.

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# Os testes (incluindo o gate do pedido_evento com Testcontainers) correm na CI, não aqui:
# dentro do docker build não há Docker para o Testcontainers.
RUN mvn -B -ntp -DskipTests package && cp target/restaurante2-*.jar /build/app.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system restaurante && useradd --system --gid restaurante restaurante
WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
USER restaurante
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
