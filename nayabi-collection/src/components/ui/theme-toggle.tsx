"use client";

import * as React from "react";
import { useTheme } from "next-themes";
import { Moon, Sun } from "lucide-react";
import { cn } from "@/lib/utils";

const emptySubscribe = () => () => {};

/** True only after hydration — without calling setState inside an effect. */
function useHydrated() {
  return React.useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false
  );
}

/**
 * Dark "Jewel" ⇄ light "Frosted" theme switch. Renders a stable placeholder
 * until hydrated so server and client markup match (next-themes resolves the
 * active theme only on the client).
 */
export function ThemeToggle({ className }: { className?: string }) {
  const { resolvedTheme, setTheme } = useTheme();
  const mounted = useHydrated();

  const isDark = resolvedTheme !== "light";

  return (
    <button
      type="button"
      onClick={() => setTheme(isDark ? "light" : "dark")}
      aria-label={mounted ? `Switch to ${isDark ? "light" : "dark"} theme` : "Toggle theme"}
      title={mounted ? `Switch to ${isDark ? "light" : "dark"} theme` : "Toggle theme"}
      className={cn(
        "relative p-2 rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-gold)] transition-colors",
        className
      )}
    >
      {/* Keep both icons mounted; cross-fade via opacity so there is no layout
          shift and the control is identical in size pre/post hydration. */}
      <span className="relative block h-5 w-5">
        <Sun
          className={cn(
            "absolute inset-0 h-5 w-5 transition-all duration-300",
            mounted && !isDark ? "opacity-100 rotate-0 scale-100" : "opacity-0 -rotate-90 scale-50"
          )}
          aria-hidden="true"
        />
        <Moon
          className={cn(
            "absolute inset-0 h-5 w-5 transition-all duration-300",
            !mounted || isDark ? "opacity-100 rotate-0 scale-100" : "opacity-0 rotate-90 scale-50"
          )}
          aria-hidden="true"
        />
      </span>
    </button>
  );
}
