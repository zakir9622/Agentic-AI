"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput, GlassBadge } from "@/components/ui";
import { saveDiscountAction, deleteDiscountAction } from "@/app/actions/admin-products";
import { Plus, Trash2 } from "lucide-react";
import { formatPrice } from "@/lib/utils";

interface Discount {
  id: string;
  code: string;
  type: string;
  value: number;
  minOrderAmount: number | null;
  usageLimit: number | null;
  usedCount: number;
  isFirstOrderOnly: boolean;
  isActive: boolean;
  startsAt: string | null;
  expiresAt: string | null;
}

interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

export default function AdminDiscountsPage() {
  const [discounts, setDiscounts] = React.useState<Discount[]>([]);
  const [editing, setEditing] = React.useState<Partial<Discount> | null>(null);
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    saveDiscountAction,
    {}
  );

  React.useEffect(() => {
    fetch("/api/admin/discounts")
      .then((r) => r.json())
      .then(setDiscounts)
      .catch(() => {});
  }, [state]);

  const [lastState, setLastState] = React.useState(state);
  if (state !== lastState) {
    setLastState(state);
    if (state.ok) setEditing(null);
  }

  async function handleDelete(id: string, code: string) {
    if (!confirm(`Deactivate discount code "${code}"?`)) return;
    await deleteDiscountAction(id);
    setDiscounts((prev) => prev.map((d) => d.id === id ? { ...d, isActive: false } : d));
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Discounts</h2>
        <GlassButton type="button" size="sm" onClick={() => setEditing({})}>
          <Plus className="h-4 w-4 mr-1" aria-hidden="true" />
          New discount
        </GlassButton>
      </div>

      <GlassCard padding="md">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Discounts table">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                {["Code", "Type", "Value", "Used", "Status", "Expires", ""].map((h) => (
                  <th key={h} scope="col" className="pb-2 pr-4 last:pr-0 text-left text-xs font-medium text-[var(--color-text-muted)]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {discounts.map((d) => (
                <tr key={d.id} className="border-b border-[var(--color-glass-border)] last:border-0 hover:bg-[var(--color-glass-bg)]">
                  <td className="py-3 pr-4 font-mono font-bold text-[var(--color-gold)]">{d.code}</td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)]">{d.type}</td>
                  <td className="py-3 pr-4 text-[var(--color-text-primary)]">
                    {d.type === "PERCENT" ? `${d.value}%` : d.type === "FREE_SHIPPING" ? "Free shipping" : formatPrice(d.value)}
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)]">
                    {d.usedCount}{d.usageLimit ? `/${d.usageLimit}` : ""}
                  </td>
                  <td className="py-3 pr-4">
                    <GlassBadge variant={d.isActive ? "success" : "neutral"}>
                      {d.isActive ? "Active" : "Inactive"}
                    </GlassBadge>
                  </td>
                  <td className="py-3 pr-4 text-xs text-[var(--color-text-muted)]">
                    {d.expiresAt ? new Date(d.expiresAt).toLocaleDateString("en-IN") : "—"}
                  </td>
                  <td className="py-3">
                    <button
                      type="button"
                      onClick={() => handleDelete(d.id, d.code)}
                      aria-label={`Delete ${d.code}`}
                      className="p-1.5 text-[var(--color-text-muted)] hover:text-[var(--color-error)] transition-colors"
                    >
                      <Trash2 className="h-4 w-4" aria-hidden="true" />
                    </button>
                  </td>
                </tr>
              ))}
              {discounts.length === 0 && (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                    No discount codes yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </GlassCard>

      {editing !== null && (
        <GlassCard padding="md">
          <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            {editing.id ? "Edit discount" : "New discount"}
          </h3>
          <form action={formAction} className="flex flex-col gap-4">
            {editing.id && <input type="hidden" name="discountId" value={editing.id} />}
            <div className="grid gap-4 sm:grid-cols-2">
              <GlassInput
                label="Code (UPPERCASE)" name="code" required
                defaultValue={editing.code ?? ""}
                error={state.errors?.code}
              />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[var(--color-text-primary)]">
                  Type <span className="text-[var(--color-error)]" aria-hidden="true">*</span>
                </label>
                <select name="type" required defaultValue={editing.type ?? "PERCENT"} className="glass-input">
                  <option value="PERCENT">Percentage off</option>
                  <option value="FIXED">Fixed amount (paise)</option>
                  <option value="FREE_SHIPPING">Free shipping</option>
                </select>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-3">
              <GlassInput label="Value (% or paise)" name="value" type="number" min={0} required defaultValue={editing.value ?? ""} error={state.errors?.value} />
              <GlassInput label="Min order (paise)" name="minOrderAmount" type="number" min={0} defaultValue={editing.minOrderAmount ?? ""} />
              <GlassInput label="Usage limit (total)" name="usageLimit" type="number" min={0} defaultValue={editing.usageLimit ?? ""} />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <GlassInput label="Starts at (optional)" name="startsAt" type="datetime-local" defaultValue={editing.startsAt ?? ""} />
              <GlassInput label="Expires at (optional)" name="expiresAt" type="datetime-local" defaultValue={editing.expiresAt ?? ""} />
            </div>
            <div className="flex flex-wrap gap-4">
              <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] cursor-pointer">
                <input type="checkbox" name="isFirstOrderOnly" defaultChecked={editing.isFirstOrderOnly} className="h-4 w-4 rounded accent-[var(--color-gold)]" />
                First order only
              </label>
              <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] cursor-pointer">
                <input type="checkbox" name="isActive" defaultChecked={editing.isActive ?? true} className="h-4 w-4 rounded accent-[var(--color-gold)]" />
                Active
              </label>
            </div>

            {state.message && (
              <p className={`text-sm ${state.ok ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
                {state.message}
              </p>
            )}
            <div className="flex gap-3">
              <GlassButton type="submit" size="sm" loading={pending}>Save</GlassButton>
              <GlassButton type="button" size="sm" variant="ghost" onClick={() => setEditing(null)}>Cancel</GlassButton>
            </div>
          </form>
        </GlassCard>
      )}
    </div>
  );
}
