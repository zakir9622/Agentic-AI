"use client";

export function PrintButton() {
  return (
    <button
      onClick={() => window.print()}
      className="rounded-lg border border-[var(--color-gold)] px-4 py-1.5 text-sm text-[var(--color-gold)] hover:bg-[var(--color-gold)]/10 transition-colors"
    >
      Print / Save PDF
    </button>
  );
}
