## 1. Gitea Setup Steps (One-time)

Run `docker compose up gitea-db gitea-server`:

1. **Open** `http://localhost:3000`
2. **Run install wizard** → set admin account
3. **Create organization:** `bioinformatics`
4. **Create repository:** `config-repo` (initialize with README)
5. **Create a user** `config-user` (or use your admin) and generate an **Access Token** with `repo` scope
6. **Clone locally, add `YAML`files , push:**

```bash
git clone http://localhost:3000/bioinformatics/config-repo.git
cd config-repo
# ... create application.yml, bioinformatics-dashboard.yml, etc ...
git add .
git commit -m "Initial config"
git push origin main
```

### `.env` additions

Add these to your existing `.env` file:

```bash
# ── Gitea ────────────────────────────────────────────────────────────────────
GITEA_DB_PASSWORD=gitea_secure_password
GITEA_HTTP_PORT=3000
GITEA_SSH_PORT=222

# Gitea config-repo access (create this user/token in Gitea UI first)
GITEA_CONFIG_USER=config-user
GITEA_CONFIG_PASSWORD=your-gitea-token-or-password

# ── Config Server ────────────────────────────────────────────────────────────
CONFIG_SERVER_PORT=8888
CONFIG_ENCRYPT_KEY=ChangeMeInProduction-32CharKey!!

# ── Backend Profile ──────────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=dev
```