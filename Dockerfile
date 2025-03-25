FROM alpine/git
WORKDIR /app
RUN git clone --branch main https://github.com/mdix-inc/fhir.terminology.git

FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY --from=0 /app/fhir.terminology/ /app
RUN mvn install
ENTRYPOINT ["java","-jar","/app/target/com.mdix.fhir.terminology.jar"]
