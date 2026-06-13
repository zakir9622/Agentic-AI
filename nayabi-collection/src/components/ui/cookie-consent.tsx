"use client";

import * as React from "react";
import { GlassCard, GlassButton } from "@/components/ui";

const COOKIE_KEY = "nc_cookie_consent";

export function CookieConsent() {
  // Always start false on server — set after hydration to avoid mismatch
  const [visible, setVisible] = React.useState(false);

  React.useEffect(() => {
    setVisible(!localStorage.getItem(COOKIE_KEY));
  }, []);

  function accept() {
    localStorage.setItem(COOKIE_KEY, "accepted");
    setVisible(false);
  }

  function decline() {
    localStorage.setItem(COOKIE_KEY, "declined");
    setVisible(false);
  }

  if (!visible) return null;

  return (
    <div
      role="dialog"
      aria-label="Cookie consent"
      aria-modal="false"
      className="fixed bottom-4 left-4 right-4 z-50 max-w-lg mx-auto"
    >
      <GlassCard padding="md" className="shadow-2xl border-[var(--color-gold)]/20">
        <p className="text-sm text-[var(--color-text-secondary)] leading-relaxed">
          We use cookies for essential site functionality (cart, session) and optional analytics
          to improve your experience. By accepting, you consent to our{" "}
          <a href="/privacy" className="text-[var(--color-gold)] underline underline-offset-2">
            Privacy Policy
          </a>
          .
        </p>
        <div className="flex gap-3 mt-4">
          <GlassButton type="button" size="sm" onClick={accept}>
            Accept all
          </GlassButton>
          <GlassButton type="button" size="sm" variant="ghost" onClick={decline}>
            Essential only
          </GlassButton>
        </div>
      </GlassCard>
    </div>
  );
}
