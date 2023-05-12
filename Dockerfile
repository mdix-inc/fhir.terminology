FROM alpine/git
WORKDIR /app
RUN git clone --branch dockerupdate https://github.com/MDMI/mdmiFhirTerminology.git

FROM maven:3.9-eclipse-temurin-20-alpine
WORKDIR /app
COPY --from=0 /app/mdmiFhirTerminology /app 
RUN mvn install 

COPY ./conceptmaps /conceptmaps

ENTRYPOINT ["java","-jar","/app/target/com.mdixinc.fhir.terminology-2.0.0.jar"]
