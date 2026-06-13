"use client";

import * as React from "react";
import Image from "next/image";
import { useRouter } from "next/navigation";
import { GlassButton, GlassInput, GlassCard } from "@/components/ui";
import { createReturnRequestAction, type ActionState } from "@/app/actions/account";
import { Minus, Plus } from "lucide-react";

interface OrderItem {
  id: string;
  variantId: string;
  productName: string;
  variantDetails: string;
  imageUrl: string | null;
  quantity: number;
  totalPrice: number;
}

interface ReturnFormProps {
  orderId: string;
  orderItems: OrderItem[];
}

interface SelectedItem {
  orderItemId: string;
  variantId: string;
  quantity: number;
  reason: string;
  maxQty: number;
}

export function ReturnForm({ orderId, orderItems }: ReturnFormProps) {
  const router = useRouter();
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    createReturnRequestAction,
    {}
  );
  const [type, setType] = React.useState<"RETURN" | "EXCHANGE">("RETURN");
  const [refundMethod, setRefundMethod] = React.useState("ORIGINAL_PAYMENT");
  const [selected, setSelected] = React.useState<Record<string, SelectedItem>>({});

  if (state.ok) {
    router.push(`/account/orders/${orderId}`);
  }

  function toggleItem(item: OrderItem) {
    setSelected((prev) => {
      if (prev[item.id]) {
        const next = { ...prev };
        delete next[item.id];
        return next;
      }
      return {
        ...prev,
        [item.id]: {
          orderItemId: item.id,
          variantId: item.variantId,
          quantity: 1,
          reason: "",
          maxQty: item.quantity,
        },
      };
    });
  }

  function updateQuantity(itemId: string, delta: number) {
    setSelected((prev) => {
      const item = prev[itemId];
      if (!item) return prev;
      const qty = Math.max(1, Math.min(item.maxQty, item.quantity + delta));
      return { ...prev, [itemId]: { ...item, quantity: qty } };
    });
  }

  function updateReason(itemId: string, reason: string) {
    setSelected((prev) => ({ ...prev, [itemId]: { ...prev[itemId], reason } }));
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const items = Object.values(selected);
    if (items.length === 0) return;

    const fd = new FormData();
    fd.set("orderId", orderId);
    fd.set("type", type);
    if (type === "RETURN") fd.set("refundMethod", refundMethod);
    fd.set("items", JSON.stringify(items));
    React.startTransition(() => formAction(fd));
  }

  const hasSelection = Object.keys(selected).length > 0;

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
      {/* Type selector */}
      <GlassCard padding="md">
        <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
          Request type
        </h3>
        <div className="flex gap-3" role="radiogroup" aria-label="Request type">
          {(["RETURN", "EXCHANGE"] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="radio"
              aria-checked={type === t}
              onClick={() => setType(t)}
              className={`flex-1 rounded-lg border px-4 py-3 text-sm font-medium transition-colors ${
                type === t
                  ? "border-[var(--color-gold)] bg-[var(--color-gold)]/10 text-[var(--color-gold)]"
                  : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-gold)]/50"
              }`}
            >
              {t === "RETURN" ? "Return & Refund" : "Exchange"}
            </button>
          ))}
        </div>
      </GlassCard>

      {/* Item selection */}
      <GlassCard padding="md">
        <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
          Select items
        </h3>
        <ul className="flex flex-col gap-4" role="list">
          {orderItems.map((item) => {
            const sel = selected[item.id];
            const isSelected = !!sel;
            return (
              <li key={item.id} className="flex flex-col gap-3">
                <button
                  type="button"
                  onClick={() => toggleItem(item)}
                  aria-pressed={isSelected}
                  className={`flex items-start gap-3 rounded-lg border p-3 text-left transition-colors w-full ${
                    isSelected
                      ? "border-[var(--color-gold)] bg-[var(--color-gold)]/5"
                      : "border-[var(--color-glass-border)] hover:border-[var(--color-gold)]/40"
                  }`}
                >
                  {item.imageUrl && (
                    <div className="relative h-14 w-14 shrink-0 overflow-hidden rounded-lg">
                      <Image
                        src={item.imageUrl}
                        alt={item.productName}
                        fill
                        sizes="56px"
                        className="object-cover"
                      />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-[var(--color-text-primary)]">
                      {item.productName}
                    </p>
                    <p className="text-xs text-[var(--color-text-muted)]">{item.variantDetails}</p>
                    <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
                      Qty ordered: {item.quantity}
                    </p>
                  </div>
                </button>

                {isSelected && (
                  <div className="ml-4 flex flex-col gap-3">
                    <div className="flex items-center gap-3">
                      <span className="text-xs text-[var(--color-text-secondary)]">
                        Return qty:
                      </span>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, -1)}
                          aria-label="Decrease quantity"
                          disabled={sel.quantity <= 1}
                          className="h-7 w-7 rounded-full border border-[var(--color-glass-border)] flex items-center justify-center text-[var(--color-text-secondary)] hover:border-[var(--color-gold)] disabled:opacity-40"
                        >
                          <Minus className="h-3 w-3" aria-hidden="true" />
                        </button>
                        <span className="text-sm w-4 text-center text-[var(--color-text-primary)]">
                          {sel.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, 1)}
                          aria-label="Increase quantity"
                          disabled={sel.quantity >= sel.maxQty}
                          className="h-7 w-7 rounded-full border border-[var(--color-glass-border)] flex items-center justify-center text-[var(--color-text-secondary)] hover:border-[var(--color-gold)] disabled:opacity-40"
                        >
                          <Plus className="h-3 w-3" aria-hidden="true" />
                        </button>
                      </div>
                    </div>
                    <GlassInput
                      label="Reason"
                      placeholder="e.g. Wrong size, defective item..."
                      value={sel.reason}
                      onChange={(e) => updateReason(item.id, e.target.value)}
                      required
                    />
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      </GlassCard>

      {/* Refund method (only for returns) */}
      {type === "RETURN" && (
        <GlassCard padding="md">
          <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
            Refund method
          </h3>
          <div className="flex flex-col gap-2" role="radiogroup" aria-label="Refund method">
            {[
              { value: "ORIGINAL_PAYMENT", label: "Original payment method" },
              { value: "STORE_CREDIT", label: "Store credit (faster)" },
              { value: "BANK_TRANSFER", label: "Bank transfer (UPI/NEFT)" },
            ].map((opt) => (
              <button
                key={opt.value}
                type="button"
                role="radio"
                aria-checked={refundMethod === opt.value}
                onClick={() => setRefundMethod(opt.value)}
                className={`flex items-center gap-3 rounded-lg border px-4 py-2.5 text-sm text-left transition-colors ${
                  refundMethod === opt.value
                    ? "border-[var(--color-gold)] bg-[var(--color-gold)]/10 text-[var(--color-gold)]"
                    : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-gold)]/50"
                }`}
              >
                <span
                  className={`h-4 w-4 rounded-full border-2 shrink-0 ${
                    refundMethod === opt.value
                      ? "border-[var(--color-gold)] bg-[var(--color-gold)]"
                      : "border-[var(--color-glass-border)]"
                  }`}
                  aria-hidden="true"
                />
                {opt.label}
              </button>
            ))}
          </div>
        </GlassCard>
      )}

      {state.message && !state.ok && (
        <p role="alert" className="text-sm text-[var(--color-error)]">
          {state.message}
        </p>
      )}
      {state.errors?.form && (
        <p role="alert" className="text-sm text-[var(--color-error)]">
          {state.errors.form}
        </p>
      )}

      <GlassButton type="submit" size="lg" fullWidth loading={pending} disabled={!hasSelection}>
        Submit {type === "RETURN" ? "Return" : "Exchange"} Request
      </GlassButton>
    </form>
  );
}
