# ── Build stage ─────────────────────────────────────────────────────────────
# Removed as Build run locally in script devops/scripts/build-all.sh
# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

ENV EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE='http://localhost:8761/eureka/'

WORKDIR /app

# Non-root user for security (OWASP A05)
RUN addgroup -S bioapp && adduser -S bioapp -G bioapp
RUN mkdir -p /app/logs && chown -R bioapp:bioapp /app/logs

USER bioapp

COPY target/*.jar dashboard.jar

# Import temp directory
RUN mkdir -p /tmp/bio-import

EXPOSE 8084

ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Deureka.client.serviceUrl.defaultZone=$EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE -jar dashboard.jar"]
