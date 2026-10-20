const SENSITIVE_KEYS = [
  "authorization",
  "apiKey",
  "token",
  "password",
  "secret",
];

function isSensitiveKey(key = "") {
  const lower = key.toLowerCase();
  return SENSITIVE_KEYS.some((segment) => lower.includes(segment));
}

export function redactSensitive(value) {
  if (Array.isArray(value)) return value.map(redactSensitive);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, nestedValue]) => [
        key,
        isSensitiveKey(key) ? "[REDACTED]" : redactSensitive(nestedValue),
      ]),
    );
  }
  return value;
}
