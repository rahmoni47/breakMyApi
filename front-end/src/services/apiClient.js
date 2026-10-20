const DEFAULT_BASE_URL = "http://localhost:8081";

export function getApiBaseUrl() {
  return import.meta.env.VITE_OPENSPEC_API_BASE_URL || DEFAULT_BASE_URL;
}

export function isBackendEnabled() {
  return String(import.meta.env.VITE_USE_BACKEND || "false") === "true";
}

export async function request(path, options = {}) {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });

  if (!response.ok) {
    let errorPayload = null;
    try {
      errorPayload = await response.json();
    } catch {
      errorPayload = { message: response.statusText };
    }

    const error = new Error(errorPayload?.message || "Request failed");
    error.payload = errorPayload;
    error.status = response.status;
    throw error;
  }

  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}
