"use client";

import * as React from "react";
import { cn } from "@/lib/utils";
import { CheckCircle, XCircle, AlertCircle, Info, X } from "lucide-react";

export type ToastType = "success" | "error" | "warning" | "info";

export interface Toast {
  id: string;
  type: ToastType;
  title: string;
  description?: string;
  duration?: number;
}

interface ToastItemProps {
  toast: Toast;
  onDismiss: (id: string) => void;
}

const iconMap: Record<ToastType, React.ComponentType<React.SVGProps<SVGSVGElement>>> = {
  success: CheckCircle,
  error:   XCircle,
  warning: AlertCircle,
  info:    Info,
};

const styleMap: Record<ToastType, string> = {
  success: "border-[rgba(134,239,172,0.3)] [--toast-icon:var(--color-success)]",
  error:   "border-[rgba(252,165,165,0.3)] [--toast-icon:var(--color-error)]",
  warning: "border-[rgba(252,211,77,0.3)]  [--toast-icon:var(--color-warning)]",
  info:    "border-[rgba(147,197,253,0.3)] [--toast-icon:var(--color-info)]",
};

const roleMap: Record<ToastType, "status" | "alert"> = {
  success: "status",
  info:    "status",
  warning: "alert",
  error:   "alert",
};

function ToastItem({ toast, onDismiss }: ToastItemProps) {
  const Icon = iconMap[toast.type];

  React.useEffect(() => {
    const t = setTimeout(
      () => onDismiss(toast.id),
      toast.duration ?? 5000
    );
    return () => clearTimeout(t);
  }, [toast, onDismiss]);

  return (
    <div
      role={roleMap[toast.type]}
      aria-live={toast.type === "error" || toast.type === "warning" ? "assertive" : "polite"}
      className={cn(
        "glass glass-sm flex items-start gap-3 p-4 w-full max-w-sm",
        "border animate-[fadeInUp_300ms_cubic-bezier(0.4,0,0.2,1)]",
        styleMap[toast.type]
      )}
    >
      <Icon
        className="w-5 h-5 shrink-0 mt-0.5"
        style={{ color: "var(--toast-icon)" } as React.CSSProperties}
      />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-[var(--color-text-primary)]">
          {toast.title}
        </p>
        {toast.description && (
          <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
            {toast.description}
          </p>
        )}
      </div>
      <button
        onClick={() => onDismiss(toast.id)}
        className="shrink-0 p-0.5 text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors"
        aria-label="Dismiss notification"
      >
        <X className="w-4 h-4" aria-hidden="true" />
      </button>
    </div>
  );
}

// ── Toast store (lightweight, no external dep) ──────────────────────────────

type ToastListener = (toasts: Toast[]) => void;

let toasts: Toast[]        = [];
const listeners = new Set<ToastListener>();

function notify(listeners: Set<ToastListener>, next: Toast[]) {
  toasts = next;
  listeners.forEach((l) => l(next));
}

export const toast = {
  add(t: Omit<Toast, "id">): string {
    const id = Math.random().toString(36).slice(2);
    notify(listeners, [...toasts, { ...t, id }]);
    return id;
  },
  dismiss(id: string) {
    notify(listeners, toasts.filter((t) => t.id !== id));
  },
  success(title: string, description?: string) {
    return this.add({ type: "success", title, description });
  },
  error(title: string, description?: string) {
    return this.add({ type: "error", title, description });
  },
  warning(title: string, description?: string) {
    return this.add({ type: "warning", title, description });
  },
  info(title: string, description?: string) {
    return this.add({ type: "info", title, description });
  },
};

// ── Toast container ─────────────────────────────────────────────────────────

export function ToastContainer() {
  const [items, setItems] = React.useState<Toast[]>(toasts);

  React.useEffect(() => {
    listeners.add(setItems);
    return () => { listeners.delete(setItems); };
  }, []);

  if (items.length === 0) return null;

  return (
    <div
      aria-label="Notifications"
      className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none"
    >
      {items.map((t) => (
        <div key={t.id} className="pointer-events-auto">
          <ToastItem toast={t} onDismiss={toast.dismiss} />
        </div>
      ))}
    </div>
  );
}
