FROM openjdk:11-jre-slim

ARG VCS_REF
ARG BUILD_DATE

EXPOSE 8080

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/people.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/people.jar"]

LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.title="people-api-dev" \
      org.opencontainers.image.authors="Denis Nefedov <nefedov.d.d@gmail.com>" \
      org.opencontainers.image.source="https://github.com/Le0nX/infrastructure-homework" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.vendor="stringconcat.com"