import Link from "next/link";
import { GlassCard } from "@/components/ui";

/** Shared split-layout for all auth pages: animated mesh + centred glass card */
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mesh-bg flex min-h-[85vh] items-center justify-center px-4 py-12">
      <div className="relative z-10 w-full max-w-[440px]">
        <Link
          href="/"
          className="mb-6 block text-center font-display text-2xl text-[var(--color-text-primary)]"
        >
          Nayabi<span className="text-[var(--color-gold)]"> Collection</span>
        </Link>
        <GlassCard padding="lg" size="lg">
          <h1 className="text-2xl text-[var(--color-text-primary)]">{title}</h1>
          {subtitle && (
            <p className="mt-1.5 text-sm text-[var(--color-text-secondary)]">{subtitle}</p>
          )}
          <div className="mt-6">{children}</div>
        </GlassCard>
      </div>
    </div>
  );
}
