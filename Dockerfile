# Base Alpine Linux based image with OpenJDK JRE only
FROM openjdk:14-alpine
# copy application WAR (with libraries inside)
COPY target/*.jar /app.jar
# specify default command
# CMD ["java", "-jar", "-Dspring.profiles.active=test", "/app.jar"]
CMD ["java", "-jar", "/app.jar"]