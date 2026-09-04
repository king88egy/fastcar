FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY desktop/dist/FastCar/FastCarServer.jar /app/FastCarServer.jar
RUN mkdir -p /app/data
VOLUME /app/data
EXPOSE 8080

ENV SMTP_HOST=smtp.gmail.com
ENV SMTP_PORT=587
ENV SMTP_USER=
ENV SMTP_PASS=

CMD ["java", "-cp", "FastCarServer.jar", "com.fastcar.server.ServerMain"]
