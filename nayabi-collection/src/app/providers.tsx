"use client";

import * as React from "react";
import { ThemeProvider } from "next-themes";

/**
 * Global client providers. Currently hosts next-themes so the storefront can
 * switch between the dark "Jewel" glass theme (default) and the light "Frosted"
 * glass theme. We use a `data-theme` attribute so the ~1000 `var(--color-*)`
 * references across the app resolve against whichever theme block is active.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider
      attribute="data-theme"
      defaultTheme="dark"
      enableSystem={false}
      themes={["dark", "light"]}
      disableTransitionOnChange
    >
      {children}
    </ThemeProvider>
  );
}
