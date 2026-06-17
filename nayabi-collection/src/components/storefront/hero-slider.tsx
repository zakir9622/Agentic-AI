"use client";

import * as React from "react";
import Link from "next/link";
import Image from "next/image";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { formatPrice } from "@/lib/utils";

export interface HeroSlide {
  src: string;
  name: string;
  slug: string;
  price: number;
  category: string;
}

/**
 * Auto-advancing product-image slider for the hero. Replaces the static
 * full-bleed background so the right side showcases real product imagery.
 * Pauses on hover/focus, supports arrows + dots, and respects reduced-motion.
 */
export function HeroSlider({ slides }: { slides: HeroSlide[] }) {
  const [index, setIndex] = React.useState(0);
  const [paused, setPaused] = React.useState(false);
  const count = slides.length;

  const go = React.useCallback(
    (n: number) => setIndex(((n % count) + count) % count),
    [count]
  );

  React.useEffect(() => {
    if (count <= 1 || paused) return;
    const reduce =
      typeof window !== "undefined" &&
      window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    if (reduce) return;
    const id = window.setInterval(() => setIndex((i) => (i + 1) % count), 4500);
    return () => window.clearInterval(id);
  }, [count, paused]);

  if (count === 0) return null;

  return (
    <div
      className="group glass glass-elevated glass-lg relative aspect-[4/5] w-full overflow-hidden !p-0 sm:aspect-[5/6]"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocusCapture={() => setPaused(true)}
      onBlurCapture={() => setPaused(false)}
      aria-roledescription="carousel"
      aria-label="Featured products"
    >
      {slides.map((s, i) => (
        <Link
          key={s.slug + i}
          href={`/products/${s.slug}`}
          aria-hidden={i !== index}
          tabIndex={i === index ? 0 : -1}
          className={[
            // h-full/w-full make the height definite so the fill image (height:100%)
            // resolves — an `absolute inset-0` parent alone leaves it indefinite.
            "absolute inset-0 block h-full w-full transition-opacity duration-700 ease-[cubic-bezier(0.4,0,0.2,1)]",
            i === index ? "opacity-100" : "pointer-events-none opacity-0",
          ].join(" ")}
        >
          <Image
            src={s.src}
            alt={s.name}
            fill
            sizes="(max-width: 1024px) 100vw, 45vw"
            priority={i === 0}
            className="object-cover"
          />
          {/* Caption scrim */}
          <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-[rgba(4,2,12,0.92)] via-[rgba(4,2,12,0.35)] to-transparent p-5 sm:p-6">
            <p className="text-[0.625rem] font-semibold tracking-widest text-[var(--color-gold)] uppercase">
              {s.category}
            </p>
            <p className="mt-1 font-display text-lg text-[var(--color-text-primary)] sm:text-xl">
              {s.name}
            </p>
            <p className="mt-0.5 text-sm font-semibold text-[var(--color-gold)]">
              {formatPrice(s.price)}
            </p>
          </div>
        </Link>
      ))}

      {count > 1 && (
        <>
          {/* Arrows — symmetric, glass, vertically centred; auto-hide until
              hover/keyboard focus so they don't clutter the imagery. */}
          <button
            type="button"
            onClick={() => go(index - 1)}
            aria-label="Previous product"
            className="glass glass-sm absolute top-1/2 left-3 z-10 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full text-[var(--color-text-primary)] opacity-0 transition-all duration-200 hover:text-[var(--color-gold)] focus-visible:opacity-100 group-hover:opacity-100 group-focus-within:opacity-100"
          >
            <ChevronLeft className="h-5 w-5" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={() => go(index + 1)}
            aria-label="Next product"
            className="glass glass-sm absolute top-1/2 right-3 z-10 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full text-[var(--color-text-primary)] opacity-0 transition-all duration-200 hover:text-[var(--color-gold)] focus-visible:opacity-100 group-hover:opacity-100 group-focus-within:opacity-100"
          >
            <ChevronRight className="h-5 w-5" aria-hidden="true" />
          </button>

          {/* Dots — always visible (they convey position) */}
          <div className="absolute bottom-3 left-1/2 z-10 flex -translate-x-1/2 items-center gap-1.5 rounded-full bg-[rgba(4,2,12,0.28)] px-2.5 py-1.5 backdrop-blur-sm">
            {slides.map((s, i) => (
              <button
                key={s.slug + "-dot-" + i}
                type="button"
                onClick={() => setIndex(i)}
                aria-label={`Go to slide ${i + 1}`}
                aria-current={i === index}
                className={[
                  "h-1.5 rounded-full transition-all duration-300",
                  i === index
                    ? "w-5 bg-[var(--color-gold)]"
                    : "w-1.5 bg-white/60 hover:bg-[var(--color-gold)]",
                ].join(" ")}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
