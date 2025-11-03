FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS builder

LABEL stage=builder
LABEL service=ado-demo-mcp

WORKDIR /app

COPY gradle ./gradle
COPY gradlew ./
COPY build.gradle ./
COPY settings.gradle ./

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew clean bootJar --no-daemon -x test

FROM mcr.microsoft.com/openjdk/jdk:21-distroless

LABEL maintainer="marco.villarreal@microsoft.com"
LABEL service=ado-demo-mcp
LABEL version=0.0.1-SNAPSHOT

COPY --from=builder /app/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=default \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
    SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]