FROM openjdk:11-jdk as builder
ARG JAR_FILE=./project.war
COPY ${JAR_FILE} app.war
ENTRYPOINT ["java","-jar","app.war"]

