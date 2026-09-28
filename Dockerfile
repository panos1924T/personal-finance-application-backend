# Build stage
FROM amazoncorretto:21 AS build

RUN dnf install -y findutils && dnf clean all

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test --no-daemon


# Runtime stage
FROM amazoncorretto:21

WORKDIR /app

COPY --from=build /app/build/libs/et.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]