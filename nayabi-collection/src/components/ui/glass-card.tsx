import * as React from "react";
import { cn } from "@/lib/utils";

interface GlassCardProps extends React.HTMLAttributes<HTMLDivElement> {
  tier?: "base" | "elevated";
  size?: "sm" | "md" | "lg";
  glow?: "gold" | "purple" | "none";
  hoverable?: boolean;
  padding?: "none" | "sm" | "md" | "lg";
}

const paddingMap = {
  none: "",
  sm:   "p-4",
  md:   "p-6",
  lg:   "p-8 sm:p-10",
};

export function GlassCard({
  children,
  className,
  tier = "base",
  size = "md",
  glow = "none",
  hoverable = false,
  padding = "md",
  ...props
}: GlassCardProps) {
  return (
    <div
      className={cn(
        "glass",
        tier === "elevated" && "glass-elevated",
        `glass-${size}`,
        paddingMap[padding],
        hoverable && "glass-hover cursor-pointer",
        glow === "gold" && "hover:[box-shadow:var(--shadow-glass-elevated),var(--shadow-glow-gold)]",
        glow === "purple" && "hover:[box-shadow:var(--shadow-glass-elevated),var(--shadow-glow-purple)]",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}
