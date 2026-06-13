"use client";

import * as React from "react";
import { GlassCard, GlassBadge } from "@/components/ui";
import { updateStockAction } from "@/app/actions/admin-products";
import { AlertTriangle } from "lucide-react";

interface VariantRow {
  id: string;
  productId: string;
  productName: string;
  sku: string;
  color: string | null;
  size: string | null;
  fabric: string | null;
  stock: number;
  lowStockThreshold: number;
}

export default function AdminInventoryPage() {
  const [variants, setVariants] = React.useState<VariantRow[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [editing, setEditing] = React.useState<Record<string, number>>({});
  const [saving, setSaving] = React.useState<string | null>(null);
  const [message, setMessage] = React.useState("");

  React.useEffect(() => {
    fetch("/api/admin/inventory")
      .then((r) => r.json())
      .then((data) => { setVariants(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  async function saveStock(variantId: string) {
    const newStock = editing[variantId];
    if (newStock === undefined) return;
    setSaving(variantId);
    const result = await updateStockAction(variantId, newStock);
    setSaving(null);
    if (result.ok) {
      setVariants((prev) => prev.map((v) => v.id === variantId ? { ...v, stock: newStock } : v));
      setEditing((prev) => { const n = { ...prev }; delete n[variantId]; return n; });
      setMessage("Stock updated");
      setTimeout(() => setMessage(""), 2000);
    }
  }

  const lowStock = variants.filter((v) => v.stock > 0 && v.stock <= v.lowStockThreshold);
  const outOfStock = variants.filter((v) => v.stock === 0);

  return (
    <div className="flex flex-col gap-6">
      <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Inventory</h2>

      {message && (
        <p className="text-sm text-[var(--color-success)]" role="status">{message}</p>
      )}

      {/* Summary */}
      <div className="grid gap-3 sm:grid-cols-3">
        {[
          { label: "Total variants", value: variants.length, color: "gold" },
          { label: "Low stock", value: lowStock.length, color: "warning" },
          { label: "Out of stock", value: outOfStock.length, color: "error" },
        ].map((kpi) => (
          <GlassCard key={kpi.label} padding="md">
            <p className="text-2xl font-bold text-[var(--color-text-primary)]">{kpi.value}</p>
            <p className="text-xs text-[var(--color-text-muted)] mt-0.5">{kpi.label}</p>
          </GlassCard>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="h-8 w-8 rounded-full border-2 border-[var(--color-gold)] border-t-transparent animate-spin" />
        </div>
      ) : (
        <GlassCard padding="md">
          <div className="overflow-x-auto">
            <table className="w-full text-sm" aria-label="Inventory table">
              <thead>
                <tr className="border-b border-[var(--color-glass-border)]">
                  {["Product", "Variant", "SKU", "Stock", ""].map((h) => (
                    <th key={h} scope="col" className="pb-2 pr-4 last:pr-0 text-left text-xs font-medium text-[var(--color-text-muted)]">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {variants.map((v) => {
                  const isLow = v.stock > 0 && v.stock <= v.lowStockThreshold;
                  const isOut = v.stock === 0;
                  const edited = editing[v.id] !== undefined;
                  return (
                    <tr key={v.id} className="border-b border-[var(--color-glass-border)] last:border-0">
                      <td className="py-3 pr-4 text-[var(--color-text-primary)] font-medium max-w-[160px] truncate">
                        {v.productName}
                      </td>
                      <td className="py-3 pr-4 text-[var(--color-text-secondary)]">
                        {[v.color, v.size, v.fabric].filter(Boolean).join(" / ") || "Default"}
                      </td>
                      <td className="py-3 pr-4 text-[var(--color-text-muted)] font-mono text-xs">
                        {v.sku}
                      </td>
                      <td className="py-3 pr-4">
                        <div className="flex items-center gap-2">
                          <input
                            type="number"
                            min={0}
                            value={editing[v.id] ?? v.stock}
                            onChange={(e) => setEditing((prev) => ({ ...prev, [v.id]: parseInt(e.target.value) || 0 }))}
                            className="glass-input w-20 text-sm"
                            aria-label={`Stock for ${v.sku}`}
                          />
                          {isOut && <GlassBadge variant="error">Out</GlassBadge>}
                          {isLow && !isOut && (
                            <AlertTriangle className="h-4 w-4 text-[var(--color-warning)]" aria-label="Low stock" />
                          )}
                        </div>
                      </td>
                      <td className="py-3">
                        {edited && (
                          <button
                            type="button"
                            onClick={() => saveStock(v.id)}
                            disabled={saving === v.id}
                            className="text-xs text-[var(--color-gold)] hover:underline disabled:opacity-50"
                          >
                            {saving === v.id ? "Saving…" : "Save"}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {variants.length === 0 && (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                      No variants found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </GlassCard>
      )}
    </div>
  );
}
