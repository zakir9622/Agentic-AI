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
 * Settings popover housing the theme control. The standalone sun/moon toggle was
 * replaced by this so the default can be time-based "Auto" and the explicit
 * Light/Dark choices live one layer in, out of the primary icon row.
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
          aria-label="Theme"
          className="glass glass-elevated glass-sm absolute right-0 z-50 mt-2 w-56 origin-top-right !rounded-2xl p-2 nav-mobile-open"
        >
          <p className="px-3 pt-1 pb-2 text-[0.625rem] font-semibold tracking-widest text-[var(--color-text-muted)] uppercase">
            Appearance
          </p>
          {OPTIONS.map(({ value, label, hint, Icon }) => {
            const active = mode === value;
            return (
              <button
                key={value}
                role="menuitemradio"
                aria-checked={active}
                onClick={() => {
                  setMode(value);
                  setOpen(false);
                }}
                className={cn(
                  "flex w-full items-center gap-3 rounded-xl px-3 py-2 text-left text-sm transition-colors",
                  active
                    ? "bg-[var(--color-glass-bg)] text-[var(--color-text-primary)]"
                    : "text-[var(--color-text-secondary)] hover:bg-[var(--color-glass-bg)] hover:text-[var(--color-text-primary)]"
                )}
              >
                <Icon
                  className={cn("h-4 w-4 shrink-0", active && "text-[var(--color-gold)]")}
                  aria-hidden="true"
                />
                <span className="flex-1">
                  {label}
                  <span className="block text-[0.6875rem] text-[var(--color-text-muted)]">
                    {value === "auto" ? `${hint} · now ${resolved}` : hint}
                  </span>
                </span>
                {active && (
                  <Check className="h-4 w-4 shrink-0 text-[var(--color-gold)]" aria-hidden="true" />
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
