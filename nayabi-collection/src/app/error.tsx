"use client";

import { GlassButton, GlassCard } from "@/components/ui";

export default function ErrorPage({
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="mesh-bg flex min-h-[70vh] items-center justify-center px-4">
      <GlassCard padding="lg" className="relative z-10 w-full max-w-md text-center">
        <p className="font-display text-5xl text-[var(--color-error)]">Oops</p>
        <h1 className="mt-4 text-2xl text-[var(--color-text-primary)]">Something went wrong</h1>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          We&apos;ve been notified. Please try again in a moment.
        </p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <GlassButton onClick={reset}>Try again</GlassButton>
          <GlassButton variant="secondary" onClick={() => (window.location.href = "/")}>
            Go home
          </GlassButton>
        </div>
      </GlassCard>
    </div>
  );
}
