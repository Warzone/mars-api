FROM openjdk:8
WORKDIR /usr/mars-api
COPY build/libs/mars-api-1.0-SNAPSHOT-all.jar ./mars-api.jar
CMD ["java", "-jar", "mars-api.jar"]
HEALTHCHECK --interval=30s --timeout=2s --retries=1 --start-period=30s CMD curl --fail http://localhost:3000/status || exit 1