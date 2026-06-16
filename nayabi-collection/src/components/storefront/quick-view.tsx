"use client";

import * as React from "react";
import Link from "next/link";
import Image from "next/image";
import { Eye, Star, ArrowRight } from "lucide-react";
import { GlassModal, GlassSkeleton } from "@/components/ui";
import { ProductActions } from "@/components/storefront/product-actions";
import type { QuickViewProduct } from "@/lib/catalog";

/** Hover/focus-revealed quick-view button. Lives as a sibling of the card link
 *  (not nested in the anchor) so it never triggers navigation. */
export function QuickViewTrigger({ slug, name }: { slug: string; name: string }) {
  const [open, setOpen] = React.useState(false);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        aria-label={`Quick view: ${name}`}
        className="glass glass-sm absolute top-2.5 right-2.5 z-20 flex h-9 w-9 items-center justify-center rounded-full text-[var(--color-text-primary)] opacity-0 transition-all duration-300 group-hover:opacity-100 hover:text-[var(--color-gold)] focus-visible:opacity-100"
      >
        <Eye className="h-4 w-4" aria-hidden="true" />
      </button>
      {open && <QuickViewModal slug={slug} onClose={() => setOpen(false)} />}
    </>
  );
}

function QuickViewModal({ slug, onClose }: { slug: string; onClose: () => void }) {
  const [product, setProduct] = React.useState<QuickViewProduct | null>(null);
  const [error, setError] = React.useState(false);

  React.useEffect(() => {
    const ctrl = new AbortController();
    fetch(`/api/products/${slug}`, { signal: ctrl.signal })
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error("not found"))))
      .then(setProduct)
      .catch((e) => {
        if (e?.name !== "AbortError") setError(true);
      });
    return () => ctrl.abort();
  }, [slug]);

  const discount =
    product?.comparePrice && product.comparePrice > product.price
      ? Math.round(((product.comparePrice - product.price) / product.comparePrice) * 100)
      : 0;

  return (
    <GlassModal open onClose={onClose} size="xl" title={product?.name ?? "Quick view"}>
      {error ? (
        <p className="py-8 text-center text-sm text-[var(--color-text-secondary)]">
          Couldn&apos;t load this product. Please try again.
        </p>
      ) : !product ? (
        <div className="grid gap-6 sm:grid-cols-2">
          <GlassSkeleton className="aspect-[3/4] w-full" />
          <div className="flex flex-col gap-3">
            <GlassSkeleton className="h-6 w-2/3" />
            <GlassSkeleton className="h-5 w-1/3" />
            <GlassSkeleton className="h-10 w-full" />
            <GlassSkeleton className="h-12 w-full" />
          </div>
        </div>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2">
          <div className="relative aspect-[3/4] overflow-hidden rounded-xl bg-[var(--color-bg-mid)]">
            {product.images[0] && (
              <Image
                src={product.images[0]}
                alt={product.name}
                fill
                sizes="(max-width: 640px) 90vw, 40vw"
                className="object-cover"
              />
            )}
            {discount > 0 && (
              <span className="absolute top-2 left-2 rounded-full bg-[var(--color-gold)] px-2.5 py-1 text-xs font-bold text-[var(--color-text-inverse)]">
                -{discount}%
              </span>
            )}
          </div>

          <div className="flex flex-col">
            <p className="text-xs text-[var(--color-text-muted)]">
              {product.category.name}
            </p>
            {product.rating != null && (product.reviewCount ?? 0) > 0 && (
              <p className="mt-1 flex items-center gap-1.5 text-sm text-[var(--color-text-secondary)]">
                <Star
                  className="h-3.5 w-3.5 fill-[var(--color-gold)] text-[var(--color-gold)]"
                  aria-hidden="true"
                />
                {product.rating} · {product.reviewCount} review
                {product.reviewCount === 1 ? "" : "s"}
              </p>
            )}

            <div className="mt-4">
              <ProductActions
                productId={product.id}
                productName={product.name}
                basePrice={product.price}
                baseImage={product.images[0] ?? ""}
                variants={product.variants}
              />
            </div>

            <p className="mt-5 line-clamp-3 text-sm leading-relaxed text-[var(--color-text-secondary)]">
              {product.description}
            </p>

            <Link
              href={`/products/${product.slug}`}
              onClick={onClose}
              className="mt-5 inline-flex items-center gap-1 text-sm text-[var(--color-gold)] underline-offset-4 hover:underline"
            >
              View full details <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>
        </div>
      )}
    </GlassModal>
  );
}
