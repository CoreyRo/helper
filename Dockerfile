FROM openjdk:11-jre

WORKDIR /etc/groovy/helper

# Copy dir to the image
COPY ./ ./

# Make gradle wrapper executable
USER root
RUN chmod +x ./gradlew

# Build jar
RUN ./gradlew build

ENTRYPOINT java -jar ./build/libs/groovy-helper.jar