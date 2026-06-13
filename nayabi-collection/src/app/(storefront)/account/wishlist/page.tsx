import Link from "next/link";
import Image from "next/image";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import { Heart } from "lucide-react";

export const metadata = { title: "My Wishlist" };

export default async function WishlistPage() {
  const session = await auth();
  let items: Awaited<ReturnType<typeof fetchWishlist>> = [];
  try {
    items = await fetchWishlist(session!.user.id);
  } catch {
    items = [];
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Wishlist</h2>

      {items.length === 0 ? (
        <GlassCard padding="lg" className="flex flex-col items-center gap-4 text-center">
          <Heart className="h-12 w-12 text-[var(--color-text-muted)]" aria-hidden="true" />
          <div>
            <p className="text-[var(--color-text-primary)] font-medium">Your wishlist is empty</p>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              Save items you love by clicking the heart icon on any product.
            </p>
          </div>
          <Link
            href="/shop"
            className="text-sm text-[var(--color-gold)] underline underline-offset-2"
          >
            Explore the collection
          </Link>
        </GlassCard>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" role="list">
          {items.map((item) => {
            const price = item.variant?.price ?? item.product.price;
            const comparePrice = item.product.comparePrice;
            const outOfStock =
              item.variant
                ? item.variant.stock === 0
                : item.product.variants.every((v) => v.stock === 0);
            const image = item.product.images[0] ?? "";

            return (
              <li key={item.id}>
                <Link href={`/products/${item.product.slug}`}>
                  <GlassCard padding="sm" hoverable className="group overflow-hidden">
                    <div className="relative overflow-hidden rounded-lg aspect-[3/4] bg-[var(--color-glass-bg)] mb-3">
                      {image && (
                        <Image
                          src={image}
                          alt={item.product.name}
                          fill
                          className="object-cover transition-transform duration-500 group-hover:scale-105"
                          sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
                        />
                      )}
                      {outOfStock && (
                        <div className="absolute inset-0 flex items-center justify-center bg-[var(--color-bg-base)]/60">
                          <GlassBadge variant="neutral">Out of stock</GlassBadge>
                        </div>
                      )}
                    </div>
                    <p className="text-sm font-medium text-[var(--color-text-primary)] line-clamp-1">
                      {item.product.name}
                    </p>
                    <div className="mt-1 flex items-center gap-2">
                      <span className="text-sm font-bold text-[var(--color-gold)]">
                        {formatPrice(price)}
                      </span>
                      {comparePrice && comparePrice > price && (
                        <span className="text-xs line-through text-[var(--color-text-muted)]">
                          {formatPrice(comparePrice)}
                        </span>
                      )}
                    </div>
                  </GlassCard>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

function fetchWishlist(userId: string) {
  return db.wishlist.findMany({
    where: { userId },
    include: {
      product: {
        include: { variants: { select: { stock: true } } },
      },
      variant: { select: { price: true, stock: true, color: true, size: true } },
    },
    orderBy: { addedAt: "desc" },
  });
}
