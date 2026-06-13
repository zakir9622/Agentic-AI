"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { signIn } from "next-auth/react";
import { AuthShell } from "@/components/auth/auth-shell";
import { PasswordInput } from "@/components/auth/password-input";
import { GoogleButton } from "@/components/auth/google-button";
import { GlassButton, GlassInput } from "@/components/ui";

function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const callbackUrl = params.get("callbackUrl") ?? "/account";
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [pending, setPending] = React.useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    const res = await signIn("credentials", { email, password, redirect: false });
    if (res?.error) {
      setError("Incorrect email or password. After 5 failed attempts your account is locked for 15 minutes.");
      setPending(false);
    } else {
      router.push(callbackUrl);
      router.refresh();
    }
  }

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to your account.">
      <GoogleButton />

      {error && (
        <div
          role="alert"
          aria-live="assertive"
          className="mb-4 rounded-xl border border-[rgba(239,68,68,0.3)] bg-[rgba(239,68,68,0.15)] px-4 py-3 text-sm text-[var(--color-error)]"
        >
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-4" aria-label="Sign in">
        <GlassInput
          label="Email" type="email" required autoComplete="email" autoFocus
          value={email} onChange={(e) => setEmail(e.target.value)}
        />
        <PasswordInput
          label="Password" required autoComplete="current-password"
          value={password} onChange={(e) => setPassword(e.target.value)}
        />
        <div className="flex items-center justify-between text-xs">
          <label className="flex items-center gap-2 text-[var(--color-text-secondary)]">
            <input type="checkbox" className="accent-[var(--color-gold)]" /> Remember me
          </label>
          <Link href="/forgot-password" className="text-[var(--color-gold)] underline-offset-2 hover:underline">
            Forgot your password?
          </Link>
        </div>
        <GlassButton type="submit" size="lg" fullWidth loading={pending}>
          Sign In
        </GlassButton>
      </form>

      <p className="mt-6 text-center text-sm text-[var(--color-text-secondary)]">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="text-[var(--color-gold)] underline underline-offset-2">
          Register
        </Link>
      </p>
    </AuthShell>
  );
}

export default function LoginPage() {
  return (
    <React.Suspense>
      <LoginForm />
    </React.Suspense>
  );
}
