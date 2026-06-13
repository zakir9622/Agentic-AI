"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput } from "@/components/ui";
import { adminLoginAction } from "@/app/actions/admin-auth";
import { Shield } from "lucide-react";

interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

export default function AdminLoginPage() {
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    adminLoginAction,
    {}
  );

  return (
    <div className="mesh-bg min-h-screen flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="flex flex-col items-center mb-8 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-gold)]/15 mb-4">
            <Shield className="h-6 w-6 text-[var(--color-gold)]" aria-hidden="true" />
          </div>
          <h1 className="text-2xl text-[var(--color-text-primary)]">Admin Login</h1>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Nayabi Collection Dashboard
          </p>
        </div>

        <GlassCard padding="lg">
          <form action={formAction} className="flex flex-col gap-4">
            <GlassInput
              label="Email" name="email" type="email" required autoComplete="username"
              autoFocus error={state.errors?.email}
            />
            <GlassInput
              label="Password" name="password" type="password" required
              autoComplete="current-password" error={state.errors?.password}
            />
            {state.errors?.form && (
              <p role="alert" className="text-sm text-[var(--color-error)]">
                {state.errors.form}
              </p>
            )}
            <GlassButton type="submit" size="lg" fullWidth loading={pending}>
              Sign in
            </GlassButton>
          </form>
        </GlassCard>
      </div>
    </div>
  );
}
