import { readStorage, writeStorage } from "../utils/storage";

const CONFIG_KEY = "ai-provider-config";
const DEFAULT_CONFIG = {
  configuredProviders: {},
  selectedModelId: "claude-3-5-sonnet",
  selectedProvider: "Anthropic",
  mode: "demo",
};

export function getAiSettings() {
  return readStorage(CONFIG_KEY, DEFAULT_CONFIG);
}

export function saveAiProviderConfiguration({ provider, modelId, apiKeyHint }) {
  const current = getAiSettings();
  const updated = {
    ...current,
    configuredProviders: {
      ...current.configuredProviders,
      [provider]: {
        configured: Boolean(apiKeyHint),
        configuredAt: new Date().toISOString(),
        mode: "demo",
      },
    },
    selectedProvider: provider,
    selectedModelId: modelId,
  };
  writeStorage(CONFIG_KEY, updated);
  return updated;
}

export function setSelectedModel(modelId, provider) {
  const current = getAiSettings();
  const updated = {
    ...current,
    selectedModelId: modelId,
    selectedProvider: provider,
  };
  writeStorage(CONFIG_KEY, updated);
  return updated;
}

export function isProviderConfigured(provider) {
  const current = getAiSettings();
  return Boolean(current.configuredProviders?.[provider]?.configured);
}
