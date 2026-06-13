"use client";

import * as React from "react";
import Image from "next/image";
import Link from "next/link";
import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { GlassButton } from "@/components/ui";
import { cn } from "@/lib/utils";

const slides = [
  {
    id: 1,
    image: "https://images.unsplash.com/photo-1583391733956-6c78276477e2?auto=format&fit=crop&w=1600&q=80",
    label: "New Season · 2026",
    heading: "Grace in",
    accent: "every",
    tail: "drape",
    sub: "Premium Hijabs, Abayas and Namaz Scarfs — thoughtfully crafted for Muslim women across India.",
    cta:       { label: "Shop the Collection", href: "/shop" },
    secondary: { label: "Explore Abayas",      href: "/shop?category=abayas" },
  },
  {
    id: 2,
    image: "https://images.unsplash.com/photo-1611507929918-08e9e7da2dd4?auto=format&fit=crop&w=1600&q=80",
    label: "Hijab Edit",
    heading: "Wrapped in",
    accent: "elegance",
    tail: "",
    sub: "Hand-picked georgette, chiffon and nida hijabs in 40+ colours. Find your perfect drape.",
    cta:       { label: "Shop Hijabs",  href: "/shop?category=hijabs" },
    secondary: { label: "View All",     href: "/shop" },
  },
  {
    id: 3,
    image: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=1600&q=80",
    label: "Prayer Collection",
    heading: "Faith meets",
    accent: "style",
    tail: "",
    sub: "Lightweight namaz scarfs and elegant abayas — from everyday prayer to special occasions.",
    cta:       { label: "Shop Abayas",       href: "/shop?category=abayas" },
    secondary: { label: "Namaz Scarfs",      href: "/shop?category=namaz-scarfs" },
  },
];

const INTERVAL_MS = 5000;

export function HeroCarousel() {
  const [current, setCurrent] = React.useState(0);
  const [paused,  setPaused]  = React.useState(false);
  const [key,     setKey]     = React.useState(0); // restarts progress bar
  const timerRef = React.useRef<ReturnType<typeof setInterval> | null>(null);

  const go = React.useCallback((index: number) => {
    setCurrent(index);
    setKey((k) => k + 1);
  }, []);

  const next = React.useCallback(() => go((current + 1) % slides.length), [current, go]);
  const prev = React.useCallback(() => go((current - 1 + slides.length) % slides.length), [current, go]);

  React.useEffect(() => {
    if (paused) return;
    timerRef.current = setInterval(next, INTERVAL_MS);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [paused, next]);

  // Keyboard navigation
  React.useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "ArrowLeft")  prev();
      if (e.key === "ArrowRight") next();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [prev, next]);

  return (
    <section
      aria-label="Featured collections slideshow"
      aria-roledescription="carousel"
      className="relative z-10 overflow-hidden"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      {/* ── Background images ── */}
      {slides.map((slide, i) => (
        <div
          key={slide.id}
          role="group"
          aria-roledescription="slide"
          aria-label={`Slide ${i + 1} of ${slides.length}: ${slide.label}`}
          aria-hidden={i !== current}
          className={cn(
            "absolute inset-0 transition-opacity duration-700 ease-in-out",
            i === current ? "opacity-100 z-10" : "opacity-0 z-0"
          )}
        >
          <Image
            src={slide.image}
            alt=""
            fill
            className="object-cover object-center"
            priority={i === 0}
            sizes="100vw"
          />
          {/* Directional gradient for text legibility */}
          <div className="absolute inset-0 bg-gradient-to-r from-[rgba(7,7,28,0.95)] via-[rgba(7,7,28,0.75)] to-[rgba(7,7,28,0.30)]" />
          <div className="absolute inset-0 bg-gradient-to-t from-[rgba(7,7,28,0.85)] via-transparent to-transparent" />
        </div>
      ))}

      {/* ── Slide content ── */}
      <div className="relative z-20 mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 py-28 sm:py-36 lg:py-48">
        {slides.map((slide, i) => (
          <div
            key={slide.id}
            aria-hidden={i !== current}
            className={cn(
              "max-w-2xl transition-all duration-700 ease-out",
              i === current
                ? "opacity-100 translate-y-0"
                : "opacity-0 translate-y-6 absolute pointer-events-none select-none"
            )}
          >
            <p className="section-label">{slide.label}</p>
            <h1 className="mt-5 text-5xl sm:text-6xl lg:text-7xl text-[var(--color-text-primary)] leading-[1.05] tracking-tight">
              {slide.heading}{" "}
              <em className="gradient-text not-italic font-display italic">{slide.accent}</em>
              {slide.tail && <> {slide.tail}</>}
            </h1>
            <p className="mt-6 text-lg text-[var(--color-text-secondary)] leading-relaxed max-w-lg">
              {slide.sub}
            </p>
            <div className="mt-10 flex flex-wrap gap-4">
              <Link href={slide.cta.href}>
                <GlassButton size="lg">
                  {slide.cta.label}
                  <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
                </GlassButton>
              </Link>
              <Link href={slide.secondary.href}>
                <GlassButton size="lg" variant="secondary">
                  {slide.secondary.label}
                </GlassButton>
              </Link>
            </div>
          </div>
        ))}

        {/* ── Stats strip (always visible) ── */}
        <div className="mt-14 flex gap-10 relative z-20">
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

      {/* ── Prev / Next arrows ── */}
      <button
        onClick={prev}
        aria-label="Previous slide"
        className="absolute z-30 left-3 sm:left-5 top-1/2 -translate-y-1/2 glass glass-sm p-2.5 text-[var(--color-text-secondary)] hover:text-[var(--color-gold)] hover:border-[var(--color-gold)]/30 transition-colors"
      >
        <ChevronLeft className="h-5 w-5" aria-hidden="true" />
      </button>
      <button
        onClick={next}
        aria-label="Next slide"
        className="absolute z-30 right-3 sm:right-5 top-1/2 -translate-y-1/2 glass glass-sm p-2.5 text-[var(--color-text-secondary)] hover:text-[var(--color-gold)] hover:border-[var(--color-gold)]/30 transition-colors"
      >
        <ChevronRight className="h-5 w-5" aria-hidden="true" />
      </button>

      {/* ── Dot indicators ── */}
      <div
        role="tablist"
        aria-label="Slide navigation"
        className="absolute z-30 bottom-8 left-1/2 -translate-x-1/2 flex items-center gap-2"
      >
        {slides.map((slide, i) => (
          <button
            key={i}
            role="tab"
            aria-selected={i === current}
            aria-label={`Go to slide ${i + 1}: ${slide.label}`}
            onClick={() => { if (timerRef.current) clearInterval(timerRef.current); go(i); }}
            className={cn(
              "h-1.5 rounded-full transition-all duration-300 ease-out",
              i === current
                ? "w-8 bg-[var(--color-gold)]"
                : "w-2 bg-[rgba(255,255,255,0.35)] hover:bg-[rgba(255,255,255,0.55)]"
            )}
          />
        ))}
      </div>

      {/* ── Auto-advance progress bar ── */}
      {!paused && (
        <div
          aria-hidden="true"
          className="absolute z-30 bottom-0 left-0 right-0 h-[2px] bg-[rgba(255,255,255,0.08)]"
        >
          <div
            key={key}
            className="h-full bg-[var(--color-gold)] carousel-progress"
          />
        </div>
      )}
    </section>
  );
}
