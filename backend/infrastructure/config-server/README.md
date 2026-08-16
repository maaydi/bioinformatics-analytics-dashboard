## Encrypting Sensitive Properties

### Step 1: Start only Config Server (or use its `/encrypt` endpoint once up)

```bash
curl -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "my-super-secret-jwt-key"
```

### Step 2: Paste the output into your config repo

```yaml
app:
  jwt:
    secret: '{cipher}AQCq1sN...encrypted-value...=='
```

The Config Server will automatically decrypt this when serving to clients **only if** the client and server share the
same `encrypt.key` (symmetric) or the server uses asymmetric keys and the client has the public key.

> **In Production:** Replace symmetric `encrypt.key` with a JKS keystore for asymmetric encryption so clients cannot
> encrypt values, only the server can decrypt.

---

## Verification

```bash
# Config Server health
curl http://localhost:8888/actuator/health

# Raw config for your app + dev profile
curl http://localhost:8888/bioinformatics-dashboard/dev

```
