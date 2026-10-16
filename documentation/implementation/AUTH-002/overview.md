# AUTH-002 — Keycloak Integration via Backend-for-Frontend

| Metadata         | Value                                                             |
|------------------|-------------------------------------------------------------------|
| Type             | Technical and normative security specification                    |
| Revision / date  | 1.0 / 2026-10-16                                                  |
| Architecture     | Approved BFF; integration to implement                            |
| Owners           | Architecture, IAM/Security, Backend, Frontend, and Platform teams |
| Project baseline | Java 25, Spring Boot 4.0.6, Spring Security 7, Angular 21         |
| Identifier       | **AUTH-002**                                                      |

The terms **MUST**, **MUST NOT**, and **SHOULD** respectively express an obligation, a prohibition, and a recommendation
to justify any waiver. The excerpts are implementation contracts, not a modification of the existing stack. They are
not, by themselves, a complete configuration or proof of approval.

## 0. Scope, decisions, and initial state

The objective is to delegate identity to Keycloak and remove OAuth tokens from the browser without changing the business
functionality of the dashboard. `auth-service` becomes the sole application HTTP façade for Angular and the sole
confidential interactive OIDC client. The gateway and services remain internal components.

The current state is based on the `/api/auth/login` and `/api/auth/refresh` endpoints, HMAC JWTs, `sessionStorage`, a
Bearer interceptor, and a Servlet security `STATELESS` policy with CSRF disabled. Nginx currently forwards `/api/`
directly to the gateway, which rewrites authentication to `/api/v1/auth/**` and passes identity headers. **This behavior
is not compatible with the target without coordinated migration.**

This decision replaces, for browser authentication and access, the historical choices of "without OIDC" and "without
BFF" from ARCH-001 and the token storage from AUTH-001. It complements the trust boundaries considered by ARCH-002; it
does not assume that these hardening measures are already in place.

Reference decisions:

- Shared **stateful** session between BFF instances; the cookie contains only an opaque random identifier.
- OAuth 2.0 Authorization Code + OIDC + PKCE `S256`, confidential client; no implicit flow or Password Grant.
- Same public origin for Angular and the BFF; Keycloak on a subdomain of the same HTTPS **site**.
- All business APIs pass through the BFF, which adds tokens only on server-to-server communications.
- CSRF enabled; `SameSite=Strict` and `HttpOnly` are not considered sufficient protections on their own.
- Federation with an IdP on another site and deployment on distinct registrable domains are outside this reference
  configuration; they require a review of the return flow before authorization.

## 1. ARCHITECTURE & AUTHENTICATION FLOW (BFF)

### 1.1 Justification and trust boundaries

| Aspect                  | Direct OAuth SPA client                                          | Target BFF AUTH-002                                                                         |
|-------------------------|------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| Client type             | Public, no secret protectable in the browser                     | Confidential, secret available only server-side                                             |
| Access/refresh tokens   | Available at JavaScript runtime, even without persistent storage | Kept exclusively on the BFF                                                                 |
| Consequence of an XSS   | Theft and reuse of tokens outside the browser is possible        | No direct exfiltration of OAuth tokens; actions on behalf of the session are still possible |
| Renewal                 | Logic and concurrency in the SPA                                 | Centralized and locked server-side                                                          |
| Revocation / expiration | State distributed across tabs and clients                        | Centralized session, revocable and auditable                                                |
| CSRF                    | Different with explicit Bearer                                   | Cookie-specific risk: mandatory synchronizer token                                          |
| Cost                    | Frontend OAuth is more complex                                   | Session storage, BFF proxy, and additional availability                                     |

The BFF greatly reduces the exposure surface for tokens; it does not remove XSS, CSRF, or the need for server
authorization. An XSS can still read business responses and perform authenticated requests: strict CSP, Angular without
bypassing sanitization, Trusted Types when supported, dependency control, and absence of unapproved third-party scripts
remain necessary.

```text
Browser: https://app.bio.example.com
  ├─ assets / UI routes ───────────────> Nginx ──> Angular static app
  ├─ /api/** ─────────────────────────> Nginx ──> auth-service (BFF)
  ├─ /oauth2/**, /login/oauth2/** ─────> Nginx ──> auth-service (BFF)
  └─ login navigation ──────────────────> https://sso.bio.example.com
                                                    │ Keycloak
 auth-service ── code / tokens / revocation ───────────┘
 auth-service ── session + OAuth clients ──> Shared private Redis
 auth-service ── server Bearer ────────────> private api-gateway ──> services
 Keycloak ────────────────────────────────> Dedicated IAM PostgreSQL
```

Angular → BFF exclusivity applies to the application's API. Navigation to Keycloak login pages is the protocol
exception, not a SPA access to its token endpoints. The browser never calls the gateway, microservices, token endpoint,
refresh endpoint, or UserInfo directly.

### 1.2 Origins, Strict cookies, and URL resolution

Reference origins: application `https://app.bio.example.com`, IAM `https://sso.bio.example.com`, issuer
`https://sso.bio.example.com/realms/bioinformatics`. The two origins are different, but they share the same **schemeful
site**: HTTPS and registrable domain `example.com`. In production, replace these reserved names with controlled
organization names.

The `__Host-BFFSESSION` cookie is **host-only**, without a `Domain` attribute: Keycloak must neither receive it nor be
able to set it for the application. A malicious subdomain remains a same-site threat; also verify `Origin`, CSRF tokens,
and subdomain ownership.

**A Strict cookie may be absent on callback if the IdP is on another site**, which prevents retrieval of the session
containing the authorization request and `state`. Silently switching it to `Lax` or disabling `state` verification is
forbidden. Same-site deployment as described above and real validation of redirect chains are production requirements,
including after SSO session expiration and after MFA. Cross-site federation flows are not validated by this
specification.

For local development, use `https://app.bio.test` and `https://sso.bio.test`, an explicit DNS/hosts resolution, and a
development CA approved by both the browser **and the JVM**. The local proxy sends the BFF routes to `auth-service:8081`
and the IAM name to Keycloak. Do not lower `Secure`/`Strict` for a non-HTTP profile. A port-only change does not create
a different site; it may still change the origin and trigger CORS.

The public issuer is identical for all components. The internal DNS or proxy alias allows the BFF to resolve the same
HTTPS name from Docker; do not arbitrarily replace the issuer with `http://keycloak:8080`. The certificate, SANs, and
TLS validation must remain valid.

### 1.3 Normative login sequence

1. Angular calls `GET /api/auth/me`. Without an authenticated session, the BFF responds with `401` JSON, never an HTML
   Keycloak page to an XHR call.
2. A user action, guard, or expiration coordinator triggers a full-page navigation to `/oauth2/authorization/keycloak`.
   Any return destination is previously validated and stored server-side.
3. Spring creates a pre-authentication session and already emits the opaque cookie
   `HttpOnly; Secure; SameSite=Strict; Path=/`. It stores the authorization request: `state`, `nonce`, PKCE verifier,
   and return destination.
4. The BFF responds with `302` to Keycloak's authorization endpoint: `response_type=code`, scopes
   `openid profile email`, PKCE challenge `S256`, exact redirect URI, and fresh `state`/`nonce`. No client secret is
   included in this URL.
5. Keycloak performs authentication, password policy enforcement, MFA, and possible SSO session handling. Credentials do
   not transit through Angular or a login form in the application.
6. Keycloak returns the browser by GET to `https://app.bio.example.com/login/oauth2/code/keycloak?code=...&state=...`.
   **The single-use code and `state` may transit through the browser; no access/refresh token transits there.**
7. The BFF retrieves its Strict session, verifies `state` correlation, and exchanges the code over server-side HTTPS,
   using `client_secret_basic` and the PKCE verifier. Keycloak also validates the exact redirect URI.
8. Spring validates the ID token: signature and allowed algorithms, exact issuer, client audience, `azp` if applicable,
   validity dates, and `nonce`. The BFF applies the authorized role mapping; no claim provided by the browser is
   trusted.
9. The BFF changes the session identifier against fixation, stores the ID token and access/refresh tokens **server-side
   only**, associates the authorized client with the new session, and rotates the opaque cookie. The old identifier
   becomes unusable.
10. Response `303`/`302` to an authorized UI route, without code or token, followed by Angular fetching a minimal DTO
    via `/api/auth/me` and a new anti-CSRF token. Session responses use `Cache-Control: no-store`.
11. At each business call, the BFF verifies the session, rights, and CSRF as needed, renews tokens server-side when
    required, and relays to the internal gateway. The response never contains tokens or Keycloak internal cookies.

Proxy and BFF access logs must mask callback query strings, cookies, `Authorization`, codes, secrets, and login bodies;
`Referrer-Policy: no-referrer` applies to login/callback endpoints. No third-party script or analytics asset is loaded
on the callback.

### 1.4 Application routing and business relay

| Public route                                         | Real destination                | Rule                                         |
|------------------------------------------------------|---------------------------------|----------------------------------------------|
| `/oauth2/authorization/keycloak`                     | BFF, OAuth initiation           | Authorized navigation; controlled parameters |
| `/login/oauth2/code/keycloak`                        | BFF, Spring callback            | Mandatory OIDC checks                        |
| `/api/auth/me`, `/api/auth/csrf`, `/api/auth/logout` | BFF session controllers         | Local processing before relay                |
| Explicitly enumerated business routes `/api/...`     | BFF → private gateway → service | Required session, allowlist relay            |
| UI routes and assets                                 | Static frontend                 | No SPA fallback on previous routes           |

The public proxy does not publish a direct route to the gateway. Its configuration MUST replace the current Nginx
routing `/api/ → api-gateway` with `/api/ → auth-service`. OAuth routes must take precedence over the SPA fallback. BFF
endpoints never pass through the gateway: avoid the BFF → gateway → auth-service loop.

The relay is a fixed table of **method + path + destination**, not a generic proxy accepting a target URL. It removes
`Authorization`, `Cookie`, incoming identity headers `X-User-*`, and hop-by-hop headers; it reconstructs allowed proxy
headers and adds the appropriate server access token for the target audience. It does not relay service `Set-Cookie`
headers. Business parameters remain transmitted after validation; they never select an origin or network host.

Upload limits, streaming, backpressure, timeouts, and cancellation MUST preserve existing import/export capabilities
without loading a full file into memory. Redirects from services to another origin are not followed with the attached
Bearer. Potential SSE uses the same BFF session and explicit duration limits.

Gateway and target services MUST validate Keycloak access tokens: JWKS signature, `iss`, `exp`/`nbf`, allowed
algorithms, and **audience `bio-api`**. The BFF client is not necessarily the API audience: a Keycloak audience mapper
explicitly adds `bio-api`. Services no longer trust identity headers from an external request. An ID token is never an
API Bearer. Network segmentation prohibits browser access bypassing the BFF; it does not replace cryptographic
validation.

## 2. DOCKER COMPOSE CONFIGURATION (INFRASTRUCTURE)

### 2.1 Operational assumptions

Keycloak **26.8.1** has been verified as a stable release published on 2026-10-13. The version is pinned below and never
set to `latest`. The PostgreSQL **16.15-alpine** tag has also been verified in the official registry. Before each
release: CVE analysis of images/dependencies, selection of a maintained patch, and pinning of both image digests in the
release artifact.

This excerpt is intended to be **merged** with the existing Compose setup after validating names and networks. The TLS
reverse proxy and Redis are not redefined here. It exposes no host port for Keycloak, its database, or management. The
proxy joins `iam_edge`; **only Keycloak and PostgreSQL** join `iam_db`. Keycloak also uses `iam_edge` for explicitly
authorized egress (e.g., SMTP), with deployment network policy.

```yaml
services:
  keycloak-db:
    image: postgres:16.15-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD_FILE: /run/secrets/keycloak_db_admin_password
      POSTGRES_INITDB_ARGS: --auth-host=scram-sha-256
    secrets:
      - keycloak_db_admin_password
      - keycloak_db_password
    volumes:
      - keycloak_pg_data:/var/lib/postgresql/data
      - ./devops/secrets/keycloak-db-init.sh:/docker-entrypoint-initdb.d/10-keycloak-app.sh:ro
    networks:
      - iam_db
    healthcheck:
      test: [ CMD-SHELL, 'pg_isready -U postgres -d keycloak' ]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 30s
    mem_limit: 1g
    cpus: 1.0
    pids_limit: 256
    logging:
      driver: json-file
      options:
        max-size: 10m
        max-file: '5'

  keycloak:
    image: quay.io/keycloak/keycloak:26.8.1
    restart: unless-stopped
    # The official image uses its non-root user; do not replace it with root.
    entrypoint: [ /bin/bash, -ec ]
    command:
      - |
        export KC_DB_PASSWORD="$(< /run/secrets/keycloak_db_password)"
        export KC_BOOTSTRAP_ADMIN_PASSWORD="$(< /run/secrets/keycloak_bootstrap_admin_password)"
        exec /opt/keycloak/bin/kc.sh start
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-db:5432/keycloak
      KC_DB_USERNAME: keycloak_app
      # KC_DB_PASSWORD is exported from a secret by the wrapper above.
      KC_HOSTNAME: https://sso.bio.example.com
      KC_HOSTNAME_STRICT: 'true'
      KC_PROXY_HEADERS: xforwarded
      # TLS at the proxy; HTTP only on the private proxy -> Keycloak network.
      KC_HTTP_ENABLED: 'true'
      KC_HEALTH_ENABLED: 'true'
      KC_METRICS_ENABLED: 'true'
      KC_BOOTSTRAP_ADMIN_USERNAME: ${KEYCLOAK_ADMIN:?Temporary admin username required}
      JAVA_OPTS_KC_HEAP: -XX:MaxRAMPercentage=65 -XX:InitialRAMPercentage=30
    secrets:
      - keycloak_db_password
      - keycloak_bootstrap_admin_password
    depends_on:
      keycloak-db:
        condition: service_healthy
    networks:
      - iam_db
      - iam_edge
    healthcheck:
      # The Keycloak image does not include curl. Bash provides /dev/tcp.
      test: [ CMD-SHELL, "/bin/bash -ec 'exec 3<>/dev/tcp/127.0.0.1/9000; printf \"GET /health/ready HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n\" >&3; IFS= read -r status <&3; [[ \"$$status\" == *\" 200 \"* ]]'" ]
      interval: 15s
      timeout: 5s
      retries: 20
      start_period: 120s
    cap_drop: [ ALL ]
    security_opt: [ no-new-privileges:true ]
    tmpfs:
      - /tmp:size=128m,mode=1777
    mem_limit: 2g
    cpus: 2.0
    pids_limit: 512
    logging:
      driver: json-file
      options:
        max-size: 10m
        max-file: '5'

volumes:
  keycloak_pg_data:

networks:
  iam_db:
    internal: true
  iam_edge:
    driver: bridge

secrets:
  keycloak_db_admin_password:
    file: ${KEYCLOAK_DB_ADMIN_PASSWORD_FILE:?PostgreSQL administrator secret path required}
  keycloak_db_password:
    file: ${KEYCLOAK_DB_PASSWORD_FILE:?PostgreSQL application secret path required}
  keycloak_bootstrap_admin_password:
    file: ${KEYCLOAK_ADMIN_PASSWORD_FILE:?Keycloak bootstrap secret path required}
```

The provisioning file to mount on an **empty database**, `devops/secrets/keycloak-db-init.sh`, is a delivery artifact.
It contains no plaintext secret:

```bash
#!/usr/bin/env bash
set -Eeuo pipefail
app_password="$(< /run/secrets/keycloak_db_password)"
psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  --set=app_password="$app_password" <<'SQL'
CREATE ROLE keycloak_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION
  PASSWORD :'app_password';
ALTER DATABASE keycloak OWNER TO keycloak_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO keycloak_app;
SQL
unset app_password
```

The runtime Keycloak role can manage its schema, but is not a PostgreSQL superuser. This script is not run against an
already initialized volume; password rotation requires a SQL procedure and coordinated secret update, not just a file
change. Access to the PostgreSQL bootstrap account remains reserved for operators.

### 2.2 Variables, secrets, and production constraints

| Variable                                                      | Requirement                                                                                                                                        |
|---------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `KC_DB`                                                       | `postgres`; no embedded database in production                                                                                                     |
| `KC_DB_URL`                                                   | Dedicated IAM database; never the shared business schema                                                                                           |
| `KC_DB_USERNAME` / `KC_DB_PASSWORD`                           | Account `keycloak_app` limited to its database / secret injected by file and exported to the process                                               |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`                  | Historical bootstrap names; **deprecated**, do not present them as the recommended Keycloak 26 interface                                           |
| `KC_BOOTSTRAP_ADMIN_USERNAME` / `KC_BOOTSTRAP_ADMIN_PASSWORD` | Current interface used; the first reads the entry alias `KEYCLOAK_ADMIN`, the second reads the secret designated by `KEYCLOAK_ADMIN_PASSWORD_FILE` |
| `KC_HOSTNAME`                                                 | Public IAM HTTPS origin, fixed; not a dynamic hostname derived from a client header                                                                |
| `KC_PROXY_HEADERS`                                            | The proxy overwrites forwarded headers; only authorized proxy peers may reach this listener                                                        |

`KEYCLOAK_ADMIN_PASSWORD` is therefore not defined in cleartext in `environment`: its historical value corresponds to
the mounted secret content, exported as `KC_BOOTSTRAP_ADMIN_PASSWORD`. No real secret, production `.env`, or realm
export containing secrets should be versioned. Secret files are supplied by the secret manager, with read permissions
only for the container UID; do not make them readable to everyone to bypass a mount issue. Local Compose does not
automatically encrypt these files on the host, and a Docker administrator remains privileged.

The bootstrap account is created only at the initialization planned by Keycloak. After creating protected named accounts
with MFA, delete the temporary account and remove bootstrap variables, mounts, and exports from the deployment
definition. Keeping it in the permanent Compose is forbidden. The OIDC client secret is distinct from these
administrative secrets.

Other release requirements:

- The internal HTTP listener assumes TLS termination controlled on the same host/network of trust. For an unreliable
  inter-host network, use TLS also for proxy → Keycloak and JDBC `sslmode=verify-full` with a PostgreSQL CA.
  `internal: true` is not encryption.
- The proxy does not publish the management port `9000`, `/health`, `/metrics`, or admin interfaces publicly; restrict
  `/admin` by VPN/ACL and use a dedicated administration origin if needed. Define trusted proxy addresses according to
  the version capabilities and network ACLs; never accept arbitrary forwarded clients.
- `start` is a production mode, unlike `start-dev`. To accelerate and make startup reproducible, build an optimized
  image via `kc.sh build` with build-time options, then use `start --optimized`; do not add `--optimized` to the raw
  image without the matching build. IAM state remains in PostgreSQL, not the container filesystem.
- Define encrypted backups, tested restoration, retention, monitoring, and log rotation. A persistent volume is not a
  backup. Back up the database and configuration before upgrades; verify migrations and do not restart an older version
  on a migrated schema.
- This single-instance excerpt does **not** provide high availability: SLOs requiring HA require highly available
  PostgreSQL, multiple Keycloak instances with a suitable cache/cluster, HA Redis, and multiple BFFs. Measure startup,
  memory, and capacity instead of treating the indicated limits as final sizing.

### 2.3 Realm and client configuration

- Realm `bioinformatics`, TLS mandatory, public registration disabled by default, MFA for administrators, brute-force
  protection, and security event logging.
- Client `bio-bff`: **Client authentication ON**, Standard Flow ON, Implicit Flow OFF, Direct Access Grants OFF, Service
  Accounts OFF for this interactive client; PKCE `S256` required.
- Unique redirect URI `https://app.bio.example.com/login/oauth2/code/keycloak`; no wildcard. Web Origins are not opened
  to Angular: it does not call OIDC APIs with XHR. A post-logout URL is configured only for an explicitly validated
  flow.
- Logical resource `bio-api`, client roles `USER` and `ADMIN`, audience mapper adding `bio-api` in the access token.
  Realm roles may be used according to the same allowlist, without importing Keycloak administrative privileges.
- Role mappers configured **explicitly for the ID token** used by `oauth2Login` and for the API access token; no
  assumption that `realm_access` is present by default in the ID token. No role is inferred from an ID token decoded in
  Angular.
- Reference access token duration: 5 minutes; SSO/client idle session ≤ 30 minutes and maximum ≤ 8 hours, aligned with
  BFF policy. Rotating refresh tokens with controlled reuse policy; no `offline_access` scope for the dashboard.

## 3. BACKEND SPECIFICATIONS (SPRING BOOT — AUTH-SERVICE)

### 3.1 Dependencies and responsibilities

Explicitly declare the dependencies used by the BFF in `backend/services/auth-service/pom.xml`, even if `common-starter`
already provides OAuth2 transitively. Versions remain managed by the parent Spring Boot BOM:

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-session-data-redis</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.boot</groupId>
<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.security</groupId>
<artifactId>spring-security-test</artifactId>
<scope>test</scope>
</dependency>
```

Keep the service Servlet starter; do not implicitly transform the BFF into a WebFlux gateway. The relay can use a Spring
HTTP client suitable for streaming. Responsibilities are separate: OIDC configuration, session/client repository,
authority mapper, session controller, renewal policy, and business relay adapter. Remove the
`GatewayUserAuthenticationFilter` trust filter from the browser chain.

### 3.2 `application.yml` configuration

Example for the local **HTTPS** stack described in §1.2; in production replace the issuer with that of
`sso.bio.example.com`. Secrets are mandatory and have no default value:

```yaml
server:
  port: 8081
  forward-headers-strategy: framework
  servlet:
    session:
      timeout: 30m
      cookie:
        name: __Host-BFFSESSION
        http-only: true
        secure: true
        same-site: strict
        path: /
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            provider: keycloak
            client-id: bio-bff
            client-secret: ${KEYCLOAK_BFF_CLIENT_SECRET}
            client-authentication-method: client_secret_basic
            authorization-grant-type: authorization_code
            redirect-uri: '{baseUrl}/login/oauth2/code/{registrationId}'
            scope: [ openid, profile, email ]
        provider:
          keycloak:
            issuer-uri: ${KEYCLOAK_ISSUER_URI:https://sso.bio.test/realms/bioinformatics}
  session:
    timeout: 30m
    redis:
      namespace: bio:bff:sessions
  data:
    redis:
      host: ${BFF_REDIS_HOST}
      port: ${BFF_REDIS_PORT:6379}
      username: ${BFF_REDIS_USERNAME}
      password: ${BFF_REDIS_PASSWORD}
      ssl:
        enabled: true
      connect-timeout: 2s
      timeout: 2s
```

With Spring Session, provide an explicit `CookieSerializer` so as not to depend on differences in Servlet property
propagation:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Bean
CookieSerializer cookieSerializer() {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    serializer.setCookieName("__Host-BFFSESSION");
    serializer.setCookiePath("/");
    serializer.setUseHttpOnlyCookie(true);
    serializer.setUseSecureCookie(true);
    serializer.setSameSite("Strict");
    // No Domain; no OAuth token in the value.
    return serializer;
}
```

The absence of `Max-Age` makes the cookie non-persistent; it does not guarantee session destruction upon browser
closure, as some browsers restore sessions. Server-side durations remain authoritative. YAML properties do not replace
the absolute-duration policy or storage encryption.

`forward-headers-strategy: framework` is acceptable only if the BFF listener is inaccessible except through authorized
proxies that **overwrite** forwarded headers. Validate the public Host and scheme; `{baseUrl}` must never be controlled
by an arbitrary client Host/Forwarded header. The origin and callback must match exactly the Keycloak registration. The
current Config Server loads a Gitea Git repository: publish configuration changes in the source actually consumed and
verify active profiles, not just local `backend-config/`.

### 3.3 Spring Security chain contract

The target configuration uses Spring Security 7 lambda APIs and satisfies the following constraints:

1. `SessionCreationPolicy.IF_REQUIRED`, fixation protection with `changeSessionId`, persistence of the `SecurityContext`
   via Spring Session. The pre-authentication session and OIDC data survive session ID change safely.
2. `oauth2Login` with a standard `OidcUserService`, an explicit `GrantedAuthoritiesMapper` (§3.4), an authorization
   resolver with PKCE, a success handler returning only UI redirection, and a failure handler without leaking sensitive
   details.
3. `oauth2Client` and an `OAuth2AuthorizedClientManager` supporting `authorization_code`/`refresh_token`, connected to
   the **same shared repository** of authorized clients as the login. Do not leave an in-memory repository per instance
   as a production solution.
4. `csrf` **enabled**, `HttpSessionCsrfTokenRepository`, header name `X-XSRF-TOKEN`, and a CSRF request handler
   compatible with a raw token returned by the JSON endpoint. An XOR handler may be kept only if the exposed encoding
   and its validation are consistent and tested; do not mix masked and raw tokens.
5. Public authorizations limited to startup/callback endpoints, `GET /api/auth/csrf`, and the controlled error route.
   `/me` and all business APIs require an authenticated session. Health probes are on a non-public management
   network/port. No global `permitAll` on `/api/auth/**`.
6. An `AuthenticationEntryPoint` for `/api/**` returns `401 application/problem+json`; an `AccessDeniedHandler`
   distinguishes `ACCESS_DENIED` and `CSRF_INVALID` (`403`). API calls do not use the OAuth redirect entry point. A
   mutating unauthenticated request may be refused by CSRF before authentication: the client therefore cannot infer
   session state from the single `403` status.
7. Logout only by **POST** with CSRF; no GET logout. No CSRF exemption for business APIs. A dedicated back-channel
   logout endpoint may have a targeted exception because it authenticates a signed `logout_token`, not a session cookie.
8. Business authorization checks and `@EnableMethodSecurity` on the server; disable old HMAC/header trust filters on
   these routes. Reject unlisted paths without reintroducing incompatible double authentication.

The confidential client MUST also use PKCE; do not rely on an automatic activation reserved for public clients. Resolver
configuration to connect in `oauth2Login().authorizationEndpoint(...)`:

```java
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;

DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver(
        ClientRegistrationRepository registrations) {
    var resolver = new DefaultOAuth2AuthorizationRequestResolver(
            registrations, "/oauth2/authorization");

    resolver.setAuthorizationRequestCustomizer(
            OAuth2AuthorizationRequestCustomizers.withPkce());
    return resolver;
}
```

### 3.4 Mapping Keycloak claims to GrantedAuthorities

The mapper processes only claims from the ID token **already validated by Spring**, with Keycloak mappers configured
(§2.3). Roles are read from `realm_access.roles` and/or `resource_access.bio-api.roles`. It does not aggregate all
clients: `account`, `realm-management`, or a third-party client must not grant business rights.

Bean excerpt to connect to `oauth2Login(...userInfoEndpoint(...userAuthoritiesMapper(...)))`:

```java
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");

@Bean
GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {
    return source -> {
        Set<GrantedAuthority> mapped = new LinkedHashSet<>(source);
        for (GrantedAuthority authority : source) {
            if (authority instanceof OidcUserAuthority oidc) {
                Map<String, Object> claims = oidc.getIdToken().getClaims();
                addRoles(mapped, claims.get("realm_access"));
                Object resources = claims.get("resource_access");
                if (resources instanceof Map<?, ?> clients) {
                    addRoles(mapped, clients.get("bio-api"));
                }
            }
        }
        return Set.copyOf(mapped);
    };
}

private static void addRoles(Set<GrantedAuthority> target, Object roleContainer) {
    if (!(roleContainer instanceof Map<?, ?> container)
            || !(container.get("roles") instanceof Collection<?> roles)) {
        return;
    }
    for (Object value : roles) {
        if (value instanceof String role && ALLOWED_ROLES.contains(role)) {
            target.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
    }
}
```

The initial authorities `OIDC_USER` and `SCOPE_*` are preserved; the new rights are `ROLE_USER`/`ROLE_ADMIN`, consumable
by `hasRole("ADMIN")`. Missing, unknown, or malformed roles grant no business privilege. The ADMIN → USER inheritance
must be defined explicitly by policy/assignment, not assumed by the frontend.

For a Resource Server API, a separate `JwtAuthenticationConverter` applies the same allowlist to the **validated access
token**, with additional audience validation. The `GrantedAuthoritiesMapper` of `oauth2Login` does not automatically
configure Resource Servers. The persistent identity is the pair `(iss, sub)`; `preferred_username` and `email` are for
display, not automatic binding of existing accounts.

### 3.5 Session, token storage, and lifecycle

| Object                                  | Storage / rule                                                                                                            |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Browser cookie                          | Opaque cryptographically random identifier; never JWT, access token, refresh token, or autonomous encrypted secret        |
| SecurityContext / temporary OAuth state | Shared Redis Spring Session, TTL and appropriate encryption                                                               |
| ID/access/refresh tokens                | Authorized client repository server-side, linked to the session and principal, with encryption and deletion at expiration |
| Anti-CSRF token                         | Server-side in session, exposed to Angular via JSON and kept only in memory; **not an OAuth token**                       |
| Angular identity                        | Minimal DTO in memory; no serialization of `OidcUser` or `OAuth2AuthorizedClient`                                         |

An implementation of `OAuth2AuthorizedClientRepository` may rely on the session, but all instances must share and
protect the same data. Do not assume Spring automatically encrypts tokens: Redis storage, its backups, and sensitive
OIDC context attributes must be encrypted at rest; use an authenticated encryption codec or an envelope-encrypted
repository with keys outside Redis, managed and rotated. Redis traffic uses TLS and dedicated ACLs; no internet
exposure, no arbitrary Java serialization from an untrusted source. Secrets do not appear in logs, traces, or metrics.

- Idle expiration: 30 minutes; absolute duration: 8 hours since authentication. Absolute duration requires
  application-level verification of `authenticatedAt`; `timeout: 30m` alone does not enforce it. The most restrictive
  OAuth/SSO expiration takes precedence.
- Renewal only server-side via the OAuth manager: a distributed lock/versioning per client-session prevents multiple
  concurrent refreshes with a rotating token. Update access token and new refresh token atomically; delay bound and
  anti-stampede strategy required.
- `invalid_grant`, revocation, expired absolute duration, or validation failure invalidate the client and session; next
  API call → `401`. An IAM technical outage must not turn into a login loop: controlled `503` response, no bypass. Redis
  unavailable → fail closed `503`, no local fallback session.
- Reduction of roles during the session must be propagated by an explicit policy: administrative back-channel
  invalidation and re-evaluation within a bounded window. Do not retain calculated authorities indefinitely from the
  single login; a new access token refresh does not automatically remap the session authorities.
- `POST /api/auth/logout`: CSRF validation, server-side read of required revocation data, local invalidation and cookie
  deletion with the same attributes/Path and `Max-Age=0`, removal of the OAuth repository, refresh token revocation, and
  end of Keycloak session via a supported, tested server mechanism. Local invalidation remains effective even if IAM is
  unavailable; distant revocation not confirmed is audited and resumed according to a bounded policy.
- Do not use browser logout with `id_token_hint` containing the ID token: this architecture forbids exposing OIDC tokens
  to the browser. If global SSO logout is required, choose and validate a back-channel/server mechanism appropriate for
  Keycloak, with minimally privileged technical accounts, rather than assuming that OAuth revocation closes all SSO
  sessions.
- Keycloak back-channel logout → BFF: verify signature, issuer, audience, event, timestamps, and anti-replay protection
  of the `logout_token`, then invalidate sessions via index `(iss, sid)`/`(iss, sub)`. This token remains
  server-to-server. An already issued JWT access token may remain valid until expiration if the APIs do not consult a
  revocation mechanism; document this maximum window.

### 3.6 HTTP session contract

| Method and route                     | Response / behavior                                                                                                          |
|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `GET /api/auth/me`                   | `200 {"subject":"...","displayName":"...","roles":["USER"]}` or `401`; never a token, no session identifier in JSON          |
| `GET /api/auth/csrf`                 | `200 {"headerName":"X-XSRF-TOKEN","token":"..."}`; creates a session if needed, forces materialization of the deferred token |
| `POST /api/auth/logout`              | `204`, CSRF required; cookie removed, local state invalidated, revocation policy per §3.5                                    |
| `GET /oauth2/authorization/keycloak` | `302` navigation to IAM; no response containing tokens                                                                       |
| `GET /login/oauth2/code/keycloak`    | Callback validated then UI redirection or generic login error                                                                |

`/me`, `/csrf`, OAuth responses, and session errors use `Cache-Control: no-store`. The CSRF token is renewed/retrieved
after login and logout: Spring may clear it during an authentication transition. `/csrf` may create a security session
but does not perform business mutation. Mutations also verify an exact expected HTTPS origin, with explicit policy for
clients without `Origin`; no exemption based on a simple `X-Requested-With`.

Errors follow Problem Details (RFC 9457), with a stable `code`: `SESSION_REQUIRED` (`401`), `ACCESS_DENIED` (`403`),
`CSRF_INVALID` (`403`), or `DEPENDENCY_UNAVAILABLE` (`503`). Do not expose Keycloak descriptions, secrets, stack traces,
or complete claims. IAM errors after callback result in a UI error page without infinite automatic redirect.

## 4. FRONTEND SPECIFICATIONS (ANGULAR)

### 4.1 Credentials and anti-CSRF interceptor

Remove `keycloak-js`/SPA adapters if previously considered: no OAuth SDK is needed in Angular. Remove token
storage/decoding, Bearer header, OAuth renewal, and refresh token restoration at bootstrap. Remove old token keys from
browser storage during migration without erasing unrelated user preferences.

Example Angular 21 interceptor to register with `provideHttpClient(withInterceptors([bffCredentialsInterceptor, ...]))`.
`CsrfState` is an application service to implement: in-memory signal of type `string | null`, populated by
`/api/auth/csrf` **before** mutations. POST/PUT/PATCH/DELETE are blocked on the service until it loads successfully;
server-side security remains authoritative.

```typescript
import {isPlatformBrowser} from '@angular/common';
import {HttpInterceptorFn} from '@angular/common/http';
import {DOCUMENT, inject, PLATFORM_ID} from '@angular/core';
import {CsrfState} from '../services/csrf-state.service';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export const bffCredentialsInterceptor: HttpInterceptorFn = (request, next) => {
    const platformId = inject(PLATFORM_ID);
    const document = inject(DOCUMENT);
    const csrf = inject(CsrfState);
    if (!isPlatformBrowser(platformId)) {
        return next(request); // SSR: separate policy, without implicit cookie transfer.
    }

    const origin = document.location.origin;
    const target = new URL(request.url, origin);
    const isBffApi = target.origin === origin && target.pathname.startsWith('/api/');
    if (!isBffApi) {
        return next(request); // No credentials/CSRF added to Keycloak or a third party.
    }

    const token = csrf.token();
    const headers = !SAFE_METHODS.has(request.method) && token !== null
        ? request.headers.set('X-XSRF-TOKEN', token)
        : request.headers;
    return next(request.clone({withCredentials: true, headers}));
};
```

The comparison is based on the exact origin and normalized path prefix `/api/`, not `url.includes('api')`. All API calls
use this origin; never propagate a CSRF token to a third-party endpoint. The CSRF service loads the token via a GET
without creating an interceptor loop; it deduplicates concurrent loads. The CSRF DTO is neither an access token nor a
refresh token and may be read by JavaScript.

In same-origin, the browser already transmits cookies according to Fetch/XHR rules; `withCredentials: true` makes the
intent explicit. This parameter **does not bypass SameSite, Secure, or a CORS policy**. Do not register simultaneously
an old Bearer interceptor or another contradictory XSRF mechanism. The cross-origin variant is not the reference; if
approved separately, it requires exact CORS origins with credentials, never `Access-Control-Allow-Origin: *`.

### 4.2 Session state and route guards

A `SessionService` maintains a typed state `unknown | authenticated | anonymous | unavailable`, the minimal identity,
and roles from `/me`, never an OAuth token. The bootstrap calls `/me` once, deduplicates consumers, and treats an
expected `401` as anonymous without immediately initiating a login on a public route.

The functional guard for protected routes waits for this state to resolve:

1. Authenticated session: allow navigation if requested roles are present.
2. Authenticated session without role: produce a `UrlTree` to `/access-denied`, without login attempt.
3. Anonymous session: return `false` and ask the login coordinator for a browser navigation to
   `/oauth2/authorization/keycloak`; do not use `HttpClient` for this flow.
4. Dependency unavailable: unavailable page with explicit retry, without OAuth loop.

The guard is only a UX measure. Access rules are enforced in the BFF and services, including for directly typed URLs and
forged requests.

The login coordinator uses `window.location.assign('/oauth2/authorization/keycloak')` only in the browser, with a
single-flight lock for simultaneous `401`s. Any `returnUrl` is a local UI path, also validated in the BFF and stored in
session; reject absolute URLs, `//`, backslashes, equivalent encodings, and API/callback paths. The Spring request cache
must not automatically re-execute a business POST after login; return to a safe UI route.

### 4.3 Global 401/403 error handling

| Situation                                   | Angular handling                                                                                              |
|---------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `401 SESSION_REQUIRED` from a protected API | Clear in-memory session/CSRF state; a single full-page navigation to the BFF authorization endpoint           |
| `401` from `/me` during public bootstrap    | Anonymous state, no immediate redirect; guard or user action decides                                          |
| `403 ACCESS_DENIED` with valid session      | Access denied page/notification; **no re-login**                                                              |
| `403 CSRF_INVALID`                          | Reload the CSRF token and verify `/me` if necessary; no automatic repetition of a non-idempotent mutation     |
| `403` without reliable session state        | A deduplicated `/me` check; if it returns `401`, trigger the same login flow; otherwise treat the prohibition |
| `503`, network failure, or IAM unavailable  | Unavailability message and controlled retry; no automatic OAuth redirect                                      |

The decision to redirect after `401/403` is therefore applied **only when authentication is actually missing**,
including a `403` followed by `/me` confirming the absence of a session. An authenticated `403` means lack of privilege,
not lack of identity: redirecting it automatically to Keycloak would be incorrect and create a loop. The BFF should
prefer `401 SESSION_REQUIRED` for missing sessions as soon as the filter ordering allows it.

The error interceptor ignores bootstrap/CSRF/logout calls handled explicitly and third-party resources. It does not
automatically replay an import, export, or mutation after login. The UI may offer the user to retry; an API with an
idempotency key may define a separate retry policy. The callback error page and `/access-denied` remain outside any
automatic redirect.

Existing SSR sources do not justify a second Node BFF: the current image serves Angular static assets. If authenticated
SSR is enabled later, handle each request in isolation, do not share the session cache between users, do not transfer
cookies or identity via `TransferState`/HTML, and never execute `window.location` on the server. The SSR forwarding
policy must be reviewed separately; by default, render an anonymous shell and hydrate the session on the browser.

## 5. Migration, operations, and cross-cutting requirements

### 5.1 Coordinated migration and absence of insecure fallback

1. Provision realm/client/roles, secrets, certificates, DNS, dedicated database, Redis, and network policy; validate the
   Strict flow with the actual reverse proxy before opening traffic.
2. Implement the BFF and business relay allowlist; configure gateway/services as Keycloak Resource Servers with
   audience, without blind trust in `X-User-*`. Test all business routes before Nginx switch-over.
3. Migrate accounts via a controlled IAM procedure: existing BCrypt passwords only if import/federation is compatible
   and validated, otherwise reset; explicit association of the historical account to `(iss, sub)`. Never automatically
   merge two identities on a declared email address. Preserve ownership of saved filters/audits currently tied to
   username via a verified mapping table.
4. Deliver Angular session-only, remove Bearer and refresh interceptor at bootstrap, adapt guards/login/logout and
   password screens to an approved Keycloak flow. Remove old tokens from browser storage and invalidate local refresh
   tokens.
5. Disable `/api/auth/login`, `/api/auth/refresh`, their internal variants, and local password authentication at the end
   of the cutover. The local password endpoint does not remain as a parallel path. Old routes respond `404`/`410`
   according to the migration contract, without emitting tokens.
6. Separate machine identities: replace the user endpoint `service-token` with technical clients `client_credentials` at
   minimal scopes/audiences, without a browser and without reusing the interactive client `bio-bff`.
7. Publish proxy, BFF, downstream validation, and frontend configuration atomically. Temporary coexistence is only
   permitted on isolated origins/environments, without a downgrade endpoint accessible to the production browser.

Update `documentation/api-contract.md` and consumer contracts during implementation, before effective API changes; these
specification endpoints are the target, not a declaration of current availability. Writing AUTH-002 does not modify the
runtime contract or historical tickets.

### 5.2 Rollout, observability, and rollback

- Staging tests then canary with dedicated IAM identities, metrics for login, `state`/`nonce` errors, refresh,
  expirations, CSRF rejection, audience rejection, BFF latency, and Redis/IAM availability. Correlation by non-secret
  technical identifier; no high-cardinality `sub` in metric labels.
- Audit login/logout, privilege changes, revocations, and administrative actions with controlled retention and access;
  minimize personal data. Mask cookies and tokens also in APM, support HARs, exports, and proxy traces.
- HSTS in production after DNS/certificate validation, modern TLS, `X-Content-Type-Options`, CSP and `frame-ancestors`
  appropriate, rate limiting on login/callback/session startup; protect HTTP caches and avoid any public cache of
  authenticated responses.
- Backups and restoration of the IAM database tested; coordinated rotation of BFF secrets with Keycloak, JWKS key
  rotation with recovery period and validator cache refresh. Do not permanently pin a single public key.
- Rollback via immutable versions and configuration compatible with the backed-up IAM database; never restore OAuth
  storage in Angular as a fallback mechanism. If no compatible BFF version is available, close access behind a
  maintenance page rather than re-enable historical authentication. Cancellation of a Keycloak schema migration follows
  a controlled restoration, not an improvised downgrade.

## 6. Acceptance criteria and test strategy

All the following criteria are **to be verified during implementation**; none is considered executed by the mere
drafting of this document.

| ID    | Given / When / Then — expected proof                                                                                                                                                                                                                                       |
|-------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AC-01 | Given the HTTPS same-site proxy, when a user logs in without and then with an SSO/MFA session, then the code+state return to the BFF and the Strict session is recovered on supported browsers.                                                                            |
| AC-02 | Given a successful login, when DevTools/Playwright inspects storage, JS-accessible cookies, HTML, responses, and application URLs, then no access/refresh/ID token appears; only transient code/state and the opaque HttpOnly cookie are expected. Logs are also scrubbed. |
| AC-03 | Given the cookie, when its attributes are inspected, then name `__Host-BFFSESSION`, Secure, HttpOnly, SameSite=Strict, Path=/, and absence of Domain are observed; `document.cookie` does not reveal it.                                                                   |
| AC-04 | Given a pre-authenticated session, when the callback succeeds, then the session identifier changes and the old one grants no access; incorrect `state`, `nonce`, PKCE, or issuer are rejected without an authenticated session.                                            |
| AC-05 | Given a hostile origin, including a same-site subdomain, when a mutation/logout is attempted without a valid CSRF token or with a bad origin, then it fails without business effect.                                                                                       |
| AC-06 | Given absent, malformed, unknown, or foreign-client roles, when the mapper receives them, then no escalation occurs; allowed USER/ADMIN roles produce exactly the expected `ROLE_*`, and OIDC/scope authorities remain present.                                            |
| AC-07 | Given an expired session and multiple simultaneous APIs, when they fail, then only one login navigation occurs; a real authenticated `403` leads to access denied without a loop.                                                                                          |
| AC-08 | Given two BFF instances and a rotating refresh token, when concurrent calls renew access, then only one effective rotation is persisted; no token is returned to the browser.                                                                                              |
| AC-09 | Given logout/revocation/back-channel or expired absolute duration, when the session is reused on another instance, then it is refused; the server tokens are removed and the residual JWT downstream window is documented.                                                 |
| AC-10 | Given an internal API, when it receives a bad audience, expired JWT, ID token, or only forged `X-User-*`, then access is denied; no public path bypasses the BFF.                                                                                                          |
| AC-11 | Given the BFF relay, when large imports/exports and cancellation are tested, then streaming, limits, and authorizations work without full buffering or transmission of cookies/tokens to the client. An arbitrary target URL does not trigger SSRF.                        |
| AC-12 | Given Redis/IAM failure, when a session is resolved or renewed, then no insecure fallback is activated and the UI distinguishes unavailability from login requirement.                                                                                                     |
| AC-13 | Given the release Compose, when network configuration and restoration are tested, then the database/management are not public, the SQL account is not superuser, and data persists after recreation without loss.                                                          |
| AC-14 | Given the migration, when the old endpoints/token storage and direct gateway routing are searched/tested, then no browser path can still obtain or use local OAuth tokens.                                                                                                 |

Required unit tests: role mapping and malformed claims, `returnUrl`/origin validation, idle/absolute policy, refresh
locking, relay allowlist, header filtering, `SessionService`, guards, and Angular interceptors. Verify that credentials
and CSRF are never added to a third-party URL. Integration tests: Spring Security/MockMvc, real Keycloak and Redis in an
isolated environment, JWT/audience validation in services; Playwright browser tests for real cookies and redirects,
impossible to prove with mocks alone.

Coverage for new backend/frontend components must be measured and documented; **less than 80% blocks closure**, per the
constitution. Targeted pentests cover XSS, CSRF, fixation, login CSRF, open redirect, SSRF, proxy header spoofing,
replay, and privilege removal. Results, image versions, CVE reports, and restoration evidence are recorded in the
journal before approval.

## 7. References and related documents

- [Implementation plan](plan.md), [journal](journal.md), [catalogue](../README.md).
- [Constitution](../../constitution.md), [current API contract](../../api-contract.md), [AUTH-001](../AUTH-001/overview.md), [ARCH-001](../ARCH-001/analyse.md), [ARCH-002](../ARCH-002/plan.md).
- [OAuth 2.0 — RFC 6749](https://www.rfc-editor.org/rfc/rfc6749), [PKCE — RFC 7636](https://www.rfc-editor.org/rfc/rfc7636), [OAuth 2.0 Security Best Current Practice — RFC 9700](https://www.rfc-editor.org/rfc/rfc9700).
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0.html), [OIDC Back-Channel Logout](https://openid.net/specs/openid-connect-backchannel-1_0.html), [OAuth Token Revocation — RFC 7009](https://www.rfc-editor.org/rfc/rfc7009), [Problem Details — RFC 9457](https://www.rfc-editor.org/rfc/rfc9457).
- [Keycloak 26.8.1 — verified release](https://github.com/keycloak/keycloak/releases/tag/26.8.1), [containers](https://www.keycloak.org/server/containers), [reverse proxy](https://www.keycloak.org/server/reverseproxy), [bootstrap and admin recovery](https://www.keycloak.org/server/bootstrap-admin-recovery), [health checks](https://www.keycloak.org/observability/health).
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html), [OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html), [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [Spring Session](https://docs.spring.io/spring-session/reference/).
- [Angular — interceptors](https://angular.dev/guide/http/interceptors), [Angular security](https://angular.dev/best-practices/security), [OWASP Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), [OWASP CSRF Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).
