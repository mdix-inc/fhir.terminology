FROM alpine/git
WORKDIR /app
RUN git clone --branch mdmiThreeOh https://github.com/MDMI/MDMITransformationService.git

FROM maven:3.9-eclipse-temurin-11
WORKDIR /app
COPY --from=0 /app/MDMITransformationService /app 
RUN mvn install 

COPY ./conceptmaps /conceptmaps

ENTRYPOINT ["java","-jar","/app/target/org.mdmi.transformation.service.jar"]




# Start with a base image containing Java runtime
FROM openjdk:11

# Add Maintainer Info
LABEL maintainer="trial"


COPY ./conceptmaps /conceptmaps

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file
ARG JAR_FILE=target/com.mdixinc.fhir.terminology-2.0.0.jar

# Add the application's jar to the container
COPY ${JAR_FILE} fhirspringboot.jar

CMD ["bundle",  "exec", "rails", "server", "-e", "production"]

ENV CLASSPATH /maps 

# Run the jar file 
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fhirspringboot.jar"]
