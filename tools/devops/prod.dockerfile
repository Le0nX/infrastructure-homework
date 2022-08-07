FROM gradle:7.5.0-jdk11-alpine AS build

ARG VCS_REF
ARG BUILD_DATE

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar -PbuildSHA=${VCS_REF} --warning-mode=all --no-daemon

FROM openjdk:11-jre-slim

ARG PORT
ARG VCS_REF
ARG BUILD_DATE

EXPOSE ${PORT}
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/people.jar

ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/people.jar"]


LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.title="people-api-release" \
      org.opencontainers.image.authors="Denis Nefedov <nefedov.d.d@gmail.com>" \
      org.opencontainers.image.source="https://github.com/Le0nX/infrastructure-homework" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.vendor="stringconcat.com"