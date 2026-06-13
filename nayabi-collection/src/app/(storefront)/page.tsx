import Link from "next/link";
import Image from "next/image";
import { Truck, ShieldCheck, RefreshCw, Star, ArrowRight } from "lucide-react";
import { GlassButton, GlassCard } from "@/components/ui";
import { ProductCard } from "@/components/storefront/product-card";
import { getActiveCategories, getNewArrivals, getBestsellers } from "@/lib/catalog";

export const revalidate = 60;

const trustItems = [
  {
    icon: Truck,
    title: "Free Shipping",
    text: "On all orders above ₹999 across India",
  },
  {
    icon: RefreshCw,
    title: "Easy Returns",
    text: "Hassle-free 7-day return window",
  },
  {
    icon: ShieldCheck,
    title: "Secure Checkout",
    text: "UPI, Cards, Net Banking & COD",
  },
  {
    icon: Star,
    title: "Premium Fabrics",
    text: "Hand-curated georgette, nida & chiffon",
  },
];

export default async function HomePage() {
  const [categories, newArrivals, bestsellers] = await Promise.all([
    getActiveCategories(),
    getNewArrivals(8),
    getBestsellers(4),
  ]);

  return (
    <div className="mesh-bg min-h-screen">

      {/* ── HERO ─────────────────────────────────────────────────────────────── */}
      <section className="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 pt-20 pb-12 sm:pt-28 sm:pb-16 lg:pt-36 lg:pb-20">
        <div className="grid gap-12 lg:grid-cols-2 lg:items-center">

          {/* Left — text */}
          <div className="max-w-xl">
            <p className="section-label">Modest wear · India</p>
            <h1 className="mt-4 text-5xl sm:text-6xl lg:text-7xl text-[var(--color-text-primary)] leading-[1.05] tracking-tight">
              Grace in{" "}
              <em className="text-[var(--color-gold)] not-italic font-display italic">
                every
              </em>{" "}
              drape
            </h1>
            <p className="mt-6 text-lg text-[var(--color-text-secondary)] leading-relaxed max-w-lg">
              Nayabi Collection brings premium Hijabs, Abayas and Namaz Scarfs — thoughtfully
              crafted for Muslim women across India who believe faith and style go hand in hand.
            </p>

            <div className="mt-8 flex flex-wrap gap-4">
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

            {/* Micro-stats */}
            <div className="mt-10 flex gap-8">
              {[
                { value: "10,000+", label: "Happy customers" },
                { value: "4.9★", label: "Average rating" },
                { value: "100%", label: "Authentic fabrics" },
              ].map((s) => (
                <div key={s.label}>
                  <p className="text-xl font-semibold text-[var(--color-gold)]">{s.value}</p>
                  <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">{s.label}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Right — featured glass cards */}
          <div className="relative hidden lg:block">
            <div className="relative h-[520px]">
              {/* Main card */}
              <div className="glass glass-elevated absolute right-0 top-0 w-64 overflow-hidden rounded-2xl !p-0">
                <div className="relative h-80 bg-[var(--color-bg-mid)]">
                  <Image
                    src="https://images.unsplash.com/photo-1631233859262-0d62ed426d7b?auto=format&fit=crop&w=600&q=85"
                    alt="Embroidered abaya"
                    fill
                    sizes="256px"
                    className="object-cover"
                    priority
                  />
                </div>
                <div className="p-4">
                  <p className="text-xs text-[var(--color-text-muted)]">New arrival</p>
                  <p className="mt-1 text-sm font-medium text-[var(--color-text-primary)]">Embroidered Kimono Abaya</p>
                  <p className="mt-1 text-sm font-semibold text-[var(--color-gold)]">₹2,499</p>
                </div>
              </div>
              {/* Secondary card — offset */}
              <div className="glass absolute left-0 top-28 w-56 overflow-hidden rounded-2xl !p-0">
                <div className="relative h-56 bg-[var(--color-bg-mid)]">
                  <Image
                    src="https://images.unsplash.com/photo-1611507929918-08e9e7da2dd4?auto=format&fit=crop&w=500&q=80"
                    alt="Chiffon hijab"
                    fill
                    sizes="224px"
                    className="object-cover"
                  />
                </div>
                <div className="p-4">
                  <p className="text-xs text-[var(--color-text-muted)]">Bestseller</p>
                  <p className="mt-1 text-sm font-medium text-[var(--color-text-primary)]">Silk Georgette Hijab</p>
                  <p className="mt-1 text-sm font-semibold text-[var(--color-gold)]">₹599</p>
                </div>
              </div>
              {/* Floating badge */}
              <div className="glass absolute bottom-16 right-8 px-4 py-2.5 rounded-xl">
                <p className="text-xs font-semibold text-[var(--color-gold)]">Free shipping above ₹999</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── GOLD DIVIDER ────────────────────────────────────────────────────── */}
      <div className="relative z-10 mx-auto max-w-5xl px-4">
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
                    {/* Gradient overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-[rgba(5,5,15,0.92)] via-[rgba(5,5,15,0.3)] to-transparent" />
                    {/* Top highlight */}
                    <div className="absolute inset-x-0 top-0 h-16 bg-gradient-to-b from-[rgba(255,255,255,0.06)] to-transparent" />
                    <div className="absolute bottom-0 left-0 right-0 p-4">
                      <p className="font-display text-base sm:text-lg text-[var(--color-text-primary)]">
                        {cat.name}
                      </p>
                      <p className="mt-1 text-xs text-[var(--color-gold)] flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
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

      {/* ── NEW ARRIVALS ─────────────────────────────────────────────────────── */}
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
          <p className="section-label text-center">Our promise</p>
          <h2 className="mt-3 text-3xl sm:text-4xl text-[var(--color-text-primary)]">
            Modest fashion that&apos;s<br />worthy of <em className="text-[var(--color-gold)] not-italic italic">you</em>
          </h2>
          <p className="mt-4 max-w-xl mx-auto text-[var(--color-text-secondary)] leading-relaxed">
            Every piece in the Nayabi Collection is selected with care — premium fabrics,
            thoughtful sizing, and styles that work for everyday life, prayer, and special occasions.
          </p>
          <div className="mt-8 grid grid-cols-2 gap-4 sm:grid-cols-4 text-left">
            {trustItems.map((t) => (
              <div key={t.title} className="flex flex-col items-center text-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-gold)]/12 border border-[var(--color-gold)]/20">
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

      {/* ── BESTSELLERS ──────────────────────────────────────────────────────── */}
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
