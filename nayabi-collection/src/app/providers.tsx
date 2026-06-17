"use client";

import * as React from "react";

/**
 * Global client providers. Theme handling moved to a lightweight custom store
 * (`@/components/theme/theme`) so the default can be a time-of-day "Auto" mode
 * — the active `data-theme` is set before paint by an inline script in the root
 * layout, so no React provider is needed here. Kept as a thin wrapper for any
 * future client-side providers.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
