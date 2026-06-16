"use client";

import * as React from "react";
import { Gift, Sparkles, Search, Check } from "lucide-react";
import { GlassCard, GlassButton, GlassBadge } from "@/components/ui";
import { checkBalanceAction } from "@/app/actions/gift-cards";
import type { GiftCardBalance } from "@/lib/gift-cards";
import { BRAND, GIFT_CARD_DENOMINATIONS } from "@/lib/constants";
import { formatPrice } from "@/lib/utils";

const initial: GiftCardBalance = { found: false };

const steps = [
  { title: "Choose an amount", text: "Pick a denomination that suits the occasion." },
  {
    title: "We send the code",
    text: "A unique gift code is delivered to the recipient by email/WhatsApp.",
  },
  { title: "Redeem at checkout", text: "Apply the code towards any order on the store." },
];

export default function GiftCardsPage() {
  const [state, action, pending] = React.useActionState(checkBalanceAction, initial);

  return (
    <div className="mesh-bg min-h-screen">
      <div className="relative z-10 mx-auto max-w-5xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        {/* Hero */}
        <div className="text-center">
          <span className="inline-flex items-center gap-2 rounded-full border border-[var(--color-gold)]/25 bg-[var(--color-gold)]/8 px-4 py-1.5 text-xs font-semibold tracking-widest text-[var(--color-gold)] uppercase">
            <Gift className="h-3 w-3" aria-hidden="true" />
            Gift Cards
          </span>
          <h1 className="mt-5 text-4xl text-[var(--color-text-primary)] sm:text-5xl">
            The gift of <em className="gradient-text italic not-italic">choice</em>
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-[var(--color-text-secondary)]">
            Give someone the joy of choosing their own premium modest wear. Nayabi gift
            cards never feel like the wrong size or colour.
          </p>
        </div>

        {/* Denominations */}
        <div className="mt-12 grid grid-cols-2 gap-4 sm:grid-cols-4">
          {GIFT_CARD_DENOMINATIONS.map((amt) => (
            <GlassCard key={amt} hoverable padding="md" className="text-center">
              <Sparkles
                className="mx-auto h-5 w-5 text-[var(--color-gold)]"
                aria-hidden="true"
              />
              <p className="font-display mt-3 text-2xl text-[var(--color-text-primary)]">
                {formatPrice(amt)}
              </p>
              <a
                href={BRAND.whatsappUrl(
                  `Hi Nayabi Collection, I'd like to buy a ${formatPrice(amt)} gift card.`
                )}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-gold mt-4 inline-block w-full rounded-lg px-3 py-2 text-sm font-semibold"
              >
                Buy on WhatsApp
              </a>
            </GlassCard>
          ))}
        </div>

        {/* How it works */}
        <div className="mt-16">
          <h2 className="text-center text-2xl text-[var(--color-text-primary)]">
            How it works
          </h2>
          <ol className="mt-8 grid gap-4 sm:grid-cols-3" role="list">
            {steps.map((s, i) => (
              <li key={s.title}>
                <GlassCard padding="md" className="h-full">
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-gold)]/15 text-sm font-semibold text-[var(--color-gold)]">
                    {i + 1}
                  </span>
                  <p className="mt-3 font-medium text-[var(--color-text-primary)]">
                    {s.title}
                  </p>
                  <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                    {s.text}
                  </p>
                </GlassCard>
              </li>
            ))}
          </ol>
        </div>

        {/* Balance checker */}
        <GlassCard tier="elevated" padding="lg" className="mt-16">
          <h2 className="text-xl text-[var(--color-text-primary)]">Check your balance</h2>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Enter your gift card code to see its remaining balance. Try{" "}
            <span className="text-[var(--color-gold)]">NAYABI-GIFT</span> to preview.
          </p>
          <form action={action} className="mt-5 flex flex-col gap-3 sm:flex-row">
            <input
              name="code"
              required
              placeholder="NAYABI-XXXX-XXXX-XXXX"
              className="glass-input sm:flex-1"
              autoComplete="off"
            />
            <GlassButton
              type="submit"
              loading={pending}
              icon={<Search className="h-4 w-4" aria-hidden="true" />}
            >
              Check balance
            </GlassButton>
          </form>

          {!pending && state.error && (
            <p className="mt-4 text-sm text-[var(--color-error)]">{state.error}</p>
          )}

          {!pending && state.found && (
            <div className="mt-5 rounded-xl border border-[var(--color-glass-border)] bg-[var(--color-glass-bg)] p-5">
              <div className="flex items-center justify-between">
                <p className="font-mono text-sm text-[var(--color-text-secondary)]">
                  {state.code}
                </p>
                <GlassBadge variant={state.status === "ACTIVE" ? "success" : "warning"}>
                  {state.status}
                </GlassBadge>
              </div>
              <p className="mt-3 text-3xl font-semibold text-[var(--color-gold)]">
                {formatPrice(state.balance ?? 0)}
              </p>
              <p className="mt-1 flex items-center gap-1 text-xs text-[var(--color-text-muted)]">
                <Check className="h-3.5 w-3.5" aria-hidden="true" />
                Remaining balance{state.demo ? " (sample)" : ""}
              </p>
            </div>
          )}
        </GlassCard>
      </div>
    </div>
  );
}
