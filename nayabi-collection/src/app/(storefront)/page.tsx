import Link from "next/link";
import Image from "next/image";
import { Truck, ShieldCheck, RefreshCw, Sparkles } from "lucide-react";
import { GlassButton, GlassCard } from "@/components/ui";
import { ProductCard } from "@/components/storefront/product-card";
import { getActiveCategories, getNewArrivals, getBestsellers } from "@/lib/catalog";

export const revalidate = 60;

const trustItems = [
  { icon: Truck, title: "Free Shipping", text: "On orders above ₹999" },
  { icon: RefreshCw, title: "Easy Returns", text: "7-day return window" },
  { icon: ShieldCheck, title: "Secure Payments", text: "UPI, Cards & COD" },
  { icon: Sparkles, title: "Premium Fabrics", text: "Hand-picked quality" },
];

export default async function HomePage() {
  const [categories, newArrivals, bestsellers] = await Promise.all([
    getActiveCategories(),
    getNewArrivals(8),
    getBestsellers(4),
  ]);

  return (
    <div className="mesh-bg">
      {/* Hero */}
      <section className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 py-20 sm:py-28 lg:py-36">
        <div className="max-w-2xl">
          <p className="text-sm uppercase tracking-[0.25em] text-[var(--color-gold)]">
            Modest wear, redefined
          </p>
          <h1 className="mt-4 text-4xl sm:text-5xl lg:text-6xl text-[var(--color-text-primary)] leading-[1.1]">
            Elegance in <em className="text-[var(--color-gold)] not-italic font-display italic">every</em> drape
          </h1>
          <p className="mt-6 max-w-lg text-base sm:text-lg text-[var(--color-text-secondary)] leading-relaxed">
            Premium Hijabs, Abayas and Namaz Scarfs — thoughtfully crafted, beautifully
            finished, delivered across India.
          </p>
          <div className="mt-8 flex flex-wrap gap-4">
            <Link href="/shop">
              <GlassButton size="lg">Shop the Collection</GlassButton>
            </Link>
            <Link href="/shop?category=abayas">
              <GlassButton size="lg" variant="secondary">
                Explore Abayas
              </GlassButton>
            </Link>
          </div>
        </div>
      </section>

      {/* Categories */}
      {categories.length > 0 && (
        <section
          aria-labelledby="categories-heading"
          className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-16"
        >
          <h2 id="categories-heading" className="text-2xl sm:text-3xl text-[var(--color-text-primary)]">
            Shop by Category
          </h2>
          <ul
            className="stagger-enter mt-8 grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4"
            role="list"
          >
            {categories.map((cat) => (
              <li key={cat.id}>
                <Link
                  href={`/shop?category=${cat.slug}`}
                  className="glass glass-md glass-hover group block overflow-hidden !p-0"
                >
                  <div className="relative aspect-square bg-[var(--color-bg-mid)]">
                    {cat.image && (
                      <Image
                        src={cat.image}
                        alt=""
                        fill
                        sizes="(max-width: 1024px) 50vw, 25vw"
                        className="object-cover transition-transform duration-500 group-hover:scale-105"
                      />
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-[rgba(10,10,20,0.85)] to-transparent" />
                    <p className="absolute bottom-4 left-4 font-display text-lg text-[var(--color-text-primary)]">
                      {cat.name}
                    </p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* New arrivals */}
      {newArrivals.length > 0 && (
        <section
          aria-labelledby="new-arrivals-heading"
          className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-16"
        >
          <div className="flex items-end justify-between">
            <h2 id="new-arrivals-heading" className="text-2xl sm:text-3xl text-[var(--color-text-primary)]">
              New Arrivals
            </h2>
            <Link
              href="/shop"
              className="text-sm text-[var(--color-gold)] hover:underline underline-offset-4"
            >
              View all →
            </Link>
          </div>
          <ul className="stagger-enter mt-8 grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4" role="list">
            {newArrivals.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Bestsellers */}
      {bestsellers.length > 0 && (
        <section
          aria-labelledby="bestsellers-heading"
          className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-16"
        >
          <h2 id="bestsellers-heading" className="text-2xl sm:text-3xl text-[var(--color-text-primary)]">
            Bestsellers
          </h2>
          <ul className="stagger-enter mt-8 grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4" role="list">
            {bestsellers.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Trust badges */}
      <section aria-label="Why shop with us" className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-20">
        <ul className="grid grid-cols-2 gap-4 lg:grid-cols-4" role="list">
          {trustItems.map((t) => (
            <li key={t.title}>
              <GlassCard padding="md" className="h-full text-center">
                <t.icon className="mx-auto h-6 w-6 text-[var(--color-gold)]" aria-hidden="true" />
                <p className="mt-3 text-sm font-semibold text-[var(--color-text-primary)]">
                  {t.title}
                </p>
                <p className="mt-1 text-xs text-[var(--color-text-secondary)]">{t.text}</p>
              </GlassCard>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
