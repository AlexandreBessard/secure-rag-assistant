# rag-backend

## Running

To run, ensure AWS credentials are in the environment:

```bash
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=us-east-1   # optional, defaults to us-east-1
cd backend && ./mvnw spring-boot:run
```

## Prompt Guard Pipeline

Every `/ask` request passes through a four-layer guard before reaching the LLM. The first three layers run before any Bedrock call is made; the fourth wraps the LLM call itself.

| Layer | Class | Mechanism | Cost |
|---|---|---|---|
| 1 | `PromptGuardService` | Regex patterns — detects injection/jailbreak phrases (`ignore your instructions`, `jailbreak`, `DAN`, etc.) | Zero |
| 2 | `PromptGuardService` | Keyword blocklist — configurable list of custom terms (`app.guard.blocked-terms`) | Zero |
| 3 | `ComprehendModerationService` | Amazon Comprehend `DetectToxicContent` — ML-based detection of hate speech, harassment, insult, profanity, sexual content, violence | AWS API call |
| 4 | `CanaryWordAdvisor` | Injects a secret UUID into the system prompt; if that UUID appears in the LLM response the model leaked the system prompt and the response is replaced with a denial | Included in the Bedrock call |

Layers 1 and 2 are zero-cost and short-circuit immediately on a match. Layer 3 is only reached if both pass. Layer 4 operates on the output side and cannot be bypassed by any input manipulation.

Blocked requests return HTTP 400 with a JSON body describing the reason:

```json
{ "error": "PROMPT_BLOCKED", "message": "Prompt injection detected" }
{ "error": "PROMPT_BLOCKED", "message": "Message contains inappropriate content: HATE_SPEECH" }
```

### Configuration

```properties
# Comprehend region — DetectToxicContent is NOT available in eu-west-1.
# Supported regions: us-east-1, us-west-2, eu-central-1, ap-southeast-1, ap-southeast-2
app.comprehend.region=eu-central-1   # closest EU region that supports the API

# Maximum prompt length in characters
app.guard.max-length=2000

# Comma-separated custom terms to block (competitor names, project codenames, etc.)
app.guard.blocked-terms=

# Amazon Comprehend toxicity score threshold (0.0–1.0) — requests scoring above this are rejected
app.guard.toxicity-threshold=0.7
```

Comprehend uses the same AWS credentials and region (`app.s3.region`) as the rest of the application — no additional credentials are required. If Comprehend is temporarily unavailable, a warning is logged and the request is allowed through so a Comprehend outage does not take down the chat.
