"use client";

import * as React from "react";
import { Clock } from "lucide-react";
import { ProductCard } from "@/components/storefront/product-card";
import { useRecentlyViewed } from "@/store/recently-viewed";
import type { ProductCard as ProductCardData } from "@/types";

const emptySubscribe = () => () => {};

/** True only after hydration — avoids SSR/client markup mismatch for the
 *  localStorage-backed list, without setState-in-effect. */
function useHydrated() {
  return React.useSyncExternalStore(
    emptySubscribe,
    () => true,
    () => false
  );
}

/**
 * Records a product view. Drop into a product detail page (server component);
 * it renders nothing and writes to the persisted store once per product.
 */
export function TrackRecentlyViewed({ product }: { product: ProductCardData }) {
  const add = useRecentlyViewed((s) => s.add);

  React.useEffect(() => {
    add(product);
  }, [add, product]);

  return null;
}

/**
 * "Recently viewed" strip. Hidden until there is at least one item (after
 * excluding the current product), so it never shows an empty section.
 */
export function RecentlyViewed({
  excludeId,
  title = "Recently viewed",
}: {
  excludeId?: string;
  title?: string;
}) {
  const items = useRecentlyViewed((s) => s.items);
  const hydrated = useHydrated();

  if (!hydrated) return null;
  const list = items.filter((p) => p.id !== excludeId);
  if (list.length === 0) return null;

  return (
    <section aria-labelledby="recently-viewed-heading" className="mt-16">
      <div className="flex items-center gap-2">
        <Clock className="h-4 w-4 text-[var(--color-gold)]" aria-hidden="true" />
        <h2
          id="recently-viewed-heading"
          className="text-2xl text-[var(--color-text-primary)]"
        >
          {title}
        </h2>
      </div>
      <ul className="mt-6 grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4" role="list">
        {list.slice(0, 4).map((p) => (
          <li key={p.id}>
            <ProductCard product={p} />
          </li>
        ))}
      </ul>
    </section>
  );
}
