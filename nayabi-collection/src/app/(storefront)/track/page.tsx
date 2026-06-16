"use client";

import * as React from "react";
import Image from "next/image";
import {
  Package,
  Truck,
  CheckCircle2,
  Clock,
  MapPin,
  Search,
  ExternalLink,
} from "lucide-react";
import { GlassCard, GlassButton, GlassBadge } from "@/components/ui";
import { lookupOrder, type TrackResult } from "@/app/actions/track";
import { formatPrice } from "@/lib/utils";

const initialState: TrackResult = { found: false };

const statusTone: Record<string, "success" | "warning" | "info" | "error" | "gold"> = {
  PENDING: "warning",
  CONFIRMED: "info",
  PROCESSING: "info",
  SHIPPED: "gold",
  DELIVERED: "success",
  CANCELLED: "error",
  RETURNED: "warning",
  REFUNDED: "warning",
};

function prettify(s: string) {
  return s
    .toLowerCase()
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

export default function TrackOrderPage() {
  const [state, formAction, pending] = React.useActionState(lookupOrder, initialState);

  return (
    <div className="mesh-bg min-h-screen">
      <div className="relative z-10 mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-20 lg:px-8">
        <div className="text-center">
          <p className="section-label">Order status</p>
          <h1 className="mt-3 text-4xl text-[var(--color-text-primary)] sm:text-5xl">
            Track your order
          </h1>
          <p className="mt-4 text-[var(--color-text-secondary)]">
            Enter your order number and the email you used at checkout to see live status
            and shipment updates.
          </p>
        </div>

        {/* Lookup form */}
        <GlassCard tier="elevated" padding="lg" className="mt-10">
          <form action={formAction} className="flex flex-col gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label
                  htmlFor="orderNumber"
                  className="mb-1.5 block text-sm font-medium text-[var(--color-text-primary)]"
                >
                  Order number
                </label>
                <input
                  id="orderNumber"
                  name="orderNumber"
                  required
                  placeholder="NAYABI-XXXXXX"
                  className="glass-input"
                  autoComplete="off"
                />
              </div>
              <div>
                <label
                  htmlFor="email"
                  className="mb-1.5 block text-sm font-medium text-[var(--color-text-primary)]"
                >
                  Email
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  required
                  placeholder="you@example.com"
                  className="glass-input"
                  autoComplete="email"
                />
              </div>
            </div>
            <GlassButton
              type="submit"
              loading={pending}
              icon={<Search className="h-4 w-4" aria-hidden="true" />}
            >
              Track order
            </GlassButton>
            <p className="text-center text-xs text-[var(--color-text-muted)]">
              Tip: enter order number{" "}
              <span className="text-[var(--color-gold)]">NAYABI-DEMO</span> with any email
              to preview a sample tracking timeline.
            </p>
          </form>
        </GlassCard>

        {/* Error */}
        {!pending && state.error && (
          <GlassCard padding="md" className="mt-6 border-[var(--color-error)]/30">
            <p className="text-sm text-[var(--color-error)]">{state.error}</p>
          </GlassCard>
        )}

        {/* Result */}
        {!pending && state.found && state.order && (
          <div className="mt-8 flex flex-col gap-6">
            {state.demo && (
              <p className="text-center text-xs text-[var(--color-text-muted)]">
                Showing a sample order for preview.
              </p>
            )}

            {/* Summary */}
            <GlassCard tier="elevated" padding="lg">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-xs text-[var(--color-text-muted)]">Order</p>
                  <p className="font-display text-2xl text-[var(--color-text-primary)]">
                    {state.order.orderNumber}
                  </p>
                  <p className="mt-1 text-xs text-[var(--color-text-muted)]">
                    Placed{" "}
                    {new Date(state.order.placedAt).toLocaleDateString("en-IN", {
                      day: "numeric",
                      month: "long",
                      year: "numeric",
                    })}
                  </p>
                </div>
                <GlassBadge variant={statusTone[state.order.orderStatus] ?? "info"}>
                  {prettify(state.order.orderStatus)}
                </GlassBadge>
              </div>

              <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3">
                <Stat label="Total" value={formatPrice(state.order.total)} />
                <Stat label="Payment" value={prettify(state.order.paymentMethod)} />
                <Stat
                  label="Fulfillment"
                  value={prettify(state.order.fulfillmentStatus)}
                />
              </div>
            </GlassCard>

            {/* Items */}
            <GlassCard padding="md">
              <h2 className="text-base !font-[var(--font-body)] font-semibold text-[var(--color-text-primary)]">
                Items
              </h2>
              <ul className="mt-4 flex flex-col gap-4" role="list">
                {state.order.items.map((item, i) => (
                  <li key={i} className="flex items-center gap-4">
                    <div className="relative h-16 w-14 shrink-0 overflow-hidden rounded-lg bg-[var(--color-bg-mid)]">
                      {item.image && (
                        <Image
                          src={item.image}
                          alt={item.name}
                          fill
                          sizes="56px"
                          className="object-cover"
                        />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">
                        {item.name}
                      </p>
                      <p className="truncate text-xs text-[var(--color-text-muted)]">
                        {item.variant}
                      </p>
                    </div>
                    <p className="text-sm text-[var(--color-text-secondary)]">
                      ×{item.quantity}
                    </p>
                  </li>
                ))}
              </ul>
            </GlassCard>

            {/* Shipment timeline */}
            <GlassCard padding="md">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="flex items-center gap-2 text-base !font-[var(--font-body)] font-semibold text-[var(--color-text-primary)]">
                  <Truck
                    className="h-4 w-4 text-[var(--color-gold)]"
                    aria-hidden="true"
                  />
                  Shipment
                </h2>
                {state.order.shipment?.trackingUrl && (
                  <a
                    href={state.order.shipment.trackingUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-sm text-[var(--color-gold)] underline-offset-4 hover:underline"
                  >
                    Track on courier site
                    <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                  </a>
                )}
              </div>

              {state.order.shipment ? (
                <>
                  <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm text-[var(--color-text-secondary)]">
                    {state.order.shipment.carrier && (
                      <span>
                        Carrier:{" "}
                        <span className="text-[var(--color-text-primary)]">
                          {state.order.shipment.carrier}
                        </span>
                      </span>
                    )}
                    {state.order.shipment.trackingNumber && (
                      <span>
                        AWB:{" "}
                        <span className="text-[var(--color-text-primary)]">
                          {state.order.shipment.trackingNumber}
                        </span>
                      </span>
                    )}
                  </div>

                  {state.order.shipment.events.length > 0 ? (
                    <ol className="mt-6 flex flex-col gap-0" role="list">
                      {state.order.shipment.events.map((ev, i) => {
                        const isLatest = i === 0;
                        return (
                          <li key={i} className="flex gap-4">
                            <div className="flex flex-col items-center">
                              <span
                                className={
                                  isLatest
                                    ? "flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-gold)]/15 text-[var(--color-gold)] ring-2 ring-[var(--color-gold)]/30"
                                    : "flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-glass-bg)] text-[var(--color-text-muted)]"
                                }
                              >
                                {isLatest ? (
                                  <MapPin className="h-4 w-4" aria-hidden="true" />
                                ) : (
                                  <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                                )}
                              </span>
                              {i < state.order!.shipment!.events.length - 1 && (
                                <span className="my-1 w-px flex-1 bg-[var(--color-glass-border)]" />
                              )}
                            </div>
                            <div className="pb-6">
                              <p className="text-sm font-medium text-[var(--color-text-primary)]">
                                {prettify(ev.status)}
                              </p>
                              {ev.message && (
                                <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                                  {ev.message}
                                </p>
                              )}
                              <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">
                                {ev.location ? `${ev.location} · ` : ""}
                                {new Date(ev.timestamp).toLocaleString("en-IN", {
                                  day: "numeric",
                                  month: "short",
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}
                              </p>
                            </div>
                          </li>
                        );
                      })}
                    </ol>
                  ) : (
                    <p className="mt-4 flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
                      <Clock className="h-4 w-4" aria-hidden="true" />
                      Awaiting first tracking update.
                    </p>
                  )}
                </>
              ) : (
                <p className="mt-4 flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
                  <Package className="h-4 w-4" aria-hidden="true" />
                  Your order is being prepared. Tracking details will appear here once it
                  ships.
                </p>
              )}
            </GlassCard>
          </div>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-[var(--color-text-muted)]">{label}</p>
      <p className="mt-0.5 text-sm font-medium text-[var(--color-text-primary)]">
        {value}
      </p>
    </div>
  );
}
