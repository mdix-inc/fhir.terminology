# Start with a base image containing Java runtime
FROM openjdk:8-jdk-alpine

# Add Maintainer Info
LABEL maintainer="trial"

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file
ARG JAR_FILE=target/hapi-fhir-spring-boot-sample-server-jersey-4.1.0.jar

# Add the application's jar to the container
COPY ${JAR_FILE} fhirspringboot.jar

CMD ["bundle",  "exec", "rails", "server", "-e", "production"]

ENV CLASSPATH /maps 

# Run the jar file 
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fhirspringboot.jar"]