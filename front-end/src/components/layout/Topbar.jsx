import { useState } from "react";
import { Settings, UserCircle2 } from "lucide-react";
import ModelSelector from "../ai/ModelSelector";

export default function Topbar({ onOpenSettings }) {
  const [profileOpen, setProfileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-end border-b border-indigo-100 bg-bg-main px-6">
      <div className="flex items-center gap-3">
        <ModelSelector />
        <button
          aria-label="Open settings"
          onClick={onOpenSettings}
          className="rounded-full p-2 text-text-secondary hover:bg-white hover:text-text-main"
        >
          <Settings className="h-5 w-5" />
        </button>
        <div className="relative">
          <button
            aria-label="Open profile"
            onClick={() => setProfileOpen((prev) => !prev)}
            className="rounded-full p-1 text-primary hover:bg-white"
          >
            <UserCircle2 className="h-7 w-7" />
          </button>
          {profileOpen && (
            <div className="absolute right-0 mt-2 w-44 rounded-lg border border-indigo-100 bg-white p-2 text-sm shadow-card">
              <p className="px-2 py-1 text-text-main">Vortex Operator</p>
              <p className="px-2 py-1 text-xs text-text-secondary">
                openspec@vortex.dev
              </p>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
