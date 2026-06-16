import Link from "next/link";
import Image from "next/image";
import {
  Truck,
  ShieldCheck,
  RefreshCw,
  Star,
  ArrowRight,
  Sparkles,
  Quote,
} from "lucide-react";
import { GlassButton, GlassCard } from "@/components/ui";
import { ProductCard } from "@/components/storefront/product-card";
import { getActiveCategories, getNewArrivals, getBestsellers } from "@/lib/catalog";
import { BRAND } from "@/lib/constants";

export const revalidate = 60;

const testimonials = [
  {
    text: "Finally found hijabs that stay in place all day without pins! The silk georgette is incredibly soft and the colours are beautiful. Will order again In Sha Allah.",
    name: "Fatima R.",
    location: "Mumbai",
    rating: 5,
  },
  {
    text: "The classic black abaya fits perfectly and the fabric is so lightweight — perfect for our Hyderabad summers. Delivered in 3 days, packaging was gorgeous.",
    name: "Amina K.",
    location: "Hyderabad",
    rating: 5,
  },
  {
    text: "Ordered the prayer scarf set for my mother — she loved it. The embroidery is detailed and the material is breathable. Great quality for the price.",
    name: "Zainab M.",
    location: "Delhi",
    rating: 5,
  },
  {
    text: "Customer service was so helpful when I needed to exchange sizes. Fast response on WhatsApp and the exchange was processed without any hassle. Highly recommend!",
    name: "Ruqayyah S.",
    location: "Chennai",
    rating: 5,
  },
];

const trustItems = [
  { icon: Truck, title: "Free Shipping", text: "On orders above ₹999 across India" },
  { icon: RefreshCw, title: "Easy Returns", text: "Hassle-free 7-day return window" },
  { icon: ShieldCheck, title: "Secure Checkout", text: "UPI, Cards, Net Banking & COD" },
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

        <div className="relative mx-auto max-w-7xl px-4 py-28 sm:px-6 sm:py-36 lg:px-10 lg:py-44">
          <div className="max-w-2xl">
            <p className="section-label">Modest wear · India</p>
            <h1 className="mt-5 text-5xl leading-[1.05] tracking-tight text-[var(--color-text-primary)] sm:text-6xl lg:text-7xl">
              Grace in{" "}
              <em className="gradient-text font-display italic not-italic">every</em>{" "}
              drape
            </h1>
            <p className="mt-6 max-w-lg text-lg leading-relaxed text-[var(--color-text-secondary)]">
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
                { value: "4.9★", label: "Average rating" },
                { value: "100%", label: "Authentic fabrics" },
              ].map((s) => (
                <div key={s.label}>
                  <p className="text-2xl font-semibold text-[var(--color-gold)]">
                    {s.value}
                  </p>
                  <p className="mt-1 text-xs tracking-widest text-[var(--color-text-muted)] uppercase">
                    {s.label}
                  </p>
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
          className="relative z-10 mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-20 lg:px-10"
        >
          <div className="mb-10 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="section-label">Collections</p>
              <h2
                id="categories-heading"
                className="mt-2 text-3xl text-[var(--color-text-primary)] sm:text-4xl"
              >
                Shop by Category
              </h2>
            </div>
            <Link
              href="/shop"
              className="flex items-center gap-1 text-sm text-[var(--color-gold)] underline-offset-4 hover:underline"
            >
              All products <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>

          <ul
            className="stagger-enter grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4"
            role="list"
          >
            {categories.map((cat) => (
              <li key={cat.id}>
                <Link
                  href={`/shop?category=${cat.slug}`}
                  className="glass glass-md glass-hover glass-sheen group block overflow-hidden !p-0"
                >
                  <div className="relative aspect-[3/4] bg-[var(--color-bg-mid)]">
                    {cat.image && (
                      <Image
                        src={cat.image}
                        alt={cat.name}
                        fill
                        sizes="(max-width: 640px) 50vw, (max-width: 1024px) 50vw, 25vw"
                        className="img-zoom object-cover"
                      />
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-[rgba(5,5,15,0.90)] via-[rgba(5,5,15,0.20)] to-transparent" />
                    <div className="absolute right-0 bottom-0 left-0 p-4 sm:p-5">
                      <p className="font-display text-base leading-tight text-[var(--color-text-primary)] sm:text-lg">
                        {cat.name}
                      </p>
                      <p className="mt-1.5 flex items-center gap-1 text-xs text-[var(--color-gold)] opacity-0 transition-opacity duration-300 group-hover:opacity-100">
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
          className="relative z-10 mx-auto max-w-7xl px-4 pb-16 sm:px-6 sm:pb-20 lg:px-10"
        >
          <div className="mb-10 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="section-label">Fresh in</p>
              <h2
                id="new-arrivals-heading"
                className="mt-2 text-3xl text-[var(--color-text-primary)] sm:text-4xl"
              >
                New Arrivals
              </h2>
            </div>
            <Link
              href="/shop?sort=newest"
              className="flex items-center gap-1 text-sm text-[var(--color-gold)] underline-offset-4 hover:underline"
            >
              View all <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>
          <ul
            className="stagger-enter grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4"
            role="list"
          >
            {newArrivals.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* ── PROMISE BANNER ────────────────────────────────────────────────────── */}
      <section className="cv-section relative z-10 mx-auto max-w-7xl px-4 pb-16 sm:px-6 sm:pb-20 lg:px-10">
        <GlassCard tier="elevated" padding="lg" className="text-center">
          <div className="mb-4 flex justify-center">
            <span className="inline-flex items-center gap-2 rounded-full border border-[var(--color-gold)]/25 bg-[var(--color-gold)]/8 px-4 py-1.5 text-xs font-semibold tracking-widest text-[var(--color-gold)] uppercase">
              <Sparkles className="h-3 w-3" aria-hidden="true" />
              Our promise
            </span>
          </div>
          <h2 className="text-3xl text-[var(--color-text-primary)] sm:text-4xl">
            Modest fashion that&apos;s
            <br />
            worthy of <em className="gradient-text italic not-italic">you</em>
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-sm leading-relaxed text-[var(--color-text-secondary)] sm:text-base">
            Every piece in the Nayabi Collection is selected with care — premium fabrics,
            thoughtful sizing, and styles that work for everyday life, prayer, and special
            occasions.
          </p>
          <div className="mt-10 grid grid-cols-2 gap-5 sm:grid-cols-4">
            {trustItems.map((t) => (
              <div key={t.title} className="flex flex-col items-center gap-3 text-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-[var(--color-gold)]/20 bg-[var(--color-gold)]/10">
                  <t.icon
                    className="h-5 w-5 text-[var(--color-gold)]"
                    aria-hidden="true"
                  />
                </div>
                <div>
                  <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                    {t.title}
                  </p>
                  <p className="mt-0.5 text-xs leading-relaxed text-[var(--color-text-muted)]">
                    {t.text}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </GlassCard>
      </section>

      {/* ── TESTIMONIALS ──────────────────────────────────────────────────────── */}
      <section
        aria-labelledby="testimonials-heading"
        className="cv-section relative z-10 mx-auto max-w-7xl px-4 pb-16 sm:px-6 sm:pb-20 lg:px-10"
      >
        <div className="mb-10 text-center">
          <p className="section-label">What our customers say</p>
          <h2
            id="testimonials-heading"
            className="mt-2 text-3xl text-[var(--color-text-primary)] sm:text-4xl"
          >
            Loved across India
          </h2>
        </div>
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" role="list">
          {testimonials.map((t) => (
            <li key={t.name}>
              <GlassCard padding="md" className="flex h-full flex-col">
                <Quote
                  className="mb-3 h-5 w-5 shrink-0 text-[var(--color-gold)]/50"
                  aria-hidden="true"
                />
                <p className="flex-1 text-sm leading-relaxed text-[var(--color-text-secondary)]">
                  {t.text}
                </p>
                <div className="mt-4 flex items-center justify-between border-t border-[var(--color-glass-border)] pt-4">
                  <div>
                    <p className="text-sm font-medium text-[var(--color-text-primary)]">
                      {t.name}
                    </p>
                    <p className="text-xs text-[var(--color-text-muted)]">{t.location}</p>
                  </div>
                  <div className="flex gap-0.5" aria-label={`Rated ${t.rating} out of 5`}>
                    {Array.from({ length: t.rating }).map((_, i) => (
                      <Star
                        key={i}
                        className="h-3.5 w-3.5 fill-[var(--color-gold)] text-[var(--color-gold)]"
                        aria-hidden="true"
                      />
                    ))}
                  </div>
                </div>
              </GlassCard>
            </li>
          ))}
        </ul>
      </section>

      {/* ── BESTSELLERS ────────────────────────────────────────────────────────── */}
      {bestsellers.length > 0 && (
        <section
          aria-labelledby="bestsellers-heading"
          className="cv-section relative z-10 mx-auto max-w-7xl px-4 pb-24 sm:px-6 lg:px-10"
        >
          <div className="mb-10 flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="section-label">Customer favourites</p>
              <h2
                id="bestsellers-heading"
                className="mt-2 text-3xl text-[var(--color-text-primary)] sm:text-4xl"
              >
                Bestsellers
              </h2>
            </div>
            <Link
              href="/shop?sort=popular"
              className="flex items-center gap-1 text-sm text-[var(--color-gold)] underline-offset-4 hover:underline"
            >
              View all <ArrowRight className="h-3.5 w-3.5" aria-hidden="true" />
            </Link>
          </div>
          <ul
            className="stagger-enter grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4"
            role="list"
          >
            {bestsellers.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} />
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* ── WHATSAPP CTA ──────────────────────────────────────────────────────── */}
      <section className="cv-section relative z-10 mx-auto max-w-7xl px-4 pb-24 sm:px-6 lg:px-10">
        <GlassCard
          tier="elevated"
          padding="lg"
          className="flex flex-col items-center justify-between gap-6 sm:flex-row"
        >
          <div>
            <p className="section-label">Need help choosing?</p>
            <h2 className="mt-1 text-2xl text-[var(--color-text-primary)] sm:text-3xl">
              Chat with us on WhatsApp
            </h2>
            <p className="mt-2 max-w-sm text-sm text-[var(--color-text-secondary)]">
              Our team can help you find the perfect hijab or abaya — size guidance,
              fabric advice, custom colour requests. We respond within minutes.
            </p>
          </div>
          <a
            href={BRAND.whatsappUrl(
              "Hi Nayabi Collection, I'd like help choosing a product."
            )}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-gold flex shrink-0 items-center gap-2 rounded-xl px-6 py-3 text-sm font-semibold"
          >
            <svg
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="currentColor"
              aria-hidden="true"
            >
              <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z" />
              <path d="M12 2C6.477 2 2 6.477 2 12c0 1.89.525 3.66 1.438 5.168L2 22l4.979-1.399A9.935 9.935 0 0012 22c5.523 0 10-4.477 10-10S17.523 2 12 2zm0 18.214a8.214 8.214 0 01-4.19-1.148l-.3-.178-3.106.871.872-3.042-.196-.313A8.214 8.214 0 113.786 12 8.214 8.214 0 0112 20.214z" />
            </svg>
            Chat on WhatsApp
          </a>
        </GlassCard>
      </section>
    </div>
  );
}
