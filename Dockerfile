FROM alpine/git
WORKDIR /app
RUN git clone --branch main https://github.com/mdix-inc/fhir.terminology.git

# FROM maven:3.9.9-eclipse-temurin-21
FROM maven:3.9-eclipse-temurin-21 AS builder
ENV MAVEN_OPTS="-Xmx2g -Xms512m"
WORKDIR /app
COPY --from=0 /app/fhir.terminology/ /app
RUN mvn dependency:go-offline
RUN mvn clean package -DskipTests
FROM eclipse-temurin:21-jdk-alpine

# RUN mvn install
ENTRYPOINT ["java","-jar","/app/target/com.mdix.fhir.terminology.jar"]
