"use client";

import * as React from "react";
import { Gift, Copy } from "lucide-react";
import { issueGiftCardAction, type IssueState } from "@/app/actions/admin-gift-cards";

const initial: IssueState = {};

export function IssueGiftCardForm() {
  const [state, action, pending] = React.useActionState(issueGiftCardAction, initial);
  const [copied, setCopied] = React.useState(false);

  async function copy(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard unavailable */
    }
  }

  return (
    <form action={action} className="flex flex-col gap-3">
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-xs text-[var(--color-text-muted)]">
          Amount (₹)
          <input
            name="amountRupees"
            type="number"
            min={1}
            required
            placeholder="1000"
            className="glass-input !py-2 text-sm"
          />
        </label>
        <label className="flex flex-col gap-1 text-xs text-[var(--color-text-muted)]">
          Recipient email (optional)
          <input
            name="recipientEmail"
            type="email"
            placeholder="recipient@example.com"
            className="glass-input !py-2 text-sm"
          />
        </label>
      </div>
      <button
        type="submit"
        disabled={pending}
        className="btn-gold flex items-center justify-center gap-1.5 rounded-lg px-4 py-2 text-sm font-semibold disabled:opacity-50"
      >
        <Gift className="h-4 w-4" aria-hidden="true" />
        {pending ? "Issuing…" : "Issue gift card"}
      </button>

      {state.message && (
        <p
          className={
            state.ok
              ? "text-xs text-[var(--color-success)]"
              : "text-xs text-[var(--color-error)]"
          }
        >
          {state.message}
        </p>
      )}
      {state.ok && state.code && (
        <button
          type="button"
          onClick={() => copy(state.code!)}
          className="flex items-center gap-2 self-start rounded-lg border border-[var(--color-glass-border)] bg-[var(--color-glass-bg)] px-3 py-2 font-mono text-sm text-[var(--color-text-primary)]"
        >
          {state.code}
          <Copy
            className="h-3.5 w-3.5 text-[var(--color-text-muted)]"
            aria-hidden="true"
          />
          {copied && <span className="text-xs text-[var(--color-success)]">copied</span>}
        </button>
      )}
    </form>
  );
}
