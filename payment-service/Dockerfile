FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY target/*.jar app.jar
USER spring:spring
EXPOSE 8086
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
