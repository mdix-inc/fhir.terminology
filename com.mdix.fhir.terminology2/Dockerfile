# Start with a base image containing Java runtime
FROM openjdk:11

# Add Maintainer Info
LABEL maintainer="trial"

COPY ./v2tofhir /v2tofhir
COPY ./localmaps /localmaps

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
