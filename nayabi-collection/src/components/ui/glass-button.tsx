"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
type ButtonSize    = "sm" | "md" | "lg";

interface GlassButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  icon?: React.ReactNode;
  iconPosition?: "left" | "right";
  fullWidth?: boolean;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:   "btn-gold rounded-xl",
  secondary: "glass glass-sm text-[var(--color-text-primary)] hover:glass-hover border border-[var(--color-glass-border)]",
  ghost:     "bg-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-glass-bg)]",
  danger:    "bg-red-900/30 text-red-300 border border-red-500/30 hover:bg-red-900/50",
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: "px-3 py-1.5 text-sm gap-1.5",
  md: "px-5 py-2.5 text-sm gap-2",
  lg: "px-7 py-3.5 text-base gap-2.5",
};

export function GlassButton({
  children,
  className,
  variant = "primary",
  size = "md",
  loading = false,
  disabled,
  icon,
  iconPosition = "left",
  fullWidth = false,
  ...props
}: GlassButtonProps) {
  const isDisabled = disabled || loading;

  return (
    <button
      disabled={isDisabled}
      aria-busy={loading}
      className={cn(
        "inline-flex items-center justify-center font-medium",
        "transition-all duration-150 ease-[cubic-bezier(0.4,0,0.2,1)]",
        "disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none",
        variantClasses[variant],
        sizeClasses[size],
        fullWidth && "w-full",
        className
      )}
      {...props}
    >
      {loading ? (
        <SpinnerIcon className={size === "sm" ? "w-3.5 h-3.5" : "w-4 h-4"} />
      ) : (
        icon && iconPosition === "left" && icon
      )}
      {children}
      {!loading && icon && iconPosition === "right" && icon}
    </button>
  );
}

function SpinnerIcon({ className }: { className?: string }) {
  return (
    <svg
      className={cn("animate-spin", className)}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle
        className="opacity-25"
        cx="12" cy="12" r="10"
        stroke="currentColor" strokeWidth="4"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
  );
}
