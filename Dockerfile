# Build stage
FROM clojure AS build

WORKDIR /build

# Copy dependencies file
COPY deps.edn /build/

# Copy source and resources
COPY src /build/src
COPY resources /build/resources

# Build the uberjar
RUN clojure -T:slim build

# Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from the build stage
# project.clj specifies :uberjar-name "pigeon-scoops.jar"
COPY --from=build /build/target/pigeon-scoops.jar /app/pigeon-scoops.jar
COPY --from=build /build/resources/server-config.edn /app/resources/server-config.edn
COPY --from=build /build/resources/db-task-config.edn /app/resources/db-task-config.edn

# Default environment variables
ENV PORT=3000
ENV JDBC_DATABASE_URL=""
ENV TEST_CLIENT_ID=""
ENV MANAGEMENT_CLIENT_ID=""
ENV MANAGEMENT_CLIENT_SECRET=""

EXPOSE ${PORT}

# Run the application
# server.clj -main takes config-file as an argument
CMD ["sh", "-c", "java -cp pigeon-scoops.jar clojure.main -m pigeon-scoops-backend.db-tasks -c resources/db-task-config.edn -t db-tasks/migration && java -jar pigeon-scoops.jar resources/server-config.edn"]
