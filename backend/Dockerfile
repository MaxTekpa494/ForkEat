FROM maven:3.9-eclipse-temurin-25-alpine

WORKDIR /app

COPY pom.xml /app/

COPY . /app/

CMD ["mvn", "spring-boot:run"]