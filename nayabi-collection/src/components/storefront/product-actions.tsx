"use client";

import * as React from "react";
import { Minus, Plus, ShoppingBag, Bell } from "lucide-react";
import { GlassButton, GlassBadge, toast } from "@/components/ui";
import { useCart } from "@/store/cart";
import { cn, formatPrice } from "@/lib/utils";

export interface VariantData {
  id: string;
  color: string | null;
  size: string | null;
  fabric: string | null;
  stock: number;
  price: number | null;
  images: string[];
}

interface ProductActionsProps {
  productId: string;
  productName: string;
  basePrice: number;
  baseImage: string;
  variants: VariantData[];
}

export function ProductActions({
  productId,
  productName,
  basePrice,
  baseImage,
  variants,
}: ProductActionsProps) {
  const colors = [...new Set(variants.map((v) => v.color).filter(Boolean))] as string[];
  const sizes = [...new Set(variants.map((v) => v.size).filter(Boolean))] as string[];

  const [color, setColor] = React.useState<string | null>(colors[0] ?? null);
  const [size, setSize] = React.useState<string | null>(sizes[0] ?? null);
  const [qty, setQty] = React.useState(1);
  const addItem = useCart((s) => s.addItem);

  const selected =
    variants.find(
      (v) => (color === null || v.color === color) && (size === null || v.size === size)
    ) ?? variants[0];

  const price = selected?.price ?? basePrice;
  const stock = selected?.stock ?? 0;
  const outOfStock = stock === 0;

  function handleAdd() {
    if (!selected || outOfStock) return;
    addItem({
      variantId: selected.id,
      productId,
      productName,
      variantLabel: [selected.color, selected.size, selected.fabric].filter(Boolean).join(" · "),
      imageUrl: selected.images[0] ?? baseImage,
      price,
      quantity: qty,
      maxStock: stock,
    });
    toast.success("Added to bag", `${productName} × ${qty}`);
  }

  return (
    <div className="flex flex-col gap-5">
      {/* Price */}
      <p className="text-2xl font-semibold text-[var(--color-gold)]">{formatPrice(price)}</p>

      {/* Stock status */}
      <div aria-live="polite">
        {outOfStock ? (
          <GlassBadge variant="error">Out of stock</GlassBadge>
        ) : stock <= 5 ? (
          <GlassBadge variant="warning">Only {stock} left</GlassBadge>
        ) : (
          <GlassBadge variant="success">In stock</GlassBadge>
        )}
      </div>

      {/* Colour selector */}
      {colors.length > 0 && (
        <fieldset>
          <legend className="mb-2 text-sm font-medium text-[var(--color-text-primary)]">
            Colour: <span className="text-[var(--color-text-secondary)]">{color}</span>
          </legend>
          <div className="flex flex-wrap gap-2">
            {colors.map((c) => (
              <button
                key={c}
                onClick={() => setColor(c)}
                aria-pressed={color === c}
                className={cn(
                  "rounded-full border px-4 py-1.5 text-sm transition-colors",
                  color === c
                    ? "border-[var(--color-gold)] bg-[rgba(201,169,110,0.15)] text-[var(--color-gold)]"
                    : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-gold)]"
                )}
              >
                {c}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      {/* Size selector */}
      {sizes.length > 0 && (
        <fieldset>
          <legend className="mb-2 text-sm font-medium text-[var(--color-text-primary)]">
            Size: <span className="text-[var(--color-text-secondary)]">{size}</span>
          </legend>
          <div className="flex flex-wrap gap-2">
            {sizes.map((s) => (
              <button
                key={s}
                onClick={() => setSize(s)}
                aria-pressed={size === s}
                className={cn(
                  "min-w-12 rounded-lg border px-3 py-1.5 text-sm transition-colors",
                  size === s
                    ? "border-[var(--color-gold)] bg-[rgba(201,169,110,0.15)] text-[var(--color-gold)]"
                    : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-gold)]"
                )}
              >
                {s}
              </button>
            ))}
          </div>
        </fieldset>
      )}

      {/* Quantity + Add */}
      {outOfStock ? (
        <BackInStockForm variantId={selected?.id} />
      ) : (
        <div className="flex gap-3">
          <div className="glass glass-sm flex items-center !rounded-xl">
            <button
              onClick={() => setQty((q) => Math.max(1, q - 1))}
              aria-label="Decrease quantity"
              className="px-3 py-2.5 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
            >
              <Minus className="h-4 w-4" aria-hidden="true" />
            </button>
            <span className="min-w-8 text-center font-medium" aria-live="polite">{qty}</span>
            <button
              onClick={() => setQty((q) => Math.min(stock, q + 1))}
              aria-label="Increase quantity"
              className="px-3 py-2.5 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
            >
              <Plus className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
          <GlassButton
            size="lg"
            fullWidth
            onClick={handleAdd}
            icon={<ShoppingBag className="h-4 w-4" aria-hidden="true" />}
          >
            Add to Bag
          </GlassButton>
        </div>
      )}
    </div>
  );
}

function BackInStockForm({ variantId }: { variantId?: string }) {
  const [email, setEmail] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [done, setDone] = React.useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!variantId || !email) return;
    setSubmitting(true);
    try {
      const res = await fetch("/api/back-in-stock", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ variantId, email }),
      });
      if (res.ok) {
        setDone(true);
        toast.success("You're on the list", "We'll notify you when it's back.");
      } else {
        toast.error("Something went wrong", "Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (done)
    return (
      <p className="text-sm text-[var(--color-success)]" role="status">
        ✓ We&apos;ll email you the moment this is back in stock.
      </p>
    );

  return (
    <form onSubmit={submit} className="flex flex-col gap-2">
      <label htmlFor="bis-email" className="text-sm font-medium text-[var(--color-text-primary)]">
        Notify me when back in stock
      </label>
      <div className="flex gap-2">
        <input
          id="bis-email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          className="glass-input"
        />
        <GlassButton
          type="submit"
          variant="secondary"
          loading={submitting}
          icon={<Bell className="h-4 w-4" aria-hidden="true" />}
        >
          Notify
        </GlassButton>
      </div>
    </form>
  );
}
