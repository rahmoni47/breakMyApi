import { createContext, useContext, useMemo, useState } from "react";
import {
  getAiSettings,
  saveAiProviderConfiguration,
  setSelectedModel,
  isProviderConfigured,
} from "../services/aiProviderService";
import { TEST_DEPTH_OPTIONS } from "../data/demoModels";
import { readStorage, writeStorage } from "../utils/storage";

const SETTINGS_KEY = "app-settings";

const defaultUiSettings = {
  defaultTestDepth: TEST_DEPTH_OPTIONS[1].id,
  reportFormat: "markdown",
};

const SettingsContext = createContext(null);

export function SettingsProvider({ children }) {
  const [aiSettings, setAiSettings] = useState(getAiSettings());
  const [uiSettings, setUiSettings] = useState(
    readStorage(SETTINGS_KEY, defaultUiSettings),
  );

  const saveProviderConfig = ({ provider, modelId, apiKeyHint }) => {
    const next = saveAiProviderConfiguration({ provider, modelId, apiKeyHint });
    setAiSettings(next);
    return next;
  };

  const selectModel = ({ modelId, provider }) => {
    const next = setSelectedModel(modelId, provider);
    setAiSettings(next);
  };

  const updateUiSettings = (patch) => {
    const next = { ...uiSettings, ...patch };
    setUiSettings(next);
    writeStorage(SETTINGS_KEY, next);
  };

  const value = useMemo(
    () => ({
      aiSettings,
      uiSettings,
      saveProviderConfig,
      selectModel,
      isProviderConfigured,
      updateUiSettings,
    }),
    [aiSettings, uiSettings],
  );

  return (
    <SettingsContext.Provider value={value}>
      {children}
    </SettingsContext.Provider>
  );
}

export function useSettings() {
  const context = useContext(SettingsContext);
  if (!context)
    throw new Error("useSettings must be used inside SettingsProvider");
  return context;
}
