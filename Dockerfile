# Run stage (use pre-built jar)
FROM eclipse-temurin:21-jre-alpine

# Install Python3 and pip
RUN apk add --no-cache python3 py3-pip

# Set working directory
WORKDIR /app

# Copy JAR file
COPY build/libs/*.jar app.jar

# Copy python-analysis directory
COPY python-analysis /app/python-analysis

# Install Python dependencies
RUN pip3 install --no-cache-dir pandas psycopg2-binary numpy scipy --break-system-packages

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
