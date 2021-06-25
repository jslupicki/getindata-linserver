FROM openjdk:15.0-slim

COPY target/getindata-linserver-0.0.1-SNAPSHOT.jar getindata-linserver.jar
COPY *.txt .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "getindata-linserver.jar"]

