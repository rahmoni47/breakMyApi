export const MODEL_OPTIONS = [
  {
    id: "claude-3-5-sonnet",
    label: "Claude 3.5 Sonnet",
    provider: "Anthropic",
    description: "Recommended for API fuzzing and schema checks.",
  },
  {
    id: "gpt-4o",
    label: "GPT-4o",
    provider: "OpenAI",
    description: "Fast multi-turn edge reasoning.",
  },
  {
    id: "gemini-1-5-pro",
    label: "Gemini 1.5 Pro",
    provider: "Google",
    description: "Large context window for complex specs.",
  },
  {
    id: "mistral-large-2",
    label: "Mistral Large 2",
    provider: "Mistral AI",
    description: "Code-specialized payload validation.",
  },
  {
    id: "llama-3-3-70b",
    label: "Llama 3.3 70B",
    provider: "Groq / Meta",
    description: "Ultra-low latency open-weight option.",
  },
];

export const TEST_DEPTH_OPTIONS = [
  {
    id: "FAST",
    title: "Fast",
    description: "Quick smoke testing of HTTP status codes and core payloads.",
    eta: "~45s",
    checks: "12 checks",
  },
  {
    id: "MEDIUM",
    title: "Medium",
    description:
      "Broader testing with parameterized inputs, boundary values, and schema validation.",
    eta: "~2m 30s",
    checks: "48 checks",
  },
  {
    id: "DEEP",
    title: "Deep",
    description:
      "Extensive edge-case testing, adversarial input checks, auth scenarios, and detailed analysis.",
    eta: "~8m 10s",
    checks: "180+ checks",
  },
];

export const DEMO_PHASES = [
  "Preparing test environment...",
  "Parsing API specification...",
  "Discovering API endpoints...",
  "Generating test scenarios...",
  "Executing API checks...",
  "Validating responses...",
  "Generating test report...",
];
