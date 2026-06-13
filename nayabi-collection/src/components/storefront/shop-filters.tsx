"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SlidersHorizontal, X } from "lucide-react";
import { GlassButton, GlassBadge } from "@/components/ui";
import { cn } from "@/lib/utils";

interface Facets {
  colors: string[];
  sizes: string[];
  fabrics: string[];
  categories: { name: string; slug: string }[];
}

const sortOptions = [
  { value: "newest", label: "Newest" },
  { value: "price-asc", label: "Price: Low to High" },
  { value: "price-desc", label: "Price: High to Low" },
];

export function ShopFilters({ facets }: { facets: Facets }) {
  const router = useRouter();
  const params = useSearchParams();
  const [drawerOpen, setDrawerOpen] = React.useState(false);

  const active = {
    category: params.get("category"),
    color: params.get("color"),
    size: params.get("size"),
    fabric: params.get("fabric"),
    sort: params.get("sort") ?? "newest",
    q: params.get("q"),
  };

  function setParam(key: string, value: string | null) {
    const next = new URLSearchParams(params.toString());
    if (value === null || next.get(key) === value) next.delete(key);
    else next.set(key, value);
    next.delete("page");
    router.push(`/shop?${next.toString()}`);
  }

  const activeChips = (
    [
      ["category", active.category],
      ["color", active.color],
      ["size", active.size],
      ["fabric", active.fabric],
      ["q", active.q],
    ] as const
  ).filter(([, v]) => v);

  const filterGroups = (
    <>
      <FilterGroup label="Category">
        {facets.categories.map((c) => (
          <FilterChip
            key={c.slug}
            selected={active.category === c.slug}
            onClick={() => setParam("category", c.slug)}
          >
            {c.name}
          </FilterChip>
        ))}
      </FilterGroup>
      {facets.colors.length > 0 && (
        <FilterGroup label="Colour">
          {facets.colors.map((c) => (
            <FilterChip key={c} selected={active.color === c} onClick={() => setParam("color", c)}>
              {c}
            </FilterChip>
          ))}
        </FilterGroup>
      )}
      {facets.sizes.length > 0 && (
        <FilterGroup label="Size">
          {facets.sizes.map((s) => (
            <FilterChip key={s} selected={active.size === s} onClick={() => setParam("size", s)}>
              {s}
            </FilterChip>
          ))}
        </FilterGroup>
      )}
      {facets.fabrics.length > 0 && (
        <FilterGroup label="Fabric">
          {facets.fabrics.map((f) => (
            <FilterChip key={f} selected={active.fabric === f} onClick={() => setParam("fabric", f)}>
              {f}
            </FilterChip>
          ))}
        </FilterGroup>
      )}
    </>
  );

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Mobile filter trigger */}
        <GlassButton
          variant="secondary"
          size="sm"
          className="lg:hidden"
          onClick={() => setDrawerOpen(true)}
          icon={<SlidersHorizontal className="h-4 w-4" aria-hidden="true" />}
          aria-expanded={drawerOpen}
        >
          Filters
        </GlassButton>

        {/* Sort */}
        <label className="ml-auto flex items-center gap-2 text-sm text-[var(--color-text-secondary)]">
          Sort by
          <select
            value={active.sort}
            onChange={(e) => setParam("sort", e.target.value === "newest" ? null : e.target.value)}
            className="glass-input !w-auto !py-1.5 text-sm"
          >
            {sortOptions.map((o) => (
              <option key={o.value} value={o.value} className="bg-[var(--color-bg-mid)]">
                {o.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {/* Active filter chips */}
      {activeChips.length > 0 && (
        <div className="flex flex-wrap items-center gap-2" aria-label="Active filters">
          {activeChips.map(([key, value]) => (
            <button key={key} onClick={() => setParam(key, null)} aria-label={`Remove ${key} filter: ${value}`}>
              <GlassBadge variant="gold" className="cursor-pointer hover:opacity-80">
                {value} <X className="h-3 w-3" aria-hidden="true" />
              </GlassBadge>
            </button>
          ))}
          <button
            onClick={() => router.push("/shop")}
            className="text-xs text-[var(--color-text-muted)] underline underline-offset-2 hover:text-[var(--color-text-primary)]"
          >
            Clear all
          </button>
        </div>
      )}

      {/* Desktop filters */}
      <div className="hidden lg:flex flex-col gap-5">{filterGroups}</div>

      {/* Mobile drawer */}
      {drawerOpen && (
        <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Filters">
          <div className="absolute inset-0 bg-black/60" onClick={() => setDrawerOpen(false)} aria-hidden="true" />
          <div className="glass !rounded-none absolute left-0 top-0 h-full w-80 overflow-y-auto border-y-0 border-l-0 p-5">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="font-display text-lg">Filters</h2>
              <button onClick={() => setDrawerOpen(false)} aria-label="Close filters" className="p-1.5">
                <X className="h-5 w-5" aria-hidden="true" />
              </button>
            </div>
            <div className="flex flex-col gap-5">{filterGroups}</div>
            <GlassButton fullWidth className="mt-6" onClick={() => setDrawerOpen(false)}>
              Show results
            </GlassButton>
          </div>
        </div>
      )}
    </div>
  );
}

function FilterGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <fieldset>
      <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
        {label}
      </legend>
      <div className="flex flex-wrap gap-2">{children}</div>
    </fieldset>
  );
}

function FilterChip({
  selected,
  onClick,
  children,
}: {
  selected: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      aria-pressed={selected}
      className={cn(
        "rounded-full border px-3 py-1 text-xs transition-colors",
        selected
          ? "border-[var(--color-gold)] bg-[rgba(201,169,110,0.15)] text-[var(--color-gold)]"
          : "border-[var(--color-glass-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-gold)] hover:text-[var(--color-text-primary)]"
      )}
    >
      {children}
    </button>
  );
}
