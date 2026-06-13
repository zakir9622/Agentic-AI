import * as React from "react";
import { cn } from "@/lib/utils";

interface GlassCardProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: "sm" | "md" | "lg";
  glow?: "gold" | "purple" | "none";
  hoverable?: boolean;
  padding?: "none" | "sm" | "md" | "lg";
}

const paddingMap = {
  none: "",
  sm:   "p-4",
  md:   "p-6",
  lg:   "p-8",
};

const glowMap = {
  none:   "",
  gold:   "hover:[box-shadow:var(--shadow-glass),var(--shadow-glow-gold)]",
  purple: "hover:[box-shadow:var(--shadow-glass),var(--shadow-glow-purple)]",
};

export function GlassCard({
  children,
  className,
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
        `glass-${size}`,
        paddingMap[padding],
        hoverable && "glass-hover cursor-pointer",
        glowMap[glow],
        "[transition:background_300ms_cubic-bezier(0.4,0,0.2,1),box-shadow_300ms_cubic-bezier(0.4,0,0.2,1)]",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}
