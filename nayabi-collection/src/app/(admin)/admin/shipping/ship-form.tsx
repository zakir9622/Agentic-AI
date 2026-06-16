"use client";

import * as React from "react";
import { Truck } from "lucide-react";
import { shipOrderAction, type ShipState } from "@/app/actions/admin-shipping";

interface Carrier {
  id: string;
  label: string;
  configured: boolean;
}

const initial: ShipState = {};

export function ShipForm({
  orderId,
  carriers,
}: {
  orderId: string;
  carriers: Carrier[];
}) {
  const [state, action, pending] = React.useActionState(shipOrderAction, initial);
  const [carrier, setCarrier] = React.useState(
    carriers.find((c) => c.configured)?.id ?? "manual"
  );

  const isManual = carrier === "manual";

  return (
    <form action={action} className="flex flex-col gap-3">
      <input type="hidden" name="orderId" value={orderId} />

      <div className="flex flex-wrap items-end gap-2">
        <label className="flex flex-col gap-1 text-xs text-[var(--color-text-muted)]">
          Carrier
          <select
            name="carrier"
            value={carrier}
            onChange={(e) => setCarrier(e.target.value)}
            className="glass-input !py-1.5 text-sm"
          >
            {carriers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.label}
                {c.configured ? "" : " (not configured)"}
              </option>
            ))}
            <option value="manual">Manual entry</option>
          </select>
        </label>

        <button
          type="submit"
          disabled={pending}
          className="btn-gold flex items-center gap-1.5 rounded-lg px-4 py-2 text-sm font-semibold disabled:opacity-50"
        >
          <Truck className="h-4 w-4" aria-hidden="true" />
          {pending ? "Shipping…" : "Mark shipped"}
        </button>
      </div>

      {/* Manual / override fields */}
      {isManual && (
        <div className="grid gap-2 sm:grid-cols-3">
          <input
            name="manualCarrierName"
            placeholder="Carrier name"
            className="glass-input !py-1.5 text-sm"
          />
          <input
            name="manualAwb"
            placeholder="AWB / tracking no."
            className="glass-input !py-1.5 text-sm"
          />
          <input
            name="manualTrackingUrl"
            placeholder="Tracking URL (optional)"
            className="glass-input !py-1.5 text-sm"
          />
        </div>
      )}

      {state.message && (
        <p
          className={
            state.ok
              ? "text-xs text-[var(--color-success)]"
              : "text-xs text-[var(--color-error)]"
          }
          role="status"
        >
          {state.message}
        </p>
      )}
    </form>
  );
}
