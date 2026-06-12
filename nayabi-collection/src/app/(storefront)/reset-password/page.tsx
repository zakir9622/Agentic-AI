import Link from "next/link";
import { KeyRound } from "lucide-react";
import { AuthShell } from "@/components/auth/auth-shell";
import { GlassButton } from "@/components/ui";
import { checkPasswordResetToken } from "@/lib/tokens";
import { ResetPasswordForm } from "./reset-form";

export const metadata = { title: "Reset Password" };

export default async function ResetPasswordPage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;

  const check = token
    ? await checkPasswordResetToken(token).catch(
        () => ({ ok: false as const, reason: "invalid" as const })
      )
    : ({ ok: false, reason: "invalid" } as const);

  if (!check.ok) {
    const messages = {
      invalid: "This reset link is invalid or missing.",
      expired: "This reset link has expired — links are valid for 1 hour.",
      used: "This link has already been used.",
    } as const;
    return (
      <AuthShell title="Link not valid">
        <div className="flex flex-col items-center gap-4 text-center">
          <KeyRound className="h-12 w-12 text-[var(--color-warning)]" aria-hidden="true" />
          <p role="alert" className="text-sm text-[var(--color-text-secondary)]">
            {messages[check.reason]}
          </p>
          <Link href="/forgot-password" className="w-full">
            <GlassButton fullWidth>Request a new link</GlassButton>
          </Link>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell title="Set a new password" subtitle="Choose a strong password you haven't used before.">
      <ResetPasswordForm token={token!} />
    </AuthShell>
  );
}
