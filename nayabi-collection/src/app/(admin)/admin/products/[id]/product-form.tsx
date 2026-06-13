"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput } from "@/components/ui";
import { saveProductAction, saveVariantAction, deleteVariantAction } from "@/app/actions/admin-products";
import { formatPrice } from "@/lib/utils";
import { Plus, Trash2 } from "lucide-react";

interface Category { id: string; name: string }

interface Variant {
  id: string;
  sku: string;
  color: string | null;
  size: string | null;
  fabric: string | null;
  stock: number;
  lowStockThreshold: number;
  price: number | null;
  isActive: boolean;
}

interface ProductFormProps {
  product?: {
    id: string;
    name: string;
    slug: string;
    description: string;
    categoryId: string;
    price: number;
    comparePrice: number | null;
    seoTitle: string | null;
    seoDescription: string | null;
    isActive: boolean;
    tags: string[];
    images: string[];
    variants: Variant[];
  };
  categories: Category[];
}

interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

export function ProductForm({ product, categories }: ProductFormProps) {
  const [productState, productAction, productPending] = React.useActionState<ActionState, FormData>(
    saveProductAction,
    {}
  );
  const [variantState, variantAction, variantPending] = React.useActionState<ActionState, FormData>(
    saveVariantAction,
    {}
  );
  const [addingVariant, setAddingVariant] = React.useState(false);

  async function handleDeleteVariant(variantId: string) {
    if (!product?.id) return;
    if (!confirm("Delete this variant?")) return;
    await deleteVariantAction(variantId, product.id);
  }

  const [lastVState, setLastVState] = React.useState(variantState);
  if (variantState !== lastVState) {
    setLastVState(variantState);
    if (variantState.ok) setAddingVariant(false);
  }

  return (
    <div className="flex flex-col gap-6 max-w-3xl">
      {/* Product form */}
      <GlassCard padding="md">
        <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
          {product ? "Edit product" : "New product"}
        </h3>
        <form action={productAction} className="flex flex-col gap-4">
          {product && <input type="hidden" name="productId" value={product.id} />}
          <div className="grid gap-4 sm:grid-cols-2">
            <GlassInput
              label="Product name" name="name" required
              defaultValue={product?.name ?? ""}
              error={productState.errors?.name}
            />
            <GlassInput
              label="Slug (auto-generated if blank)" name="slug"
              defaultValue={product?.slug ?? ""}
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <label htmlFor="description" className="text-sm font-medium text-[var(--color-text-primary)]">
              Description <span className="text-[var(--color-error)]" aria-hidden="true">*</span>
            </label>
            <textarea
              id="description"
              name="description"
              required
              rows={5}
              defaultValue={product?.description ?? ""}
              className="glass-input resize-none"
            />
            {productState.errors?.description && (
              <p className="text-xs text-[var(--color-error)]">{productState.errors.description}</p>
            )}
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="categoryId" className="text-sm font-medium text-[var(--color-text-primary)]">
                Category <span className="text-[var(--color-error)]" aria-hidden="true">*</span>
              </label>
              <select
                id="categoryId"
                name="categoryId"
                required
                defaultValue={product?.categoryId ?? ""}
                className="glass-input"
              >
                <option value="">Select category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
            </div>
            <GlassInput
              label="Base price (₹ paise)" name="price" type="number" required min={0}
              defaultValue={product?.price ?? ""}
              hint="Enter in paise: ₹999 = 99900"
              error={productState.errors?.price}
            />
            <GlassInput
              label="Compare price (₹ paise, optional)" name="comparePrice" type="number" min={0}
              defaultValue={product?.comparePrice ?? ""}
            />
          </div>

          <GlassInput
            label="Images (one URL per line)" name="images"
            defaultValue={product?.images.join("\n") ?? ""}
          />
          <GlassInput
            label="Tags (comma separated)" name="tags"
            defaultValue={product?.tags.join(", ") ?? ""}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <GlassInput
              label="SEO title (optional)" name="seoTitle"
              defaultValue={product?.seoTitle ?? ""}
            />
            <GlassInput
              label="SEO description (optional)" name="seoDescription"
              defaultValue={product?.seoDescription ?? ""}
            />
          </div>

          <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] cursor-pointer">
            <input
              type="checkbox"
              name="isActive"
              defaultChecked={product?.isActive ?? true}
              className="h-4 w-4 rounded accent-[var(--color-gold)]"
            />
            Active (visible on store)
          </label>

          {productState.message && (
            <p className={`text-sm ${productState.ok ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
              {productState.message}
            </p>
          )}

          <GlassButton type="submit" size="sm" loading={productPending} className="self-start">
            {product ? "Update product" : "Create product"}
          </GlassButton>
        </form>
      </GlassCard>

      {/* Variants (only when editing existing product) */}
      {product && (
        <GlassCard padding="md">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
              Variants ({product.variants.length})
            </h3>
            <GlassButton type="button" size="sm" variant="secondary" onClick={() => setAddingVariant(true)}>
              <Plus className="h-4 w-4 mr-1" aria-hidden="true" />
              Add variant
            </GlassButton>
          </div>

          <ul className="flex flex-col divide-y divide-[var(--color-glass-border)]" role="list">
            {product.variants.map((v) => (
              <li key={v.id} className="py-3 flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-[var(--color-text-primary)]">
                    {[v.color, v.size, v.fabric].filter(Boolean).join(" / ") || "Default"}
                  </p>
                  <p className="text-xs text-[var(--color-text-muted)]">
                    SKU: {v.sku} · Stock: {v.stock}
                    {v.price ? ` · ${formatPrice(v.price)}` : ""}
                    {!v.isActive ? " · Inactive" : ""}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleDeleteVariant(v.id)}
                  aria-label={`Delete variant ${v.sku}`}
                  className="p-1.5 text-[var(--color-text-muted)] hover:text-[var(--color-error)] transition-colors"
                >
                  <Trash2 className="h-4 w-4" aria-hidden="true" />
                </button>
              </li>
            ))}
            {product.variants.length === 0 && (
              <li className="py-4 text-sm text-[var(--color-text-muted)] text-center">
                No variants yet
              </li>
            )}
          </ul>

          {addingVariant && (
            <div className="mt-4 border-t border-[var(--color-glass-border)] pt-4">
              <h4 className="text-xs font-semibold text-[var(--color-text-muted)] mb-3 uppercase tracking-wide">
                New variant
              </h4>
              <form action={variantAction} className="flex flex-col gap-3">
                <input type="hidden" name="productId" value={product.id} />
                <div className="grid gap-3 sm:grid-cols-3">
                  <GlassInput label="Color (optional)" name="color" />
                  <GlassInput label="Size (optional)" name="size" />
                  <GlassInput label="Fabric (optional)" name="fabric" />
                </div>
                <div className="grid gap-3 sm:grid-cols-3">
                  <GlassInput label="SKU" name="sku" required error={variantState.errors?.sku} />
                  <GlassInput label="Stock" name="stock" type="number" min={0} defaultValue="0" required />
                  <GlassInput label="Price override (paise, optional)" name="price" type="number" min={0} />
                </div>
                {variantState.message && !variantState.ok && (
                  <p className="text-sm text-[var(--color-error)]">{variantState.message}</p>
                )}
                <div className="flex gap-3">
                  <GlassButton type="submit" size="sm" loading={variantPending}>Add</GlassButton>
                  <GlassButton type="button" size="sm" variant="ghost" onClick={() => setAddingVariant(false)}>
                    Cancel
                  </GlassButton>
                </div>
              </form>
            </div>
          )}
        </GlassCard>
      )}
    </div>
  );
}
