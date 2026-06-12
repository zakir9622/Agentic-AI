import * as React from "react";
import { cn } from "@/lib/utils";

interface GlassSkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "text" | "circle" | "rect";
  width?: string | number;
  height?: string | number;
  lines?: number; // for text variant, number of lines
}

export function GlassSkeleton({
  className,
  variant = "rect",
  width,
  height,
  lines = 1,
  style,
  ...props
}: GlassSkeletonProps) {
  if (variant === "text" && lines > 1) {
    return (
      <div className="flex flex-col gap-2" {...props}>
        {Array.from({ length: lines }).map((_, i) => (
          <div
            key={i}
            className={cn("skeleton h-4 rounded", i === lines - 1 && "w-3/4", className)}
            aria-hidden="true"
          />
        ))}
      </div>
    );
  }

  return (
    <div
      className={cn(
        "skeleton",
        variant === "circle" && "rounded-full",
        variant === "text"   && "h-4",
        variant === "rect"   && "rounded-md",
        className
      )}
      style={{ width, height, ...style }}
      aria-hidden="true"
      {...props}
    />
  );
}

/** Pre-built product card skeleton */
export function ProductCardSkeleton() {
  return (
    <div className="glass glass-md p-0 overflow-hidden" aria-busy="true" aria-label="Loading product">
      <GlassSkeleton variant="rect" className="w-full aspect-[3/4]" />
      <div className="p-4 flex flex-col gap-3">
        <GlassSkeleton variant="text" className="w-3/4 h-4" />
        <GlassSkeleton variant="text" className="w-1/2 h-3" />
        <GlassSkeleton variant="text" className="w-1/3 h-5" />
      </div>
    </div>
  );
}
