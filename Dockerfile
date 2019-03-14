FROM openjdk:11-jre-slim as BUILDER
WORKDIR /etc/groovy/helper
COPY ./ ./
USER root
RUN chmod +x ./gradlew
RUN ./gradlew build

FROM openjdk:11-jre-slim as RUNNER
COPY --from=BUILDER ./etc/groovy/helper/build/libs/ .
ENTRYPOINT java -jar ./groovy-helper.jarI