import Link from "next/link";
import Image from "next/image";
import { Truck, ShieldCheck, RefreshCw, Star, ArrowRight, Sparkles } from "lucide-react";
import { GlassButton, GlassCard } from "@/components/ui";
import { ProductCard } from "@/components/storefront/product-card";
import { getActiveCategories, getNewArrivals, getBestsellers } from "@/lib/catalog";

export const revalidate = 60;

const trustItems = [
  { icon: Truck,       title: "Free Shipping",   text: "On orders above ₹999 across India" },
  { icon: RefreshCw,   title: "Easy Returns",    text: "Hassle-free 7-day return window" },
  { icon: ShieldCheck, title: "Secure Checkout", text: "UPI, Cards, Net Banking & COD" },
  { icon: Star,        title: "Premium Fabrics", text: "Hand-curated georgette, nida & chiffon" },
];

export default async function HomePage() {
  const [categories, newArrivals, bestsellers] = await Promise.all([
    getActiveCategories(),
    getNewArrivals(8),
    getBestsellers(4),
  ]);

  return (
    <div className="mesh-bg min-h-screen">

      {/* ── HERO ──────────────────────────────────────────────────────────────── */}
      <section className="relative z-10 overflow-hidden">
        {/* Full-bleed background image */}
        <div className="absolute inset-0">
          <Image
            src="https://images.unsplash.com/photo-1583391733956-6c78276477e2?auto=format&fit=crop&w=1600&q=80"
            alt=""
            fill
            className="object-cover object-center"
            priority
            sizes="100vw"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-[rgba(5,5,15,0.93)] via-[rgba(5,5,15,0.72)] to-[rgba(5,5,15,0.35)]" />
          <div className="absolute inset-0 bg-gradient-to-t from-[rgba(5,5,15,0.80)] via-transparent to-transparent" />
        </div>

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 py-28 sm:py-36 lg:py-44">
          <div className="max-w-2xl">
            <p className="section-label">Modest wear · India</p>
            <h1 className="mt-5 text-5xl sm:text-6xl lg:text-7xl text-[var(--color-text-primary)] leading-[1.05] tracking-tight">
              Grace in{" "}
              <em className="text-[var(--color-gold)] not-italic font-display italic">every</em>{" "}
              drape
            </h1>
            <p className="mt-6 text-lg text-[var(--color-text-secondary)] leading-relaxed max-w-lg">
              Premium Hijabs, Abayas and Namaz Scarfs — thoughtfully crafted for Muslim
              women across India who believe faith and style go hand in hand.
            </p>

            <div className="mt-10 flex flex-wrap gap-4">
              <Link href="/shop">
                <GlassButton size="lg">
                  Shop the Collection
                  <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                </GlassButton>
              </Link>
              <Link href="/shop?category=abayas">
                <GlassButton size="lg" variant="secondary">
                  Explore Abayas
                </GlassButton>
              </Link>
            </div>

            <div className="mt-12 flex gap-10">
              {[
                { value: "10,000+", label: "Happy customers" },
                { value: "4.9★",    label: "Average rating" },
                { value: "100%",    label: "Authentic fabrics" },
              ].map((s) => (
                <div key={s.label}>
                  <p className="text-2xl font-semibold text-[var(--color-gold)]">{s.value}</p>
                  <p className="mt-1 text-xs text-[var(--color-text-muted)] uppercase tracking-widest">{s.label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── GOLD DIVIDER ────────────────────────────────────────────────────── */}
      <div className="relative z-10 mx-auto max-w-5xl px-4 py-2">
        <div className="gold-divider" />
      </div>

      {/* ── CATEGORIES ──────────────────────────────────────────────────────── */}
      {categories.length > 0 && (
        <section
          aria-labelledby="categories-heading"
          className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 py-16 sm:py-20"
        >
          <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between mb-10">
            <div>
              <p className="section-label">Collections</p>
              <h2 id="categories-heading" className="mt-2 text-3xl sm:text-4xl text-[var(--color-text-primary)]">
                Shop by Category
              </h2>
            </div>
            <Link href="/shop" className="text-sm text-[var(--color-gold)] hover:underline underline-offset-4 flex items-center gap-1">
              All products <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>

          <ul className="stagger-enter grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4" role="list">
            {categories.map((cat) => (
              <li key={cat.id}>
                <Link
                  href={`/shop?category=${cat.slug}`}
                  className="glass glass-md glass-hover group block overflow-hidden !p-0"
                >
                  <div className="relative aspect-[3/4] bg-[var(--color-bg-mid)]">
                    {cat.image && (
                      <Image
                        src={cat.image}
                        alt={cat.name}
                        fill
                        sizes="(max-width: 640px) 50vw, (max-width: 1024px) 50vw, 25vw"
                        className="object-cover img-zoom"
                      />
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-[rgba(5,5,15,0.90)] via-[rgba(5,5,15,0.20)] to-transparent" />
                    <div className="absolute bottom-0 left-0 right-0 p-4 sm:p-5">
                      <p className="font-display text-base sm:text-lg text-[var(--color-text-primary)] leading-tight">
                        {cat.name}
                      </p>
                      <p className="mt-1.5 text-xs text-[var(--color-gold)] flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                        Shop now <ArrowRight className="h-3 w-3" aria-hidden="true" />
                      </p>
                    </div>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* ── NEW ARRIVALS ──────────────────────────────────────────────────────── */}
      {newArrivals.length > 0 && (
        <section
          aria-labelledby="new-arrivals-heading"
          className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-16 sm:pb-20"
        >
          <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between mb-10">
            <div>
              <p className="section-label">Fresh in</p>
              <h2 id="new-arrivals-heading" className="mt-2 text-3xl sm:text-4xl text-[var(--color-text-primary)]">
                New Arrivals
              </h2>
            </div>
            <Link href="/shop?sort=newest" className="text-sm text-[var(--color-gold)] hover:underline underline-offset-4 flex items-center gap-1">
              View all <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>
          <ul className="stagger-enter grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4" role="list">
            {newArrivals.map((p) => (
              <li key={p.id}><ProductCard product={p} /></li>
            ))}
          </ul>
        </section>
      )}

      {/* ── PROMISE BANNER ────────────────────────────────────────────────────── */}
      <section className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-16 sm:pb-20">
        <GlassCard tier="elevated" padding="lg" className="text-center">
          <div className="flex justify-center mb-4">
            <span className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-semibold tracking-widest uppercase text-[var(--color-gold)] border border-[var(--color-gold)]/25 bg-[var(--color-gold)]/8">
              <Sparkles className="h-3 w-3" aria-hidden="true" />
              Our promise
            </span>
          </div>
          <h2 className="text-3xl sm:text-4xl text-[var(--color-text-primary)]">
            Modest fashion that&apos;s<br />worthy of{" "}
            <em className="text-[var(--color-gold)] not-italic italic">you</em>
          </h2>
          <p className="mt-4 max-w-xl mx-auto text-[var(--color-text-secondary)] leading-relaxed text-sm sm:text-base">
            Every piece in the Nayabi Collection is selected with care — premium fabrics,
            thoughtful sizing, and styles that work for everyday life, prayer, and special occasions.
          </p>
          <div className="mt-10 grid grid-cols-2 gap-5 sm:grid-cols-4">
            {trustItems.map((t) => (
              <div key={t.title} className="flex flex-col items-center text-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--color-gold)]/10 border border-[var(--color-gold)]/20">
                  <t.icon className="h-5 w-5 text-[var(--color-gold)]" aria-hidden="true" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-[var(--color-text-primary)]">{t.title}</p>
                  <p className="mt-0.5 text-xs text-[var(--color-text-muted)] leading-relaxed">{t.text}</p>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </section>

      {/* ── BESTSELLERS ────────────────────────────────────────────────────────── */}
      {bestsellers.length > 0 && (
        <section
          aria-labelledby="bestsellers-heading"
          className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pb-24"
        >
          <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between mb-10">
            <div>
              <p className="section-label">Customer favourites</p>
              <h2 id="bestsellers-heading" className="mt-2 text-3xl sm:text-4xl text-[var(--color-text-primary)]">
                Bestsellers
              </h2>
            </div>
            <Link href="/shop?sort=popular" className="text-sm text-[var(--color-gold)] hover:underline underline-offset-4 flex items-center gap-1">
              View all <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>
          <ul className="stagger-enter grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4" role="list">
            {bestsellers.map((p) => (
              <li key={p.id}><ProductCard product={p} /></li>
            ))}
          </ul>
        </section>
      )}

    </div>
  );
}
