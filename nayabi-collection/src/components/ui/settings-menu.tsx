"use client";

import * as React from "react";
import { Settings, Sun, Moon, Clock, Check } from "lucide-react";
import { useTheme, type ThemeMode } from "@/components/theme/theme";
import { cn } from "@/lib/utils";

const OPTIONS: { value: ThemeMode; label: string; hint: string; Icon: typeof Sun }[] = [
  { value: "auto", label: "Auto", hint: "Follows time of day", Icon: Clock },
  { value: "light", label: "Light", hint: "Frosted", Icon: Sun },
  { value: "dark", label: "Dark", hint: "Jewel", Icon: Moon },
];

/**
 * Settings popover housing the theme control. Replaces the standalone sun/moon
 * toggle so the default can be a time-based "Auto" and the explicit choices sit
 * one layer in. Rows are uniform (the resolved-theme note lives in the footer,
 * not on a single row) so every option has an identical, accurate hit area.
 */
export function SettingsMenu() {
  const { mode, resolved, setMode } = useTheme();
  const [open, setOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && setOpen(false);
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label="Settings"
        title="Settings"
        className="nav-action inline-flex"
      >
        <Settings className="h-5 w-5" aria-hidden="true" />
      </button>

      {open && (
        <div
          role="menu"
          aria-label="Appearance"
          className="nav-mobile-open glass glass-elevated absolute right-0 z-50 mt-2 w-64 origin-top-right overflow-hidden !rounded-2xl p-2"
        >
          <p className="px-2.5 pt-1 pb-2 text-[0.625rem] font-semibold tracking-[0.18em] text-[var(--color-text-muted)] uppercase">
            Appearance
          </p>

          <div className="flex flex-col gap-0.5">
            {OPTIONS.map(({ value, label, hint, Icon }) => {
              const active = mode === value;
              return (
                <button
                  key={value}
                  type="button"
                  role="menuitemradio"
                  aria-checked={active}
                  onClick={() => {
                    setMode(value);
                    setOpen(false);
                  }}
                  className={cn(
                    "flex min-h-11 w-full items-center gap-3 rounded-xl px-2.5 py-2 text-left transition-colors",
                    active
                      ? "bg-[var(--color-glass-bg)]"
                      : "hover:bg-[var(--color-glass-bg)]"
                  )}
                >
                  <span
                    className={cn(
                      "flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border",
                      active
                        ? "border-[var(--color-gold)]/40 text-[var(--color-gold)]"
                        : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)]"
                    )}
                  >
                    <Icon className="h-4 w-4" aria-hidden="true" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-medium text-[var(--color-text-primary)]">
                      {label}
                    </span>
                    <span className="block truncate text-[0.6875rem] text-[var(--color-text-muted)]">
                      {hint}
                    </span>
                  </span>
                  {active && (
                    <Check
                      className="h-4 w-4 shrink-0 text-[var(--color-gold)]"
                      aria-hidden="true"
                    />
                  )}
                </button>
              );
            })}
          </div>

          <p className="mt-1.5 border-t border-[var(--color-glass-border)] px-2.5 pt-2 pb-1 text-[0.6875rem] text-[var(--color-text-muted)]">
            Currently showing the{" "}
            <span className="font-medium text-[var(--color-text-secondary)]">{resolved}</span>{" "}
            theme.
          </p>
        </div>
      )}
    </div>
  );
}
