# NLQ-001 — Ambiguities & Analysis

## Status: Resolved — implementation can proceed

---

## Resolved Decisions

### 1. Spring AI vs Direct REST Client for Gemini

**Decision:** Use Spring AI with a raw `RestClient` fallback for Gemini.

**Rationale:**

| Approach            | Pros                                                                                                 | Cons                                                        |
|---------------------|------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| **Spring AI**       | Unified `ChatClient` API; prompt templating; output parsers; provider switch via config              | Gemini starter may lag behind API changes; extra dependency |
| **Direct REST**     | Full control over Gemini request/response; no abstraction overhead                                   | Boilerplate for each provider; no prompt templating         |
| **Hybrid (chosen)** | Spring AI interface for portability; `GeminiLlmProvider` can use `RestClient` if starter is unstable | Slightly more code than pure Spring AI                      |

- The `LlmProvider` interface is the contract. `GeminiLlmProvider` is free to use Spring AI's `GoogleAiGeminiChatModel`
  OR a hand-rolled `RestClient`.
- If the Spring AI Gemini starter is unavailable or incompatible at implementation time, the `RestClient` fallback
  ensures zero architectural disruption.
- OpenAI and Ollama providers can use Spring AI starters directly (mature support).

---

### 2. Gemini Model Selection: Flash vs Pro

**Decision:** Default to `gemini-1.5-flash` (free tier).

**Rationale:**

| Model                | Speed     | Cost               | Context Window | Structured Output | Suitability                                              |
|----------------------|-----------|--------------------|----------------|-------------------|----------------------------------------------------------|
| **gemini-1.5-flash** | Very fast | Free tier (60 QPM) | 1M tokens      | Excellent         | ✅ Chosen — sufficient for JSON translation              |
| **gemini-1.5-pro**   | Fast      | Paid               | 2M tokens      | Excellent         | Overkill for filter translation; use only if Flash fails |
| **gemini-1.0-pro**   | Moderate  | Cheaper paid       | 32K            | Good              | Deprecated; avoid                                        |

- Flash is optimized for high-volume, low-latency tasks. Filter translation is a low-complexity structured-output task.
- Pro can be configured as an override (`app.nlq.gemini.model=gemini-1.5-pro`) for deployments with paid keys.

---

### 3. JSON Output Reliability: Function Calling vs Raw JSON

**Decision:** Use raw JSON with strict system prompt + `BeanOutputParser` fallback; defer function calling to v1.1.

**Rationale:**

- Gemini supports "function calling" (tool use) which guarantees schema-valid output.
- However, Spring AI's function calling abstraction adds complexity and may not map cleanly to our existing
  `GeneSearchRequest` POJO.
- **v1 approach:** System prompt includes the full JSON schema and demands raw JSON. `NlqResponseParser` handles
  markdown fences and fuzzy repair.
- **v1.1 enhancement:** If raw JSON reliability is < 95 % in production, migrate to Gemini's
  `responseMimeType: application/json` + `responseSchema` (Google's native JSON mode) or Spring AI function calling.
- The prompt will explicitly state: `Return ONLY a JSON object. No markdown. No explanations.`

---

### 4. Conversation State: Backend vs Frontend

**Decision:** Stateless backend; conversation history stored in frontend signals.

**Rationale:**

| Approach                    | Pros                                                              | Cons                                                              |
|-----------------------------|-------------------------------------------------------------------|-------------------------------------------------------------------|
| **Frontend state (chosen)** | Zero backend storage; GDPR-simple; users clear history by refresh | History lost on page reload; limited to current session           |
| **Backend session**         | Survives page reload; shared across tabs                          | Requires session/DB storage; privacy concerns; scaling complexity |
| **Backend DB persistence**  | Full audit trail; cross-device                                    | Overkill for MVP; GDPR complexity; storage cost                   |

- The `NlqChatRequest` includes `messages[]` which the frontend maintains.
- Backend treats each request independently.
- If v1.1 requires persistence, a simple `chat_session` table can be added without breaking the API contract.

---

### 5. Summarization Context Window Management

**Decision:** Cap at 100 proteins; truncate metadata per protein to essential fields.

**Rationale:**

- 100 proteins × ~500 tokens of metadata = ~50K tokens. Well within Gemini Flash's 1M context window.
- If users select > 100, return HTTP 413 with guidance to refine selection.
- Metadata included in summarize prompt per protein:
    - accession, proteinFullName, organismName, length, keywords (first 5), evidenceLevel, reviewed
- Fields omitted to save tokens: sequence, features, goTerms, comments, publications, crossReferences.

---

### 6. Provider Switching Mechanism

**Decision:** Spring `@ConditionalOnProperty` with a resolver bean, not a runtime dispatcher.

**Rationale:**

- The existing `GeneServiceDispatcher` resolves providers **per request** via HTTP header (`X-Data-Provider`).
- LLM providers are infrastructure concerns, not business-logic data sources. Switching them per-request is unnecessary.
- **Chosen:** Single provider active per deployment, selected via `app.nlq.provider` in `application.yml`. Spring Boot
  creates only the matching bean.
- If future requirements demand per-request LLM switching (e.g., user preference), a `LlmProviderDispatcher` can be
  added later without breaking the interface.

---

### 7. Prompt Injection Defense Depth

**Decision:** Three-layer defense.

**Layer 1 — Input Sanitization:**

- Strip HTML, truncate length, normalize Unicode.

**Layer 2 — Prompt Injection Guard:**

- Regex blocklist for known jailbreak patterns.
- Reject inputs with excessive newlines or uppercase ratio.

**Layer 3 — Architecture Safety:**

- LLM output is NEVER executed as code, SQL, or shell commands.
- LLM output is parsed into a DTO and validated by existing Jakarta Bean Validation.
- The worst-case failure mode is an invalid `GeneSearchRequest` → HTTP 422.

**No "system prompt leakage" risk:** The system prompt contains only schema definitions and examples — no secrets, no
credentials, no internal architecture details.

---

## Open Questions (non-blocking)

| Question                                                          | Owner   | Priority | Resolution Path                                                                                        |
|-------------------------------------------------------------------|---------|----------|--------------------------------------------------------------------------------------------------------|
| Should we support voice input (Web Speech API) in the search bar? | UX      | Low      | Defer to v1.1; microphone icon can be added as a no-op button for now                                  |
| Should the chat assistant suggest follow-up questions?            | Product | Low      | Add `suggestedFollowUps: string[]` to `NlqChatResponse` in v1.1                                        |
| Do we need i18n for NLQ prompts (non-English queries)?            | Product | Low      | Gemini supports multilingual input out-of-the-box; system prompt remains English; evaluate post-launch |
| Should NLQ queries be saved to `saved_filter` table?              | Product | Low      | Defer; if requested, add `source: 'NLQ'` enum to saved filters                                         |

---

**Last Updated:** 2026-08-12
