import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";
import { SettingsProvider } from "./context/SettingsContext";
import { TestProvider } from "./context/TestContext";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <SettingsProvider>
      <TestProvider>
        <App />
      </TestProvider>
    </SettingsProvider>
  </React.StrictMode>,
);
