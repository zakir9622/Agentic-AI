"use client";

import * as React from "react";
import Link from "next/link";
import { MailCheck } from "lucide-react";
import { AuthShell } from "@/components/auth/auth-shell";
import { GlassButton, GlassInput } from "@/components/ui";
import { forgotPasswordAction, type ActionState } from "@/app/actions/auth";

export default function ForgotPasswordPage() {
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    forgotPasswordAction,
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
    <AuthShell
      title="Forgot your password?"
      subtitle="Enter your email and we'll send you a reset link."
    >
      {state.ok ? (
        <div aria-live="polite" className="flex flex-col items-center gap-4 text-center">
          <MailCheck className="h-12 w-12 text-[var(--color-gold)]" aria-hidden="true" />
          <p className="text-sm text-[var(--color-text-secondary)]">{state.message}</p>
          <form action={formAction} className="w-full">
            <input type="hidden" name="email" value={String(state.message ? "" : "")} />
            <GlassButton
              type="button" variant="secondary" fullWidth disabled={cooldown > 0}
              onClick={() => window.location.reload()}
            >
              {cooldown > 0 ? `Try again in ${cooldown}s` : "Send another link"}
            </GlassButton>
          </form>
        </div>
      ) : (
        <form action={formAction} className="flex flex-col gap-4" aria-label="Reset password request">
          <GlassInput
            label="Email" name="email" type="email" required autoComplete="email" autoFocus
            error={state.errors?.email}
          />
          <GlassButton type="submit" size="lg" fullWidth loading={pending}>
            Send Reset Link
          </GlassButton>
        </form>
      )}

      <p className="mt-6 text-center text-sm text-[var(--color-text-secondary)]">
        Remembered it?{" "}
        <Link href="/login" className="text-[var(--color-gold)] underline underline-offset-2">
          Back to sign in
        </Link>
      </p>
    </AuthShell>
  );
}
