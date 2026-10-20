# openSpec — Backend API Contract (Frontend Handoff)

## 1. Overview & Objective

This repository contains the backend foundation and REST API contract for **openSpec**, an AI-powered automated API testing and validation suite.

**Contract-Only Scope**:
In this phase, only the backend foundations, data models, Bean Validation constraints, enums, REST controllers, and OpenAPI / Swagger documentation are implemented. No business logic (external LLM calls, background test execution engines, database persistence, or live SSE event dispatching) is executed. All endpoints return clearly documented contract stubs and schemas ready for immediate frontend UI development and integration.

---

## 2. Technology Stack & Prerequisites

- **Java**: OpenJDK 17 LTS (Java 17 target explicitly configured; no Java 21+ features used)
- **Framework**: Spring Boot 3.3.x (Spring Web, Spring Validation)
- **API Documentation**: Springdoc OpenAPI 2.5.0 (Swagger UI v3)
- **Serialization**: Jackson JSON & Java Time (`Instant` ISO-8601 timestamps)
- **Boilerplate**: Project Lombok
- **Containerization**: Eclipse Temurin 17 JRE Docker image
- **Default Server Port**: `8081`

---

## 3. Quick Start & Execution

### Option A: Run via Maven Wrapper
```bash
# On Linux/macOS
./mvnw clean spring-boot:run

# On Windows PowerShell
.\mvnw.cmd clean spring-boot:run
```

### Option B: Build and Run the Packaged JAR
```bash
mvn clean package
java -jar target/backend-api-0.1.0-SNAPSHOT.jar
```

### Option C: Docker Containerization
```bash
# 1. Package the application
mvn clean package -DskipTests

# 2. Build the Docker image
docker build -t openspec-backend:0.1.0 .

# 3. Run the container on port 8081
docker run -p 8081:8081 openspec-backend:0.1.0
```

---

## 4. Swagger / OpenAPI Documentation Access

Once the application is running, the frontend team can explore and test all endpoints via the interactive Swagger UI:

- **Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI 3.0 JSON Specification**: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

---

## 5. Architecture Rules & Key Concepts

1. **Test Run is the Primary Domain Entity**: Every test run represents an execution against a specific OpenAPI specification. There are no separate specification entity CRUD workflows.
2. **Asynchronous Execution Workflow**: Creating a test run via `POST /api/v1/tests` returns `202 Accepted` with a `testRunId`. Execution begins asynchronously in subsequent phases.
3. **Dedicated SSE Stream**: Subscribing to execution progress (`GET /api/v1/tests/{testRunId}/events`) is an independent HTTP connection producing `text/event-stream`.
4. **AI Configuration Isolation**: Dedicated endpoints manage LLM provider settings. The API key is accepted via `PUT /api/v1/ai-configuration` but is **never** exposed in any status response (`GET /api/v1/ai-configuration/status`).
5. **Artifact Endpoints**:
   - Final report is served directly as GitHub Flavored Markdown (`text/markdown;charset=UTF-8`).
   - Execution logs are served directly as plain text (`text/plain;charset=UTF-8`) with an optional attachment download endpoint (`/logs/download`).
6. **Consistent Error Handling**: All HTTP 4xx and 5xx errors conform to the standard `ErrorResponse` contract with machine-readable error codes and field-level validation details.

---

## 6. API Endpoints Reference

| Category | HTTP Method | Endpoint | Status Code | Content-Type | Description |
|---|---|---|---|---|---|
| **AI Configuration** | `PUT` | `/api/v1/ai-configuration` | `200 OK` | `application/json` | Configure AI provider, model, endpoint, and API key |
| **AI Configuration** | `GET` | `/api/v1/ai-configuration/status` | `200 OK` | `application/json` | Get AI status (secret API key is omitted) |
| **OpenAPI Validation** | `POST` | `/api/v1/tests/validate` | `200 OK` | `application/json` | Validate OpenAPI schema syntax and structure |
| **Test Run** | `POST` | `/api/v1/tests` | `202 Accepted` | `application/json` | Queue a new test run for asynchronous execution |
| **SSE Stream** | `GET` | `/api/v1/tests/{testRunId}/events` | `200 OK` | `text/event-stream` | Subscribe to real-time test execution events |
| **History** | `GET` | `/api/v1/tests` | `200 OK` | `application/json` | List previous test runs with pagination and filters |
| **Test Run Details** | `GET` | `/api/v1/tests/{testRunId}` | `200 OK` | `application/json` | Get full metadata, steps count, and findings summary |
| **Report Artifact** | `GET` | `/api/v1/tests/{testRunId}/report` | `200 OK` | `text/markdown` | Retrieve final test report in Markdown format |
| **Logs Artifact** | `GET` | `/api/v1/tests/{testRunId}/logs` | `200 OK` | `text/plain` | Retrieve raw test execution logs |
| **Logs Download** | `GET` | `/api/v1/tests/{testRunId}/logs/download` | `200 OK` | `text/plain` | Download raw logs as attachment file |
| **Stop Test Run** | `POST` | `/api/v1/tests/{testRunId}/stop` | `202 Accepted` | `application/json` | Submit cancellation request for an active test run |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/test-batch` | `200 OK` | `application/json` | Get simulated AI test batch command list (`TEST_BATCH`) |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/execution-results` | `200 OK` | `application/json` | Get simulated backend HTTP execution results (`BATCH_EXECUTION_RESULTS`) |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/batch-analysis` | `200 OK` | `application/json` | Get simulated AI verdicts and security findings (`BATCH_ANALYSIS`) |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/test-run-completed` | `200 OK` | `application/json` | Get simulated test completion and Markdown report (`TEST_RUN_COMPLETED`) |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/test-failed` | `200 OK` | `application/json` | Get simulated run failure payload (`TEST_FAILED`) |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/full-flow` | `200 OK` | `application/json` | Get all 7 protocol messages in conversational sequence |
| **Protocol Simulation** | `GET` | `/api/v1/simulation/events` | `200 OK` | `text/event-stream` | Stream live simulated protocol events via SSE in real time |

---

## 7. Data Models & Enums

### Enums

#### `AiProvider` (Open-Source Inference Providers)
- `GROQ`: Ultra-low latency open-source inference (Llama, Mistral)
- `TOGETHER_AI`: Serverless open-source models
- `HUGGING_FACE`: Serverless and dedicated inference endpoints
- `MISTRAL_AI`: Mistral & Mixtral models
- `DEEPINFRA`: Serverless open-source model hosting
- `FIREWORKS_AI`: Low-latency open-weights models
- `OPENROUTER`: Gateway for open-source LLMs
- `OLLAMA`: Self-hosted local model runner

#### `OpenSourceAiModel` (Curated Open-Source Models)
Users can **only** choose from this strict enum list:
- `LLAMA_3_3_70B`: Meta Llama 3.3 70B Instruct
- `LLAMA_3_1_8B`: Meta Llama 3.1 8B Instruct (Lightweight & fast)
- `LLAMA_3_1_70B`: Meta Llama 3.1 70B Instruct
- `DEEPSEEK_R1`: DeepSeek R1 reasoning model
- `DEEPSEEK_V3`: DeepSeek V3 Mixture-of-Experts (MoE)
- `MISTRAL_7B`: Mistral 7B Instruct
- `MIXTRAL_8X7B`: Mixtral 8x7B MoE Instruct
- `QWEN_2_5_72B`: Qwen 2.5 72B Instruct
- `CODELLAMA_70B`: Meta CodeLlama 70B Instruct
- `PHI_4`: Microsoft Phi-4 14B reasoning model

#### `TestType`
- `FAST`: Fast sanity and schema conformance checks
- `MEDIUM`: Standard boundary values and edge case exploration
- `DEEP`: Deep fuzzing, stateful flow testing, and vulnerability probes

#### `TestRunStatus`
- `PENDING`: Queued and waiting for execution worker
- `RUNNING`: Actively running test scenarios
- `COMPLETED`: Finished execution successfully
- `FAILED`: Execution halted due to fatal error
- `STOPPED`: Cancelled by user request

#### `TestEventType` (SSE Stream)
- `TEST_RUN_STARTED`: Test execution initialized
- `TEST_STEP_STARTED`: A specific test step or endpoint scenario began
- `TEST_STEP_COMPLETED`: A test step completed
- `FINDING_DISCOVERED`: An anomaly, defect, or vulnerability was found
- `PROGRESS_UPDATED`: Progress metrics updated (step count, percentage)
- `WARNING`: Non-fatal warning emitted
- `TEST_RUN_FAILED`: Test run failed
- `TEST_RUN_COMPLETED`: Test run successfully completed all steps
- `TEST_RUN_STOPPED`: Test run cancelled
- `FINAL_REPORT_AVAILABLE`: Markdown report is ready for download

---

## 8. Sample Payloads

### A. AI Configuration
**Request (`PUT /api/v1/ai-configuration`)**:
```json
{
  "provider": "GROQ",
  "model": "LLAMA_3_3_70B",
  "apiKey": "gsk_abc123xyz789...",
  "apiEndpoint": "https://api.groq.com/openai/v1"
}
```

**Response (`GET /api/v1/ai-configuration/status` or `PUT`)**:
```json
{
  "configured": true,
  "provider": "GROQ",
  "model": "LLAMA_3_3_70B",
  "apiEndpoint": "https://api.groq.com/openai/v1",
  "apiKeyConfigured": true,
  "updatedAt": "2026-10-20T12:00:00Z",
  "message": "Open-source AI provider and model configuration successfully registered (contract stub)"
}
```

### B. OpenAPI Document Validation
**Request (`POST /api/v1/tests/validate`)**:
```json
{
  "openApiDocument": "{\"openapi\": \"3.0.3\", \"info\": {\"title\": \"Payment API\", \"version\": \"1.0.0\"}, \"paths\": {\"/charges\": {\"post\": {\"summary\": \"Create charge\"}}}}"
}
```

**Response (`200 OK`)**:
```json
{
  "valid": true,
  "errors": [],
  "warnings": [
    "Optional summary missing on some operations"
  ],
  "detectedVersion": "3.0.3",
  "endpointsCount": 15,
  "message": "OpenAPI specification passed validation checks"
}
```

### C. Create Test Run
**Request (`POST /api/v1/tests`)**:
```json
{
  "name": "Payment Service Regression Suite",
  "openApiDocument": "{\"openapi\": \"3.0.3\", ...}",
  "testType": "FAST"
}
```

**Response (`202 Accepted`)**:
```json
{
  "testRunId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Payment Service Regression Suite",
  "testType": "FAST",
  "status": "PENDING",
  "createdAt": "2026-10-20T12:00:00Z",
  "message": "Test run queued for asynchronous execution. Subscribe to SSE events for progress updates."
}
```

### D. Server-Sent Events (SSE) Stream
**Endpoint**: `GET /api/v1/tests/{testRunId}/events`  
**Media Type**: `text/event-stream`  
**Event payload format (`TestRunEventDto`)**:
```json
{
  "eventId": "evt-10293",
  "testRunId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "TEST_STEP_STARTED",
  "timestamp": "2026-10-20T12:01:10Z",
  "message": "Executing test step: POST /v1/charges with boundary amount 999999.99",
  "details": {
    "stepIndex": 3,
    "endpoint": "/v1/charges",
    "method": "POST",
    "expectedStatus": 201
  }
}
```

### E. Standard Error Response
In case of validation failures, missing entities, or server issues, responses adhere to `ErrorResponse`:
```json
{
  "code": "VALIDATION_FAILED",
  "message": "The request payload contains invalid or missing required fields",
  "timestamp": "2026-10-20T12:00:00Z",
  "path": "/api/v1/tests",
  "details": [
    {
      "field": "name",
      "message": "Test run name is required"
    },
    {
      "field": "testType",
      "message": "Test type is required"
    }
  ]
}
```

---

## 9. Protocol Simulation & Frontend Handoff Guide

To allow the frontend team to develop UI dashboards, live test stream monitors, findings lists, and Markdown report viewers **immediately** without depending on a live AI key or target service, the backend exposes **Protocol Simulation Endpoints** under `/api/v1/simulation`.

### How the AI Testing Protocol Works:
1. **`TEST_BATCH`**: The AI plans and proposes an ordered list of HTTP commands to execute against the target API.
2. **`BATCH_EXECUTION_RESULTS`**: The backend engine executes the commands, collects the actual status code, headers, response bodies, and assertion verdicts, and returns this evidence to the AI.
3. **`BATCH_ANALYSIS`**: The AI evaluates the execution results against the OpenAPI specification, flags defects/vulnerabilities as severity-ranked `findings` (HIGH, MEDIUM, LOW), assigns `PASSED`/`FAILED` command verdicts, and instructs the next action (`NEXT_TEST`, `EXECUTE_BATCH`, or `FINISH_TESTING`).
4. **`TEST_RUN_COMPLETED`**: Concludes the test run with overall metrics and an authoritative GitHub Flavored Markdown report.

---

### Simulation Endpoints Reference:

#### 1. `GET /api/v1/simulation/test-batch?sequence=1|2`
- **Sequence 1**: Baseline discovery checks (`GET /v1/products`, `POST /v1/orders`).
- **Sequence 2**: Edge cases and fuzzing probes (negative order quantity, SQL injection attempt).
- **Sample Payload**:
```json
{
  "type": "TEST_BATCH",
  "testRunId": "e3b0c442-98fc-1c14-9afb-f4c8996fb924",
  "testId": "ecommerce-checkout-flow",
  "testName": "E-Commerce Core Flow & Boundary Verification",
  "testType": "FAST",
  "batch": {
    "batchId": "batch-001",
    "sequence": 1,
    "isLastBatch": false
  },
  "commands": [
    {
      "commandId": "cmd-001",
      "order": 1,
      "name": "List Active Products Catalog",
      "request": {
        "method": "GET",
        "path": "/v1/products",
        "queryParams": { "category": "electronics", "limit": "10" },
        "headers": { "Accept": "application/json" }
      },
      "expected": {
        "statusCodes": [200],
        "contentType": "application/json"
      },
      "timeoutMs": 5000
    }
  ],
  "nextAction": "EXECUTE_BATCH"
}
```

#### 2. `GET /api/v1/simulation/execution-results?sequence=1|2`
- **Sample Payload (Sequence 2 demonstrating unhandled 500 crash)**:
```json
{
  "type": "BATCH_EXECUTION_RESULTS",
  "testRunId": "e3b0c442-98fc-1c14-9afb-f4c8996fb924",
  "testId": "ecommerce-checkout-flow",
  "batchId": "batch-002",
  "batchStatus": "COMPLETED",
  "executionResults": [
    {
      "commandId": "cmd-003",
      "order": 1,
      "name": "Negative Order Quantity Validation Probe",
      "statusCode": 500,
      "durationMs": 230,
      "responseBody": "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"java.lang.IllegalArgumentException: Total items cannot be negative\"}",
      "backendValidation": {
        "statusPassed": false,
        "contentTypePassed": true,
        "evidence": "Expected HTTP status in [400, 422] but received 500 Internal Server Error"
      }
    }
  ]
}
```

#### 3. `GET /api/v1/simulation/batch-analysis?sequence=1|2`
- **Sample Payload (Sequence 2 showing AI findings)**:
```json
{
  "type": "BATCH_ANALYSIS",
  "testRunId": "e3b0c442-98fc-1c14-9afb-f4c8996fb924",
  "batchId": "batch-002",
  "status": "FAILED",
  "report": {
    "summary": "Batch 2 identified a severe robustness defect: submitting a negative quantity triggers an unhandled 500 Internal Server Error with internal exception details leaked.",
    "commandResults": [
      {
        "commandId": "cmd-003",
        "status": "FAILED",
        "explanation": "Submitting a negative quantity crashed the server with HTTP 500 instead of a standard 400/422 validation response."
      }
    ],
    "findings": [
      {
        "id": "find-001",
        "severity": "HIGH",
        "title": "Unhandled 500 Internal Server Error on Negative Order Quantity",
        "description": "Sending a negative order quantity (-5) to POST /v1/orders causes an unhandled java.lang.IllegalArgumentException yielding HTTP 500 instead of a standard 400 Bad Request validation response.",
        "relatedCommandId": "cmd-003",
        "evidence": "HTTP 500 Internal Server Error; message: Total items cannot be negative"
      }
    ]
  },
  "nextAction": "FINISH_TESTING"
}
```

#### 4. `GET /api/v1/simulation/test-run-completed`
- Returns `TestRunCompletedMessage` with summary metrics and the full GitHub Flavored Markdown report.

#### 5. `GET /api/v1/simulation/full-flow`
- Returns an array containing the entire 7-message dialogue (`List<ProtocolMessage>`) in exact chronological sequence.

#### 6. `GET /api/v1/simulation/events?intervalMs=1000` (Real-time SSE Stream)
- Connect using standard browser `EventSource` in React/Vue/Angular:
```typescript
const eventSource = new EventSource('http://localhost:8081/api/v1/simulation/events?intervalMs=1000');

eventSource.addEventListener('TEST_BATCH', (event) => {
  const batch = JSON.parse(event.data);
  console.log('AI generated batch:', batch);
});

eventSource.addEventListener('BATCH_EXECUTION_RESULTS', (event) => {
  const results = JSON.parse(event.data);
  console.log('Execution results:', results);
});

eventSource.addEventListener('BATCH_ANALYSIS', (event) => {
  const analysis = JSON.parse(event.data);
  console.log('Analysis and findings:', analysis.report.findings);
});

eventSource.addEventListener('TEST_RUN_COMPLETED', (event) => {
  const completed = JSON.parse(event.data);
  console.log('Markdown report:', completed.finalReport.content);
  eventSource.close();
});
```

---

## 10. Project Directory Structure

```
com.openspec
├── OpenSpecApplication.java          # Spring Boot main application class
├── config
│   └── OpenApiConfig.java            # Springdoc OpenAPI & Swagger UI metadata
├── controller
│   ├── AiConfigurationController.java   # Endpoints for AI settings and status
│   ├── ProtocolSimulationController.java# Mock protocol sandbox endpoints & SSE stream
│   └── TestRunController.java           # Endpoints for validation, runs, SSE, artifacts
├── dto
│   ├── event
│   │   └── TestRunEventDto.java         # SSE event payload structure
│   ├── request
│   │   ├── AiConfigurationRequest.java
│   │   ├── CreateTestRunRequest.java
│   │   └── ValidateOpenApiRequest.java
│   └── response
│       ├── AiConfigurationStatusResponse.java
│       ├── CreateTestRunResponse.java
│       ├── ErrorDetailDto.java
│       ├── ErrorResponse.java
│       ├── OpenApiValidationErrorDto.java
│       ├── OpenApiValidationResponse.java
│       ├── PagedResponse.java
│       ├── StopTestRunResponse.java
│       ├── TestRunDetailsResponse.java
│       └── TestRunHistoryItemResponse.java
├── enums
│   ├── AiProvider.java
│   ├── TestEventType.java
│   ├── TestRunStatus.java
│   └── TestType.java
├── exception
│   ├── GlobalExceptionHandler.java      # Centralized error mapping to ErrorResponse
│   └── ResourceNotFoundException.java
└── service
    ├── AiConfigurationService.java      # Service contract interface
    ├── ProtocolSimulationService.java   # Generates realistic mock protocol data and SSE streams
    ├── TestRunService.java              # Service contract interface
    └── impl
        ├── AiConfigurationServiceImpl.java # Stub implementation
        └── TestRunServiceImpl.java         # Stub implementation
```
