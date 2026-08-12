# NLQ-001 — Implementation Journal

## 2026-08-12 — Ticket Created & Requirements Analyzed

**Action:** Created `NLQ-001` implementation folder and drafted specification.  
**Outcome:**

- Evaluated Spring AI vs direct REST client integration for LLM providers.
- Selected Spring AI for standardized `ChatClient` abstraction and provider portability.
- Selected Google Gemini 1.5 Flash (free tier) as default provider due to generous quota (60 QPM, 1M TPM) and strong
  structured JSON output adherence.
- Defined pluggable `LlmProvider` interface inspired by existing `REFACTOR-001` provider dispatcher pattern.
- Reviewed OWASP LLM Top 10; designed defense-in-depth strategy (sanitization + guard + schema validation + no SQL
  generation).
- Confirmed `GeneSearchRequest` schema is sufficient for LLM output target; no new DB migrations required.
- Identified reuse opportunities: `AuditService` (OPS-001) for telemetry, `GlobalExceptionHandler` for error mapping,
  `Bucket4j` for rate limiting.

**Next Step:** Add Spring AI dependencies and implement `LlmProvider` interface once ticket is prioritized.

---

**Coverage Target:** ≥ 80 % (pending)
