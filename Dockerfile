FROM openjdk:8
WORKDIR /usr/mars-api
COPY build/libs/mars-api-1.0-SNAPSHOT-all.jar ./mars-api.jar
CMD ["java", "-jar", "mars-api.jar"]