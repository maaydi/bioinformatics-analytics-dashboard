# ── Build stage ─────────────────────────────────────────────────────────────
# Removed as Build run locally in script devops/scripts/build-all.sh
# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security (OWASP A05)
RUN addgroup -S bioapp && adduser -S bioapp -G bioapp
RUN mkdir -p /app/logs && chown -R bioapp:bioapp /app/logs

USER bioapp

COPY target/*.jar import-service.jar

EXPOSE 8083

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "import-service.jar"]
