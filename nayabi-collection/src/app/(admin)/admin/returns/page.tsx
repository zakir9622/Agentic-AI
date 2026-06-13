"use client";

import * as React from "react";
import { GlassCard, GlassBadge, GlassButton, GlassInput } from "@/components/ui";
import { processReturnAction } from "@/app/actions/admin-orders";

interface ReturnItem {
  id: string;
  orderId: string;
  orderNumber: string;
  type: string;
  status: string;
  createdAt: string;
  userName: string;
  userEmail: string;
  refundMethod: string | null;
  items: {
    id: string;
    productName: string;
    variantDetails: string;
    quantity: number;
    reason: string;
  }[];
}

const statusVariant: Record<string, "neutral" | "info" | "gold" | "success" | "error" | "warning"> = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "error",
  PICKUP_SCHEDULED: "info",
  RECEIVED: "info",
  REFUNDED: "gold",
  EXCHANGED: "gold",
};

export default function AdminReturnsPage() {
  const [returns, setReturns] = React.useState<ReturnItem[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [statusFilter, setStatusFilter] = React.useState("PENDING");
  const [processState, processAction, processPending] = React.useActionState<
    { ok?: boolean; message?: string },
    FormData
  >(processReturnAction, { ok: undefined, message: undefined });
  const [selected, setSelected] = React.useState<ReturnItem | null>(null);
  const [adminNote, setAdminNote] = React.useState("");

  React.useEffect(() => {
    let active = true;
    fetch(`/api/admin/returns?status=${statusFilter}`)
      .then((r) => r.json())
      .then((data) => { if (active) { setReturns(data); setLoading(false); } })
      .catch(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [statusFilter, processState]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">
          Returns & Exchanges
        </h2>
        <div className="flex gap-2">
          {["PENDING", "APPROVED", "REJECTED", "ALL"].map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1.5 text-xs rounded-lg border transition-colors ${
                statusFilter === s
                  ? "border-[var(--color-gold)] text-[var(--color-gold)] bg-[var(--color-gold)]/10"
                  : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)]"
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {processState?.message && (
        <div className={`rounded-lg px-4 py-2 text-sm ${processState.ok ? "bg-[var(--color-success)]/10 text-[var(--color-success)]" : "bg-[var(--color-error)]/10 text-[var(--color-error)]"}`}>
          {processState.message}
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="h-8 w-8 rounded-full border-2 border-[var(--color-gold)] border-t-transparent animate-spin" />
        </div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-[1fr_360px]">
          {/* List */}
          <GlassCard padding="md">
            <ul className="flex flex-col divide-y divide-[var(--color-glass-border)]" role="list">
              {returns.map((ret) => (
                <li key={ret.id}>
                  <button
                    type="button"
                    onClick={() => { setSelected(ret); setAdminNote(""); }}
                    className={`w-full text-left py-4 px-2 rounded-lg transition-colors ${
                      selected?.id === ret.id ? "bg-[var(--color-gold)]/10" : "hover:bg-[var(--color-glass-bg)]"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium text-[var(--color-gold)]">
                            {ret.orderNumber}
                          </span>
                          <GlassBadge variant={statusVariant[ret.status]}>{ret.status}</GlassBadge>
                          <span className="text-xs text-[var(--color-text-muted)]">
                            {ret.type}
                          </span>
                        </div>
                        <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
                          {ret.userName} · {ret.userEmail}
                        </p>
                        <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                          {new Date(ret.createdAt).toLocaleDateString("en-IN")}
                          {ret.refundMethod && ` · Refund: ${ret.refundMethod}`}
                        </p>
                      </div>
                    </div>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {ret.items.map((item) => (
                        <span key={item.id} className="text-xs text-[var(--color-text-secondary)]">
                          {item.productName} (×{item.quantity})
                        </span>
                      ))}
                    </div>
                  </button>
                </li>
              ))}
              {returns.length === 0 && (
                <li className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                  No return requests found
                </li>
              )}
            </ul>
          </GlassCard>

          {/* Detail panel */}
          {selected && (
            <GlassCard padding="md" className="self-start">
              <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
                Return #{selected.orderNumber}
              </h3>
              <div className="space-y-3 mb-4">
                {selected.items.map((item) => (
                  <div key={item.id} className="text-sm">
                    <p className="font-medium text-[var(--color-text-primary)]">
                      {item.productName}
                    </p>
                    <p className="text-xs text-[var(--color-text-secondary)]">
                      {item.variantDetails} · Qty: {item.quantity}
                    </p>
                    <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                      Reason: {item.reason}
                    </p>
                  </div>
                ))}
              </div>

              {selected.status === "PENDING" && (
                <div className="flex flex-col gap-3">
                  <GlassInput
                    label="Admin note (optional)"
                    value={adminNote}
                    onChange={(e) => setAdminNote(e.target.value)}
                  />
                  <div className="flex gap-2">
                    <form action={processAction} className="flex-1">
                      <input type="hidden" name="returnId" value={selected.id} />
                      <input type="hidden" name="action" value="APPROVED" />
                      <input type="hidden" name="adminNote" value={adminNote} />
                      <GlassButton type="submit" size="sm" fullWidth loading={processPending}>
                        Approve
                      </GlassButton>
                    </form>
                    <form action={processAction} className="flex-1">
                      <input type="hidden" name="returnId" value={selected.id} />
                      <input type="hidden" name="action" value="REJECTED" />
                      <input type="hidden" name="adminNote" value={adminNote} />
                      <GlassButton type="submit" variant="danger" size="sm" fullWidth loading={processPending}>
                        Reject
                      </GlassButton>
                    </form>
                  </div>
                </div>
              )}
            </GlassCard>
          )}
        </div>
      )}
    </div>
  );
}
