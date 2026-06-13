"use client";

import * as React from "react";
import { GlassButton, GlassCard, toast } from "@/components/ui";
import { PasswordInput } from "@/components/auth/password-input";
import { changePasswordAction, type ActionState } from "@/app/actions/auth";

export default function SecurityPage() {
  const formRef = React.useRef<HTMLFormElement>(null);
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    changePasswordAction,
    {}
  );

  React.useEffect(() => {
    if (state.ok) {
      toast.success("Password updated", "Your password was changed successfully.");
      formRef.current?.reset();
    }
  }, [state]);

  return (
    <GlassCard padding="lg" className="max-w-lg">
      <h2 className="text-base font-semibold !font-[var(--font-body)]">Change password</h2>
      <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
        Use a strong password you don&apos;t use anywhere else.
      </p>

      {state.message && !state.ok && (
        <p role="alert" className="mt-4 rounded-xl border border-[rgba(239,68,68,0.3)] bg-[rgba(239,68,68,0.15)] px-4 py-3 text-sm text-[var(--color-error)]">
          {state.message}
        </p>
      )}

      <form ref={formRef} action={formAction} className="mt-5 flex flex-col gap-4" aria-label="Change password">
        <PasswordInput
          label="Current password" name="currentPassword" required
          autoComplete="current-password" error={state.errors?.currentPassword}
        />
        <PasswordInput
          label="New password" name="newPassword" required showStrength
          autoComplete="new-password" error={state.errors?.newPassword}
        />
        <PasswordInput
          label="Confirm new password" name="confirmPassword" required
          autoComplete="new-password" error={state.errors?.confirmPassword}
        />
        <label className="flex items-center gap-2.5 text-sm text-[var(--color-text-secondary)]">
          <input type="checkbox" name="signOutOthers" className="accent-[var(--color-gold)]" />
          Sign out all other devices
        </label>
        <GlassButton type="submit" size="lg" loading={pending}>
          Update Password
        </GlassButton>
      </form>
    </GlassCard>
  );
}
