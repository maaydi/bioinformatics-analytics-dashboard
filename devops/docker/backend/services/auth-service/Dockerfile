# ── Build stage ─────────────────────────────────────────────────────────────
# Removed as Build run locally in script devops/scripts/build-all.sh
# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security (OWASP A05)
RUN addgroup -S bioapp && adduser -S bioapp -G bioapp
RUN mkdir -p /app/logs && chown -R bioapp:bioapp /app/logs

USER bioapp

COPY target/*.jar auth-service.jar

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=50.0", \
  "-jar", "auth-service.jar"]
