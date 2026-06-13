import * as React from "react";
import { cn } from "@/lib/utils";

type BadgeVariant = "gold" | "purple" | "success" | "error" | "warning" | "info" | "neutral";

interface GlassBadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: "sm" | "md";
}

const variantMap: Record<BadgeVariant, string> = {
  gold:    "bg-[rgba(201,169,110,0.15)] text-[var(--color-gold)] border-[rgba(201,169,110,0.3)]",
  purple:  "bg-[rgba(139,94,155,0.15)] text-[var(--color-amethyst-light)] border-[rgba(139,94,155,0.3)]",
  success: "bg-[rgba(134,239,172,0.12)] text-[var(--color-success)] border-[rgba(134,239,172,0.25)]",
  error:   "bg-[rgba(252,165,165,0.12)] text-[var(--color-error)] border-[rgba(252,165,165,0.25)]",
  warning: "bg-[rgba(252,211,77,0.12)]  text-[var(--color-warning)] border-[rgba(252,211,77,0.25)]",
  info:    "bg-[rgba(147,197,253,0.12)] text-[var(--color-info)] border-[rgba(147,197,253,0.25)]",
  neutral: "bg-[var(--color-glass-bg)] text-[var(--color-text-secondary)] border-[var(--color-glass-border)]",
};

export function GlassBadge({
  children,
  className,
  variant = "neutral",
  size = "md",
  ...props
}: GlassBadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 border font-medium rounded-full",
        size === "sm" ? "px-2 py-0.5 text-xs" : "px-2.5 py-0.5 text-xs",
        variantMap[variant],
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
}
