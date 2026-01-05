FROM ubuntu


ENV DEBIAN_FRONTEND=noninteractive

RUN apt update && \
    apt-get install openjdk-17-jdk -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


COPY ./target/IAM-0.0.1-SNAPSHOT.jar app.jar



CMD ["java", "-jar", "app.jar"]