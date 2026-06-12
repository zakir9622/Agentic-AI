"use client";

import * as React from "react";
import { GlassButton, GlassInput } from "@/components/ui";
import { resendVerificationAction, type ActionState } from "@/app/actions/auth";

export function ResendVerification() {
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    resendVerificationAction,
    {}
  );
  const [cooldown, setCooldown] = React.useState(0);

  const [lastState, setLastState] = React.useState(state);
  if (state !== lastState) {
    setLastState(state);
    if (state.ok) setCooldown(60);
  }

  React.useEffect(() => {
    if (cooldown <= 0) return;
    const t = setInterval(() => setCooldown((c) => c - 1), 1000);
    return () => clearInterval(t);
  }, [cooldown]);

  return (
    <form action={formAction} className="flex w-full flex-col gap-3">
      <GlassInput
        label="Email" name="email" type="email" required autoComplete="email"
      />
      <GlassButton
        type="submit" variant="secondary" fullWidth
        loading={pending} disabled={cooldown > 0}
      >
        {cooldown > 0 ? `Resend available in ${cooldown}s` : "Resend verification email"}
      </GlassButton>
      {state.message && (
        <p role="status" aria-live="polite" className="text-xs text-[var(--color-success)]">
          {state.message}
        </p>
      )}
    </form>
  );
}
