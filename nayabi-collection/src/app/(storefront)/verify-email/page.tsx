import Link from "next/link";
import { MailCheck, MailWarning, CheckCircle } from "lucide-react";
import { AuthShell } from "@/components/auth/auth-shell";
import { ResendVerification } from "./resend-form";
import { GlassButton } from "@/components/ui";
import { consumeVerificationToken } from "@/lib/tokens";

export const metadata = { title: "Verify Email" };

export default async function VerifyEmailPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string; sent?: string }>;
}) {
  const { token, sent } = await searchParams;

  // Token in URL → attempt verification
  if (token) {
    const result = await consumeVerificationToken(token).catch(
      () => ({ ok: false as const, reason: "invalid" as const })
    );

    if (result.ok) {
      return (
        <AuthShell title="Email verified!">
          <div className="flex flex-col items-center gap-4 text-center">
            <CheckCircle className="h-12 w-12 text-[var(--color-success)]" aria-hidden="true" />
            <p className="text-sm text-[var(--color-text-secondary)]">
              Your email is confirmed. You can now sign in and start shopping.
            </p>
            <Link href="/login" className="w-full">
              <GlassButton fullWidth size="lg">Sign In</GlassButton>
            </Link>
          </div>
        </AuthShell>
      );
    }

    const messages = {
      invalid: "This verification link is invalid.",
      expired: "This verification link has expired.",
      used: "This link was already used — your email may already be verified.",
    } as const;

    return (
      <AuthShell title="Verification failed">
        <div className="flex flex-col items-center gap-4 text-center">
          <MailWarning className="h-12 w-12 text-[var(--color-warning)]" aria-hidden="true" />
          <p role="alert" className="text-sm text-[var(--color-text-secondary)]">
            {messages[result.reason]}
          </p>
          <ResendVerification />
          <Link href="/login" className="text-sm text-[var(--color-gold)] underline underline-offset-2">
            Back to sign in
          </Link>
        </div>
      </AuthShell>
    );
  }

  // Default: "check your inbox" state (after registration)
  return (
    <AuthShell title="Check your inbox">
      <div className="flex flex-col items-center gap-4 text-center">
        <MailCheck className="h-12 w-12 text-[var(--color-gold)]" aria-hidden="true" />
        <p className="text-sm text-[var(--color-text-secondary)]">
          {sent
            ? "We've sent a verification link to your email. Click it to activate your account."
            : "Enter your email below and we'll send you a fresh verification link."}
        </p>
        <ResendVerification />
        <Link href="/login" className="text-sm text-[var(--color-gold)] underline underline-offset-2">
          Back to sign in
        </Link>
      </div>
    </AuthShell>
  );
}
