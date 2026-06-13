"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AuthShell } from "@/components/auth/auth-shell";
import { PasswordInput } from "@/components/auth/password-input";
import { GoogleButton } from "@/components/auth/google-button";
import { GlassButton, GlassInput, toast } from "@/components/ui";
import { registerAction, type ActionState } from "@/app/actions/auth";
import { Turnstile } from "@/components/auth/turnstile";

export default function RegisterPage() {
  const router = useRouter();
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    registerAction,
    {}
  );

  React.useEffect(() => {
    if (state.ok) {
      toast.success("Account created", "Check your inbox to verify your email.");
      router.push("/verify-email?sent=1");
    }
  }, [state.ok, router]);

  return (
    <AuthShell title="Create your account" subtitle="Join Nayabi Collection — it takes a minute.">
      <GoogleButton label="Sign up with Google" />

      <form action={formAction} className="flex flex-col gap-4" aria-label="Create account">
        <GlassInput
          label="Full name" name="name" required autoComplete="name" autoFocus
          error={state.errors?.name}
        />
        <GlassInput
          label="Email" name="email" type="email" required autoComplete="email"
          error={state.errors?.email}
        />
        <PasswordInput
          label="Password" name="password" required autoComplete="new-password"
          showStrength error={state.errors?.password}
        />
        <PasswordInput
          label="Confirm password" name="confirmPassword" required autoComplete="new-password"
          error={state.errors?.confirmPassword}
        />

        <label className="flex items-start gap-2.5 text-xs text-[var(--color-text-secondary)]">
          <input type="checkbox" name="terms" className="mt-0.5 accent-[var(--color-gold)]" />
          <span>
            I agree to the{" "}
            <Link href="/terms" className="text-[var(--color-gold)] underline underline-offset-2">Terms of Service</Link>{" "}
            and{" "}
            <Link href="/privacy" className="text-[var(--color-gold)] underline underline-offset-2">Privacy Policy</Link>
          </span>
        </label>
        {state.errors?.terms && (
          <p role="alert" className="text-xs text-[var(--color-error)]">{state.errors.terms}</p>
        )}

        <Turnstile />
        {state.message && !state.ok && (
          <p role="alert" className="text-sm text-[var(--color-error)]">{state.message}</p>
        )}
        <GlassButton type="submit" size="lg" fullWidth loading={pending}>
          Create Account
        </GlassButton>
      </form>

      <p className="mt-6 text-center text-sm text-[var(--color-text-secondary)]">
        Already have an account?{" "}
        <Link href="/login" className="text-[var(--color-gold)] underline underline-offset-2">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
