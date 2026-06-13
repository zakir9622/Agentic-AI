"use client";

import * as React from "react";

declare global {
  interface Window {
    turnstile?: {
      render: (container: string | HTMLElement, params: Record<string, unknown>) => string;
      reset: (widgetId: string) => void;
      remove: (widgetId: string) => void;
    };
  }
}

interface TurnstileProps {
  siteKey?: string;
}

export function Turnstile({ siteKey }: TurnstileProps) {
  const key = siteKey ?? process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY;
  const containerRef = React.useRef<HTMLDivElement>(null);
  const widgetId = React.useRef<string | null>(null);

  React.useEffect(() => {
    if (!key || !containerRef.current) return;

    function render() {
      if (!containerRef.current || !window.turnstile) return;
      widgetId.current = window.turnstile.render(containerRef.current, {
        sitekey: key,
        theme: "dark",
        size: "normal",
      });
    }

    if (window.turnstile) {
      render();
    } else {
      const script = document.getElementById("cf-turnstile-script");
      if (!script) {
        const s = document.createElement("script");
        s.id = "cf-turnstile-script";
        s.src = "https://challenges.cloudflare.com/turnstile/v0/api.js";
        s.async = true;
        s.defer = true;
        s.onload = render;
        document.head.appendChild(s);
      } else {
        (script as HTMLScriptElement).addEventListener("load", render);
      }
    }

    return () => {
      if (widgetId.current && window.turnstile) {
        window.turnstile.remove(widgetId.current);
        widgetId.current = null;
      }
    };
  }, [key]);

  if (!key) return null;

  return <div ref={containerRef} className="mt-1" />;
}
