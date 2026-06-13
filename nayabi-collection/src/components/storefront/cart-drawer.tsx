"use client";

import * as React from "react";
import Link from "next/link";
import Image from "next/image";
import { X, Minus, Plus, Trash2, ShoppingBag } from "lucide-react";
import { useCart } from "@/store/cart";
import { GlassButton } from "@/components/ui";
import { formatPrice } from "@/lib/utils";

const FREE_SHIPPING_THRESHOLD = 99900; // ₹999 in paise

export function CartDrawer() {
  const { items, isOpen, close, setQuantity, removeItem } = useCart();
  const subtotal = items.reduce((s, i) => s + i.price * i.quantity, 0);
  const remaining = FREE_SHIPPING_THRESHOLD - subtotal;
  const panelRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && close();
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    panelRef.current?.querySelector<HTMLElement>("button")?.focus();
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [isOpen, close]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label="Shopping bag">
      <div
        className="absolute inset-0 bg-black/60"
        style={{ backdropFilter: "blur(6px)", WebkitBackdropFilter: "blur(6px)" }}
        onClick={close}
        aria-hidden="true"
      />

      <div
        ref={panelRef}
        className="glass glass-elevated !rounded-none absolute right-0 top-0 flex h-full w-full max-w-md flex-col border-y-0 border-r-0 animate-[slideIn_300ms_cubic-bezier(0.4,0,0.2,1)]"
      >
        <style>{`@keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }`}</style>

        <div className="flex items-center justify-between border-b border-[var(--color-glass-border)] p-5">
          <h2 className="font-display text-lg text-[var(--color-text-primary)]">
            Your Bag ({items.length})
          </h2>
          <button
            onClick={close}
            aria-label="Close shopping bag"
            className="p-1.5 rounded-lg text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-glass-bg)] transition-colors"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        {items.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-4 p-8 text-center">
            <ShoppingBag className="h-12 w-12 text-[var(--color-text-muted)]" aria-hidden="true" />
            <p className="text-[var(--color-text-secondary)]">Your bag is empty</p>
            <GlassButton onClick={close} variant="secondary" size="sm">
              Continue shopping
            </GlassButton>
          </div>
        ) : (
          <>
            {/* Free shipping progress */}
            <div className="border-b border-[var(--color-glass-border)] px-5 py-3">
              {remaining > 0 ? (
                <p className="text-xs text-[var(--color-text-secondary)]">
                  Add <strong className="text-[var(--color-gold)]">{formatPrice(remaining)}</strong>{" "}
                  more for free shipping
                </p>
              ) : (
                <p className="text-xs text-[var(--color-success)]">
                  ✓ You&apos;ve unlocked free shipping
                </p>
              )}
              <div
                className="mt-2 h-1 overflow-hidden rounded-full bg-[var(--color-glass-bg)]"
                role="progressbar"
                aria-valuenow={Math.min(100, Math.round((subtotal / FREE_SHIPPING_THRESHOLD) * 100))}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label="Progress towards free shipping"
              >
                <div
                  className="h-full bg-[var(--color-gold)] transition-all duration-300"
                  style={{ width: `${Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100)}%` }}
                />
              </div>
            </div>

            {/* Items */}
            <ul className="flex-1 divide-y divide-[var(--color-glass-border)] overflow-y-auto" role="list">
              {items.map((item) => (
                <li key={item.variantId} className="flex gap-4 p-5">
                  <div className="relative h-24 w-20 shrink-0 overflow-hidden rounded-lg bg-[var(--color-bg-mid)]">
                    {item.imageUrl && (
                      <Image src={item.imageUrl} alt={item.productName} fill sizes="80px" className="object-cover" />
                    )}
                  </div>
                  <div className="flex flex-1 flex-col">
                    <p className="text-sm font-medium text-[var(--color-text-primary)]">
                      {item.productName}
                    </p>
                    <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">{item.variantLabel}</p>
                    <div className="mt-auto flex items-center justify-between pt-2">
                      <div className="glass glass-sm flex items-center !rounded-lg">
                        <button
                          onClick={() => setQuantity(item.variantId, item.quantity - 1)}
                          aria-label={`Decrease quantity of ${item.productName}`}
                          className="px-2 py-1 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                        >
                          <Minus className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                        <span className="min-w-6 text-center text-sm" aria-live="polite">
                          {item.quantity}
                        </span>
                        <button
                          onClick={() => setQuantity(item.variantId, item.quantity + 1)}
                          disabled={item.quantity >= item.maxStock}
                          aria-label={`Increase quantity of ${item.productName}`}
                          className="px-2 py-1 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] disabled:opacity-40"
                        >
                          <Plus className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                      </div>
                      <p className="text-sm font-semibold text-[var(--color-gold)]">
                        {formatPrice(item.price * item.quantity)}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => removeItem(item.variantId)}
                    aria-label={`Remove ${item.productName} from bag`}
                    className="self-start p-1 text-[var(--color-text-muted)] hover:text-[var(--color-error)] transition-colors"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </li>
              ))}
            </ul>

            {/* Footer */}
            <div className="border-t border-[var(--color-glass-border)] p-5">
              <div className="mb-4 flex items-center justify-between">
                <span className="text-sm text-[var(--color-text-secondary)]">Subtotal</span>
                <span className="text-lg font-semibold text-[var(--color-text-primary)]">
                  {formatPrice(subtotal)}
                </span>
              </div>
              <p className="mb-4 text-xs text-[var(--color-text-muted)]">
                Shipping, GST and discounts calculated at checkout
              </p>
              <Link href="/checkout" onClick={close}>
                <GlassButton fullWidth size="lg">
                  Proceed to Checkout
                </GlassButton>
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
