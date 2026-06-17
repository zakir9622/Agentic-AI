"use client";

import * as React from "react";

/**
 * Lightweight theme store (replaces next-themes) so we can default to an
 * automatic, time-of-day theme — something next-themes can't express natively
 * (it only knows OS preference). Modes:
 *   - "auto"  → light during the day (07:00–18:59), dark in the evening/night
 *   - "light" → always the Frosted theme
 *   - "dark"  → always the Jewel theme
 *
 * The actual `data-theme` attribute is set BEFORE paint by an inline script in
 * the root layout (no flash). This store just reads/writes the user's choice
 * and re-applies on change, on cross-tab `storage` events, and periodically so
 * an open "auto" session flips when the clock crosses the day/night boundary.
 */
export type ThemeMode = "auto" | "light" | "dark";
type Resolved = "light" | "dark";

export const THEME_STORAGE_KEY = "nc-theme-mode";

/** Daytime = light. Kept in sync with the inline script in layout.tsx. */
export function autoResolved(now = new Date()): Resolved {
  const h = now.getHours();
  return h >= 7 && h < 19 ? "light" : "dark";
}

function resolveMode(m: ThemeMode): Resolved {
  return m === "auto" ? autoResolved() : m;
}

function readMode(): ThemeMode {
  try {
    const v = localStorage.getItem(THEME_STORAGE_KEY);
    return v === "light" || v === "dark" || v === "auto" ? v : "auto";
  } catch {
    return "auto";
  }
}

function applyResolved(r: Resolved) {
  document.documentElement.setAttribute("data-theme", r);
}

const listeners = new Set<() => void>();

/** Set the user's theme mode, persist it, apply it, and notify subscribers. */
export function setThemeMode(mode: ThemeMode) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, mode);
  } catch {
    /* ignore */
  }
  applyResolved(resolveMode(mode));
  listeners.forEach((l) => l());
}

function subscribe(cb: () => void) {
  listeners.add(cb);
  const onStorage = (e: StorageEvent) => {
    if (e.key === THEME_STORAGE_KEY) {
      applyResolved(resolveMode(readMode()));
      cb();
    }
  };
  window.addEventListener("storage", onStorage);
  // Re-evaluate "auto" every 10 min so a long-open tab flips at dawn/dusk.
  const id = window.setInterval(() => {
    if (readMode() === "auto") {
      applyResolved(autoResolved());
      cb();
    }
  }, 600_000);
  return () => {
    listeners.delete(cb);
    window.removeEventListener("storage", onStorage);
    window.clearInterval(id);
  };
}

// Snapshot encodes both mode and resolved theme so either change re-renders.
function getSnapshot(): string {
  const m = readMode();
  return `${m}|${resolveMode(m)}`;
}
function getServerSnapshot(): string {
  return "auto|dark";
}

export interface ThemeState {
  mode: ThemeMode;
  resolved: Resolved;
  setMode: (m: ThemeMode) => void;
}

/** Hydration-safe theme hook (uses useSyncExternalStore — no setState-in-effect). */
export function useTheme(): ThemeState {
  const snap = React.useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);
  const [mode, resolved] = snap.split("|") as [ThemeMode, Resolved];
  return { mode, resolved, setMode: setThemeMode };
}

/** Inline script string — run before paint in <head>/<body> to avoid FOUC. */
export const THEME_INIT_SCRIPT = `(function(){try{var m=localStorage.getItem('${THEME_STORAGE_KEY}')||'auto';var t=m;if(m==='auto'){var h=new Date().getHours();t=(h>=7&&h<19)?'light':'dark';}document.documentElement.setAttribute('data-theme',t);}catch(e){document.documentElement.setAttribute('data-theme','dark');}})();`;
