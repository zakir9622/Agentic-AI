import Link from "next/link";
import { GlassButton, GlassCard } from "@/components/ui";

export default function NotFound() {
  return (
    <div className="mesh-bg flex min-h-[70vh] items-center justify-center px-4">
      <GlassCard padding="lg" className="relative z-10 w-full max-w-md text-center">
        <p className="font-display text-7xl text-[var(--color-gold)]">404</p>
        <h1 className="mt-4 text-2xl text-[var(--color-text-primary)]">Page not found</h1>
        <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
          The page you&apos;re looking for has moved or doesn&apos;t exist.
        </p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Link href="/">
            <GlassButton fullWidth>Back to Home</GlassButton>
          </Link>
          <Link href="/shop">
            <GlassButton fullWidth variant="secondary">Browse Shop</GlassButton>
          </Link>
        </div>
      </GlassCard>
    </div>
  );
}
