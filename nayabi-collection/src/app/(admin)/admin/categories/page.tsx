"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput, GlassBadge } from "@/components/ui";
import { saveCategoryAction } from "@/app/actions/admin-products";
import { Pencil, Plus } from "lucide-react";

interface Category {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  isActive: boolean;
  _count: { products: number };
}

interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

export default function AdminCategoriesPage() {
  const [categories, setCategories] = React.useState<Category[]>([]);
  const [editing, setEditing] = React.useState<Partial<Category> | null>(null);
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    saveCategoryAction,
    {}
  );

  React.useEffect(() => {
    fetch("/api/admin/categories")
      .then((r) => r.json())
      .then(setCategories)
      .catch(() => {});
  }, [state]);

  const [lastState, setLastState] = React.useState(state);
  if (state !== lastState) {
    setLastState(state);
    if (state.ok) setEditing(null);
  }

  return (
    <div className="flex flex-col gap-6 max-w-2xl">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Categories</h2>
        <GlassButton type="button" size="sm" onClick={() => setEditing({})}>
          <Plus className="h-4 w-4 mr-1" aria-hidden="true" />
          New category
        </GlassButton>
      </div>

      <GlassCard padding="md">
        <ul className="flex flex-col divide-y divide-[var(--color-glass-border)]" role="list">
          {categories.map((cat) => (
            <li key={cat.id} className="flex items-center justify-between gap-4 py-3">
              <div>
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-[var(--color-text-primary)]">{cat.name}</p>
                  <GlassBadge variant={cat.isActive ? "success" : "neutral"}>
                    {cat.isActive ? "Active" : "Inactive"}
                  </GlassBadge>
                </div>
                <p className="text-xs text-[var(--color-text-muted)]">
                  /{cat.slug} · {cat._count.products} products
                </p>
              </div>
              <button
                type="button"
                onClick={() => setEditing(cat)}
                aria-label={`Edit ${cat.name}`}
                className="p-1.5 text-[var(--color-text-muted)] hover:text-[var(--color-gold)] transition-colors"
              >
                <Pencil className="h-4 w-4" aria-hidden="true" />
              </button>
            </li>
          ))}
          {categories.length === 0 && (
            <li className="py-8 text-center text-sm text-[var(--color-text-muted)]">
              No categories yet
            </li>
          )}
        </ul>
      </GlassCard>

      {editing !== null && (
        <GlassCard padding="md">
          <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            {editing.id ? "Edit category" : "New category"}
          </h3>
          <form action={formAction} className="flex flex-col gap-4">
            {editing.id && <input type="hidden" name="categoryId" value={editing.id} />}
            <GlassInput
              label="Name" name="name" required
              defaultValue={editing.name ?? ""}
              error={state.errors?.name}
            />
            <GlassInput
              label="Slug (auto-generated if blank)" name="slug"
              defaultValue={editing.slug ?? ""}
            />
            <GlassInput
              label="Description (optional)" name="description"
              defaultValue={editing.description ?? ""}
            />
            <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] cursor-pointer">
              <input
                type="checkbox"
                name="isActive"
                defaultChecked={editing.isActive ?? true}
                className="h-4 w-4 rounded accent-[var(--color-gold)]"
              />
              Active
            </label>
            {state.message && (
              <p className={`text-sm ${state.ok ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
                {state.message}
              </p>
            )}
            <div className="flex gap-3">
              <GlassButton type="submit" size="sm" loading={pending}>Save</GlassButton>
              <GlassButton type="button" size="sm" variant="ghost" onClick={() => setEditing(null)}>
                Cancel
              </GlassButton>
            </div>
          </form>
        </GlassCard>
      )}
    </div>
  );
}
