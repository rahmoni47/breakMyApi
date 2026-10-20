const PREFIX = "openspec.v1";

export function readStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(`${PREFIX}.${key}`);
    if (!raw) return fallback;
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export function writeStorage(key, value) {
  localStorage.setItem(`${PREFIX}.${key}`, JSON.stringify(value));
}
