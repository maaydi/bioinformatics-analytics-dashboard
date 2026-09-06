# ── Build stage ─────────────────────────────────────────────────────────────
# Removed as Build run locally in script devops/scripts/build-all.sh
# ── Runtime stage (nginx) ─────────────────────────────────────────────────────
FROM nginx:1.25-alpine AS runtime

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

COPY frontend/dist/bioinformatics-dashboard-frontend/browser /usr/share/nginx/html
COPY devops/docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
