# NLQ-001 Implementation Plan

## Tasks

1. Analyze requirements and update plan
2. Resolve ambiguities (Spring AI version, Gemini SDK, structured output reliability)
3. Add Spring AI dependencies to `pom.xml`
4. Implement `LlmProvider` interface and provider resolver
5. Implement `GeminiLlmProvider` (Spring AI Google Gemini)
6. Implement `OpenAiLlmProvider` and `OllamaLlmProvider` (stubs acceptable for v1)
7. Implement `NlqPromptBuilder` with JSON schema context + few-shot examples
8. Implement `NlqResponseParser` with fuzzy JSON repair
9. Implement `NlqInputSanitizer` and `PromptInjectionGuard`
10. Implement `NlqService` (translate, chat, summarize)
11. Implement `NlqController` with validation
12. Add usage telemetry / audit hooks
13. Implement Angular models (`nlq.model.ts`)
14. Implement `NlqService` (frontend)
15. Implement `NlqSearchBarComponent`
16. Implement `NlqChatComponent`
17. Wire NLQ output to `filtersStore` and Gene Explorer table
18. Write backend unit tests
19. Write frontend unit tests
20. Write integration tests
21. Update documentation and journal

## Status

- [x] Requirements analyzed
- [x] Ambiguities resolved (see analyse.md)
- [ ] Dependencies added to pom.xml
- [ ] LlmProvider interface implemented
- [ ] GeminiLlmProvider implemented
- [ ] OpenAiLlmProvider / OllamaLlmProvider stubs
- [ ] NlqPromptBuilder implemented
- [ ] NlqResponseParser implemented
- [ ] NlqInputSanitizer + PromptInjectionGuard implemented
- [ ] NlqService implemented
- [ ] NlqController implemented
- [ ] Audit hooks wired
- [ ] Angular models defined
- [ ] Frontend NlqService implemented
- [ ] NlqSearchBarComponent implemented
- [ ] NlqChatComponent implemented
- [ ] Store wiring complete
- [ ] Backend unit tests written
- [ ] Frontend unit tests written
- [ ] Integration tests written
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Coverage ≥ 80 %

---

## Detailed Checklist

### Backend — Dependencies (`pom.xml`)

- [ ] `spring-ai-core` — Spring AI abstractions (`ChatClient`, `Prompt`, `ChatResponse`)
- [ ] `spring-ai-google-ai-gemini` — Google AI Studio Gemini integration (free tier)
- [ ] `spring-ai-openai` — OpenAI provider (optional, compile scope or profile)
- [ ] `spring-ai-ollama` — Local Ollama provider (optional, compile scope or profile)
- [ ] `json-schema-validator` — Validates LLM output against GeneSearchRequest JSON schema

### Backend — Configuration (`application.yml`)

- [ ] `app.nlq.provider` — enum: `gemini`, `openai`, `ollama`
- [ ] `app.nlq.timeout-seconds` — default 15
- [ ] `app.nlq.max-input-length` — default 500
- [ ] `app.nlq.retry.max-attempts` — default 3
- [ ] `app.nlq.retry.backoff-ms` — default 1000
- [ ] `app.nlq.gemini.api-key` — from env var `GEMINI_API_KEY`
- [ ] `app.nlq.gemini.model` — `gemini-1.5-flash` (free tier, fast, good for structured output)
- [ ] `app.nlq.gemini.temperature` — 0.1 (low creativity for deterministic JSON)
- [ ] `app.nlq.openai.api-key` — from env var `OPENAI_API_KEY`
- [ ] `app.nlq.openai.model` — `gpt-4o-mini`
- [ ] `app.nlq.ollama.base-url` — `http://localhost:11434`
- [ ] `app.nlq.ollama.model` — `llama3.1`

### Backend — Provider Abstraction

- [ ] `LlmProvider` interface (`infrastructure/llm/`):
    - [ ] `chat(List<LlmMessage> messages): String` — raw text response
    - [ ] `getProviderName(): String` — for telemetry
- [ ] `LlmMessage` record — `{ role: String (system/user/assistant), content: String }`
- [ ] `LlmProviderResolver` — `@Component` that resolves the active bean by `app.nlq.provider` name
- [ ] Conditional beans: `@ConditionalOnProperty(name = "app.nlq.provider", havingValue = "gemini")` on
  `GeminiLlmProvider`

### Backend — Provider Implementations

- [ ] `GeminiLlmProvider` (`infrastructure/llm/gemini/`):
    - [ ] Injects `GoogleAiGeminiChatModel` (Spring AI) or builds `RestClient` calls to
      `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
    - [ ] Maps `LlmMessage` list to Gemini `contents` format
    - [ ] Extracts `candidates[0].content.parts[0].text` from response
    - [ ] Throws `LlmProviderException` on API errors (429, 5xx)
- [ ] `OpenAiLlmProvider` (`infrastructure/llm/openai/`):
    - [ ] Injects `OpenAiChatModel` (Spring AI)
    - [ ] Stub implementation acceptable for v1 if API key absent
- [ ] `OllamaLlmProvider` (`infrastructure/llm/ollama/`):
    - [ ] Injects `OllamaChatModel` (Spring AI)
    - [ ] Stub implementation acceptable for v1

### Backend — Prompt Engineering

- [ ] `NlqPromptBuilder` (`service/nlq/`):
    - [ ] `buildTranslationPrompt(String userQuery, GeneSearchRequest previousContext): String`
    - [ ] System prompt includes:
        - [ ] Role definition: "You are a bioinformatics query translator..."
        - [ ] JSON schema of `GeneSearchRequest` (all fields, types, constraints)
        - [ ] Few-shot examples (≥ 5 pairs covering organism, keywords, length, evidence, reviewed, GO terms)
        - [ ] Instruction: "Return ONLY a JSON object. No markdown fences. No explanations."
    - [ ] If `previousContext` is non-null, append: "Previous filters: {...}. Apply the new instruction as a delta."
- [ ] `buildSummarizationPrompt(List<ProteinSummaryDto> proteins): String`
    - [ ] System prompt: "Summarize this protein set for a biologist..."
    - [ ] Includes organism counts, keyword frequencies, length stats, evidence distribution
    - [ ] Instruction: "Return markdown. Use bullet points. Highlight unexpected findings."

### Backend — Response Parsing & Validation

- [ ] `NlqResponseParser` (`service/nlq/`):
    - [ ] `parseTranslation(String raw): GeneSearchRequest`
        - [ ] Strip markdown fences (```json ... ```)
        - [ ] Attempt `ObjectMapper.readValue(raw, GeneSearchRequest.class)`
        - [ ] On `UnrecognizedPropertyException`: log warning, ignore unknown fields (forward compatibility)
        - [ ] On failure: throw `NlqParseException` with raw response preserved for debugging
    - [ ] `parseChatReply(String raw): String` — returns markdown text directly
- [ ] `NlqInputSanitizer` (`service/nlq/`):
    - [ ] `sanitize(String input): String`
        - [ ] Strip HTML tags (Jsoup or regex)
        - [ ] Trim whitespace
        - [ ] Truncate to `app.nlq.max-input-length`
        - [ ] Normalize Unicode (NFKC)
- [ ] `PromptInjectionGuard` (`service/nlq/`):
    - [ ] `isSafe(String input): boolean`
        - [ ] Blocklist regex patterns: `ignore previous`, `system prompt`, `DAN`, `jailbreak`, `\[\[`, `\{\{`
        - [ ] Reject if input contains `

` (attempt to inject system prompt separator)
- [ ] Reject if ratio of uppercase letters > 70 % (shouting / injection attempts)

- [ ] Throws `PromptInjectionDetectedException` → mapped to HTTP 400

### Backend — Service

- [ ] `NlqService` (`service/NlqService.java`):
    - [ ] `translate(NlqTranslateRequest request): NlqTranslateResponse`
        - [ ] Sanitize input → guard check → build prompt → call `LlmProvider` → parse JSON → validate with Bean
          Validation → return
        - [ ] `confidence` score: derived from parser success (1.0) vs fuzzy repair (0.7) vs fallback (0.5)
    - [ ] `chat(NlqChatRequest request): NlqChatResponse`
        - [ ] Build conversation prompt from `messages[]` + new user query
        - [ ] LLM returns `NlqChatResponse` containing: `reply` (markdown), `suggestedFilters` (optional
          GeneSearchRequest), `results` (optional if execute=true)
        - [ ] If `execute=true` in request, call `GeneService.search()` with suggested filters and embed `PagedResponse`
          in response
    - [ ] `summarize(NlqSummarizeRequest request): NlqSummarizeResponse`
        - [ ] Fetch `ProteinSummaryDto` list by accessions via `GeneService`
        - [ ] Build summarization prompt with protein metadata
        - [ ] LLM returns markdown summary + `keyFindings[]` (extracted via regex) + `goEnrichment[]` (extracted via
          regex)
    - [ ] `@Async` audit logging via `AuditService` (reuses OPS-001 infrastructure)

### Backend — Controller

- [ ] `NlqController` (`controller/NlqController.java`):
    - [ ] `POST /api/nlq/translate` → `200 OK` with `NlqTranslateResponse`
        - [ ] Request body: `@Valid NlqTranslateRequest`
        - [ ] Returns `{ geneSearchRequest: {...}, explanation: "...", confidence: 0.95 }`
    - [ ] `POST /api/nlq/chat` → `200 OK` with `NlqChatResponse`
        - [ ] Request body: `@Valid NlqChatRequest`
        - [ ] Supports `execute: boolean` flag to auto-run search
    - [ ] `POST /api/nlq/summarize` → `200 OK` with `NlqSummarizeResponse`
        - [ ] Request body: `@Valid NlqSummarizeRequest` (max 100 accessions)
        - [ ] Returns `{ summary: "...", keyFindings: [...], goEnrichment: [...] }`
    - [ ] Error responses:
        - [ ] `400` — validation failure, prompt injection detected, parse failure
        - [ ] `401` — missing JWT
        - [ ] `429` — LLM rate limit (from provider) or Bucket4j rate limit
        - [ ] `503` — LLM provider unreachable after retries
        - [ ] `413` — summarize request exceeds 100 accessions
    - [ ] Thin controller — delegates all logic to `NlqService`

### Backend — DTOs

- [ ] `NlqTranslateRequest` (`dto/nlq/`):
    - [ ] `@NotBlank @Size(max=500) String query`
    - [ ] `GeneSearchRequest previousContext` — optional prior filter state
- [ ] `NlqTranslateResponse`:
    - [ ] `GeneSearchRequest geneSearchRequest`
    - [ ] `String explanation` — human-readable description of applied filters
    - [ ] `double confidence` — 0.0 to 1.0
- [ ] `NlqChatRequest`:
    - [ ] `@NotEmpty List<@Valid NlqMessage> messages`
    - [ ] `@NotBlank String currentQuery`
    - [ ] `boolean execute` — whether to run search immediately
- [ ] `NlqChatResponse`:
    - [ ] `String reply` — assistant markdown message
    - [ ] `GeneSearchRequest suggestedFilters` — optional parsed filters
    - [ ] `PagedResponse<ProteinSummaryDto> results` — optional executed results
- [ ] `NlqSummarizeRequest`:
    - [ ] 
      `@NotEmpty @Size(max=100) List<@Pattern(regexp="[A-NR-Z][0-9][A-Z][A-Z0-9]{2}[0-9]|[OPQ][0-9][A-Z0-9]{3}[0-9]") String> accessions`
    - [ ] `String focus` — optional focus area (e.g., "disease relevance", "evolution")
- [ ] `NlqSummarizeResponse`:
    - [ ] `String summary` — markdown narrative
    - [ ] `List<String> keyFindings` — bullet points
    - [ ] `List<String> goEnrichment` — enriched GO terms

### Backend — Exception Handling

- [ ] `NlqParseException` → `422 Unprocessable Entity`
- [ ] `PromptInjectionDetectedException` → `400 Bad Request`
- [ ] `LlmProviderException` → `503 Service Unavailable` (with `Retry-After` if known)
- [ ] `LlmRateLimitException` → `429 Too Many Requests`
- [ ] All mapped in `GlobalExceptionHandler`

### Backend — Rate Limiting (Bucket4j)

- [ ] Separate filter for `/api/nlq/**`:
  ```yaml
  bucket4j:
    filters:
      - cache-name: nlq-rate-limit-cache
        url: /api/nlq/.*
        http-response-body: '{"status":429,"error":"Too Many Requests","message":"NLQ rate limit exceeded. Retry after {retry-after} seconds."}'
        rate-limits:
          - cache-key: getRemoteAddr()
            bandwidths:
              - capacity: 10
                time: 1
                unit: minutes
  ```
- [ ] Rationale: 10 req/min protects the free Gemini tier (60/min) while allowing legitimate use.

### Frontend — Models (`core/models/nlq.model.ts`)

- [ ] `NlqTranslateRequest` — `{ query: string; previousContext?: GeneSearchRequest }`
- [ ] `NlqTranslateResponse` — `{ geneSearchRequest: GeneSearchRequest; explanation: string; confidence: number }`
- [ ] `NlqMessage` — `{ role: 'user' | 'assistant' | 'system'; content: string }`
- [ ] `NlqChatRequest` — `{ messages: NlqMessage[]; currentQuery: string; execute?: boolean }`
- [ ] `NlqChatResponse` —
  `{ reply: string; suggestedFilters?: GeneSearchRequest; results?: PagedResponse<ProteinSummary> }`
- [ ] `NlqSummarizeRequest` — `{ accessions: string[]; focus?: string }`
- [ ] `NlqSummarizeResponse` — `{ summary: string; keyFindings: string[]; goEnrichment: string[] }`

### Frontend — Service (`features/nlq/nlq.service.ts`)

- [ ] `translate(query: string, previousContext?: GeneSearchRequest): Observable<NlqTranslateResponse>`
- [ ] `chat(request: NlqChatRequest): Observable<NlqChatResponse>`
- [ ] `summarize(accessions: string[], focus?: string): Observable<NlqSummarizeResponse>`
- [ ] `conversationHistory = signal<NlqMessage[]>([])` — local state, not persisted

### Frontend — `NlqSearchBarComponent` (`features/nlq/nlq-search-bar/`)

- [ ] `nlq-search-bar.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `nlq-search-bar.component.html` — external template
- [ ] `nlq-search-bar.component.scss` — design system tokens
- [ ] Layout: Inline search input with microphone icon (future) + send button
- [ ] Input:
    - [ ] `placeholder="Ask in plain English: e.g., 'Human kinases under 400aa'"`
    - [ ] `maxlength="500"`
    - [ ] Debounce 300 ms on input (for character count only); submit on Enter or click
- [ ] States:
    - [ ] Idle → input ready
    - [ ] Loading → spinner inside input, disabled state
    - [ ] Success → brief green checkmark, then clear input
    - [ ] Error → red border + inline error message
- [ ] Output: `filterChange = output<GeneSearchRequest>()` — emits parsed request to parent
- [ ] Accessibility:
    - [ ] `aria-label="Natural language protein search"`
    - [ ] `aria-describedby="nlq-hint"` linking to hint text

### Frontend — `NlqChatComponent` (`features/nlq/nlq-chat/`)

- [ ] `nlq-chat.component.ts` — `ChangeDetectionStrategy.OnPush`, standalone
- [ ] `nlq-chat.component.html` — external template
- [ ] `nlq-chat.component.scss` — design system tokens; max-height 600px; scrollable message list
- [ ] Layout:
  ```
  ┌─ Chat Header ─────────────────────────────┐
  │  🤖 Protein AI Assistant          [✕]     │
  ├─ Messages (scrollable) ───────────────────┤
  │  [User] "Human kinases under 400aa"       │
  │  [Assistant] "Showing reviewed human..."  │
  │  [Result Preview] 42 rows · [Apply]       │
  ├─ Input Area ──────────────────────────────┤
  │  [Type a message...] [Send]               │
  └───────────────────────────────────────────┘
  ```
- [ ] Signals:
    - [ ] `messages = signal<NlqMessage[]>([])`
    - [ ] `loading = signal<boolean>(false)`
    - [ ] `error = signal<string | null>(null)`
- [ ] Behavior:
    - [ ] On send: append user message, set loading, call `NlqService.chat()`, append assistant reply
    - [ ] If response contains `suggestedFilters`, show "Apply Filters" chip button
    - [ ] Clicking "Apply Filters" emits `filtersApplied = output<GeneSearchRequest>()`
    - [ ] If response contains `results`, show mini table preview (first 3 rows)
- [ ] Accessibility:
    - [ ] `aria-live="polite"` on message list container
    - [ ] Each message has `role="listitem"` inside `role="log"`
    - [ ] Focus returns to input after assistant reply

### Frontend — Integration with Gene Explorer

- [ ] Add `NlqSearchBarComponent` to `GenesPageComponent` header (next to global search)
- [ ] Add floating action button (FAB) to open `NlqChatComponent` as a slide-out drawer
- [ ] Wire `filterChange` from NLQ components to `filtersStore.setFilter()` — triggers existing table reload
- [ ] Add "Summarize Selection" context menu item in `GenesTableComponent` (when rows selected)
- [ ] Summarize result shown in a `MatBottomSheet` or inline panel

### Tests — Backend

- [ ] `NlqPromptBuilderTest`:
    - [ ] `buildTranslationPrompt_includesSchema`
    - [ ] `buildTranslationPrompt_includesFewShots`
    - [ ] `buildTranslationPrompt_appendsPreviousContext`
- [ ] `NlqResponseParserTest`:
    - [ ] `parseTranslation_validJson_returnsRequest`
    - [ ] `parseTranslation_withMarkdownFences_stripsThem`
    - [ ] `parseTranslation_malformedJson_throws`
    - [ ] `parseTranslation_unknownFields_ignoresThem`
- [ ] `NlqInputSanitizerTest`:
    - [ ] `sanitize_stripsHtml`
    - [ ] `sanitize_truncatesToMaxLength`
- [ ] `PromptInjectionGuardTest`:
    - [ ] `isSafe_allowsNormalQuery`
    - [ ] `isSafe_rejectsJailbreak`
    - [ ] `isSafe_rejectsHtmlScript`
- [ ] `NlqServiceTest` (mock LlmProvider + mock GeneService):
    - [ ] `translate_validQuery_returnsGeneSearchRequest`
    - [ ] `translate_promptInjection_throws`
    - [ ] `translate_llmFailure_retriesThenThrows`
    - [ ] `chat_withExecute_returnsResults`
    - [ ] `summarize_validAccessions_returnsSummary`
    - [ ] `summarize_tooManyAccessions_throws`
- [ ] `GeminiLlmProviderTest` (`MockRestServiceServer` or Spring AI test stubs):
    - [ ] `chat_returnsTextFromGeminiResponse`
    - [ ] `chat_apiError_throwsLlmProviderException`
- [ ] `NlqControllerIntegrationTest`:
    - [ ] `POST /api/nlq/translate` → `200` with valid request
    - [ ] `POST /api/nlq/translate` injection → `400`
    - [ ] `POST /api/nlq/chat` → `200` with reply
    - [ ] `POST /api/nlq/summarize` > 100 accessions → `413`
    - [ ] `POST /api/nlq/translate` without JWT → `401`

### Tests — Frontend

- [ ] `NlqSearchBarComponent` unit tests:
    - [ ] Emits `filterChange` on successful translation
    - [ ] Shows loading state during API call
    - [ ] Shows error message on 422
    - [ ] Trims input before sending
- [ ] `NlqChatComponent` unit tests:
    - [ ] Appends user message on send
    - [ ] Appends assistant message on response
    - [ ] Shows "Apply Filters" chip when suggestedFilters present
    - [ ] Emits `filtersApplied` on chip click
    - [ ] Shows error state on API failure
- [ ] `NlqService` unit tests (HttpClientTestingModule):
    - [ ] `translate` sends POST to `/api/nlq/translate`
    - [ ] `chat` sends POST to `/api/nlq/chat`
    - [ ] `summarize` sends POST to `/api/nlq/summarize`

### General

- [ ] No `ngClass` / `ngStyle` — `class` / `style` bindings only
- [ ] Native control flow (`@if`, `@for`)
- [ ] `ChangeDetectionStrategy.OnPush` on all new components
- [ ] AXE checks pass (chat log has `role="log"`, input has label, focus management)
- [ ] Code reviewed
- [ ] Coverage ≥ 80 % (JaCoCo + Jest)

---

## Risk Register

| ID | Risk                                                                        | Probability | Mitigation                                                                                                                        |
|----|-----------------------------------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------------------|
| R1 | Gemini free tier rate limit (60/min) hit during demo                        | Medium      | Bucket4j 10 req/min per IP; graceful 503 with retry guidance; operator can switch to paid tier or Ollama                          |
| R2 | LLM hallucinates invalid filter values (e.g., organism="Mars")              | Medium      | Bean Validation on parsed `GeneSearchRequest`; invalid values return 422 with explanation; prompt explicitly lists valid enums    |
| R3 | Spring AI Gemini starter not yet stable / incompatible with Spring Boot 3.x | Low         | Fallback: implement `GeminiLlmProvider` as raw `RestClient` calling Gemini REST API directly; Spring AI abstraction still applies |
| R4 | Prompt injection bypasses guard                                             | Low         | Defense in depth: guard + input sanitization + LLM never executes raw user text as code + no SQL generation                       |
| R5 | Token cost explosion on summarize with large protein metadata               | Medium      | Cap at 100 accessions; truncate protein names to 100 chars each in prompt; monitor via audit telemetry                            |
| R6 | Conversation history exceeds LLM context window                             | Medium      | Send only last 6 messages; summarize older context if needed (v1.1)                                                               |

---

## Commands

```bash
# Run backend tests
cd backend
./mvnw -Dtest=com.bioinformatics.dashboard.nlq.*Test test

# Run frontend tests
cd frontend
ng test --include='**/nlq-*/*'

# Verify Gemini connectivity (requires GEMINI_API_KEY env var)
curl -X POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY \
  -H 'Content-Type: application/json' \
  -d '{"contents":[{"parts":[{"text":"Say hello"}]}]}'
```
