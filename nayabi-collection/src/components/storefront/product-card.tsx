import Link from "next/link";
import Image from "next/image";
import { Star } from "lucide-react";
import { GlassBadge } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import type { ProductCard as ProductCardData } from "@/types";

export function ProductCard({ product }: { product: ProductCardData }) {
  const discount =
    product.comparePrice && product.comparePrice > product.price
      ? Math.round(((product.comparePrice - product.price) / product.comparePrice) * 100)
      : 0;

  return (
    <Link
      href={`/products/${product.slug}`}
      className="glass glass-md glass-hover group block overflow-hidden !p-0"
    >
      <div className="relative aspect-[3/4] overflow-hidden bg-[var(--color-bg-mid)]">
        {product.images[0] ? (
          <Image
            src={product.images[0]}
            alt={product.name}
            fill
            sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw"
            className="object-cover transition-transform duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-[var(--color-text-muted)] text-sm">
            No image
          </div>
        )}

        <div className="absolute left-2 top-2 flex flex-col gap-1.5">
          {discount > 0 && <GlassBadge variant="gold">-{discount}%</GlassBadge>}
          {product.isOutOfStock ? (
            <GlassBadge variant="error">Out of stock</GlassBadge>
          ) : (
            product.isLowStock && <GlassBadge variant="warning">Low stock</GlassBadge>
          )}
        </div>

        {/* Quick-view label — fades in on hover */}
        {!product.isOutOfStock && (
          <div className="absolute inset-x-0 bottom-0 flex justify-center pb-3 opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none">
            <span className="glass glass-sm border border-[var(--color-gold)]/35 px-4 py-1 text-xs font-semibold text-[var(--color-gold)] tracking-wide">
              View Product
            </span>
          </div>
        )}
      </div>

      <div className="p-4">
        <p className="text-xs text-[var(--color-text-muted)]">{product.category.name}</p>
        <h3 className="mt-1 truncate text-sm font-medium text-[var(--color-text-primary)] !font-[var(--font-body)]">
          {product.name}
        </h3>

        <div className="mt-2 flex items-center justify-between gap-2">
          <p className="flex items-baseline gap-1.5">
            <span className="text-base font-semibold text-[var(--color-gold)]">
              {formatPrice(product.price)}
            </span>
            {discount > 0 && (
              <s className="text-xs text-[var(--color-text-muted)]">
                {formatPrice(product.comparePrice!)}
              </s>
            )}
          </p>

          {product.rating != null && product.reviewCount! > 0 && (
            <p className="flex items-center gap-1 text-xs text-[var(--color-text-secondary)]">
              <Star
                className="h-3.5 w-3.5 fill-[var(--color-gold)] text-[var(--color-gold)]"
                aria-hidden="true"
              />
              <span aria-label={`Rated ${product.rating} out of 5`}>{product.rating}</span>
            </p>
          )}
        </div>
      </div>
    </Link>
  );
}
