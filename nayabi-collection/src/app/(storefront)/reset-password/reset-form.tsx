"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { PasswordInput } from "@/components/auth/password-input";
import { GlassButton, toast } from "@/components/ui";
import { resetPasswordAction, type ActionState } from "@/app/actions/auth";

export function ResetPasswordForm({ token }: { token: string }) {
  const router = useRouter();
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    resetPasswordAction,
    {}
  );

  React.useEffect(() => {
    if (state.ok) {
      toast.success("Password updated", "You can now sign in.");
      router.push("/login");
    }
  }, [state.ok, router]);

  return (
    <form action={formAction} className="flex flex-col gap-4" aria-label="Set new password">
      <input type="hidden" name="token" value={token} />
      {state.message && !state.ok && (
        <p role="alert" className="rounded-xl border border-[rgba(239,68,68,0.3)] bg-[rgba(239,68,68,0.15)] px-4 py-3 text-sm text-[var(--color-error)]">
          {state.message}
        </p>
      )}
      <PasswordInput
        label="New password" name="password" required autoComplete="new-password"
        showStrength autoFocus error={state.errors?.password}
      />
      <PasswordInput
        label="Confirm new password" name="confirmPassword" required autoComplete="new-password"
        error={state.errors?.confirmPassword}
      />
      <GlassButton type="submit" size="lg" fullWidth loading={pending}>
        Update Password
      </GlassButton>
    </form>
  );
}
