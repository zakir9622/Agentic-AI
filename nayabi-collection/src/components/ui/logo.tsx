import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Nayabi Collection brand mark — a gold "N" monogram inside a soft glass
 * roundel with a sparkle accent. Uses theme CSS variables so the gold adapts
 * to the active (dark/light) theme. Each instance gets a unique gradient id so
 * multiple logos on one page don't collide.
 */
export function LogoMark({ className }: { className?: string }) {
  const id = React.useId().replace(/:/g, "");
  const gold = `nc-gold-${id}`;
  return (
    <svg
      viewBox="0 0 40 40"
      className={className}
      fill="none"
      role="img"
      aria-label="Nayabi Collection"
    >
      <defs>
        <linearGradient
          id={gold}
          x1="2"
          y1="2"
          x2="38"
          y2="38"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="var(--color-gold-light)" />
          <stop offset="55%" stopColor="var(--color-gold)" />
          <stop offset="100%" stopColor="var(--color-gold-dark)" />
        </linearGradient>
      </defs>
      <rect
        x="1.25"
        y="1.25"
        width="37.5"
        height="37.5"
        rx="11"
        fill="var(--color-glass-bg)"
        stroke={`url(#${gold})`}
        strokeWidth="1.5"
      />
      {/* N monogram */}
      <path
        d="M13 28 V13 L27 28 V13"
        stroke={`url(#${gold})`}
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* sparkle */}
      <path
        d="M30 8 l0.85 2.05 2.05 0.85 -2.05 0.85 -0.85 2.05 -0.85 -2.05 -2.05 -0.85 2.05 -0.85 z"
        fill={`url(#${gold})`}
      />
    </svg>
  );
}

interface LogoProps {
  className?: string;
  markClassName?: string;
  showWordmark?: boolean;
  /** Tailwind text-size class for the wordmark, e.g. "text-xl" */
  wordmarkClassName?: string;
}

export function Logo({
  className,
  markClassName = "h-8 w-8",
  showWordmark = true,
  wordmarkClassName = "text-xl sm:text-2xl",
}: LogoProps) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <LogoMark className={markClassName} />
      {showWordmark && (
        <span
          className={cn(
            "font-display tracking-wide text-[var(--color-text-primary)]",
            wordmarkClassName
          )}
        >
          Nayabi<span className="text-[var(--color-gold)]"> Collection</span>
        </span>
      )}
    </span>
  );
}
