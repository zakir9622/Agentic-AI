"use client";

import * as React from "react";
import { GlassCard, GlassButton } from "@/components/ui";

const COOKIE_KEY = "nc_cookie_consent";

// Minimal external store over localStorage. Reading consent through
// useSyncExternalStore lets us derive visibility during render instead of
// calling setState inside an effect (which triggers cascading re-renders).
const listeners = new Set<() => void>();

function subscribe(cb: () => void) {
  listeners.add(cb);
  return () => listeners.delete(cb);
}

function getSnapshot() {
  return localStorage.getItem(COOKIE_KEY) ?? "";
}

// On the server (and during hydration) report a decided value so the banner
// stays out of the initial HTML and appears only after the client reads storage.
function getServerSnapshot() {
  return "ssr";
}

function setConsent(value: "accepted" | "declined") {
  localStorage.setItem(COOKIE_KEY, value);
  listeners.forEach((l) => l());
}

export function CookieConsent() {
  const consent = React.useSyncExternalStore(
    subscribe,
    getSnapshot,
    getServerSnapshot
  );

  // Empty string === no choice recorded yet → show the banner.
  if (consent !== "") return null;

  return (
    // Anchored to the bottom-left corner on desktop (a slim full-width bar on
    // mobile) so it never sits over centered hero CTAs. z-30 keeps it below the
    // navbar (z-40) and cart drawer.
    <div
      role="dialog"
      aria-label="Cookie consent"
      aria-modal="false"
      className="fixed bottom-4 left-4 right-4 z-30 mx-auto max-w-md sm:right-auto sm:mx-0"
    >
      <GlassCard padding="md" className="border-[var(--color-gold)]/20 shadow-2xl">
        <p className="text-sm leading-relaxed text-[var(--color-text-secondary)]">
          We use cookies for essential site functionality (cart, session) and optional
          analytics to improve your experience. See our{" "}
          <a href="/privacy" className="text-[var(--color-gold)] underline underline-offset-2">
            Privacy Policy
          </a>
          .
        </p>
        <div className="mt-4 flex gap-2.5">
          <GlassButton
            type="button"
            size="sm"
            onClick={() => setConsent("accepted")}
          >
            Accept
          </GlassButton>
          <GlassButton
            type="button"
            size="sm"
            variant="secondary"
            onClick={() => setConsent("declined")}
          >
            Decline
          </GlassButton>
        </div>
      </GlassCard>
    </div>
  );
}
