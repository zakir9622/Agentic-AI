"use client";

import * as React from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";

export function ProductGallery({ images, name }: { images: string[]; name: string }) {
  const [active, setActive] = React.useState(0);

  if (images.length === 0) {
    return (
      <div className="glass glass-md flex aspect-[3/4] items-center justify-center !p-0 text-[var(--color-text-muted)]">
        No image available
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="glass glass-md relative aspect-[3/4] overflow-hidden !p-0 bg-[var(--color-bg-mid)]">
        <Image
          src={images[active]}
          alt={`${name} — image ${active + 1} of ${images.length}`}
          fill
          priority
          sizes="(max-width: 1024px) 100vw, 50vw"
          className="object-cover"
        />
      </div>

      {images.length > 1 && (
        <ul className="flex gap-2 overflow-x-auto" role="list" aria-label="Product images">
          {images.map((src, i) => (
            <li key={src}>
              <button
                onClick={() => setActive(i)}
                aria-label={`View image ${i + 1}`}
                aria-current={i === active}
                className={cn(
                  "relative h-20 w-16 shrink-0 overflow-hidden rounded-lg border transition-colors",
                  i === active
                    ? "border-[var(--color-gold)]"
                    : "border-[var(--color-glass-border)] opacity-70 hover:opacity-100"
                )}
              >
                <Image src={src} alt="" fill sizes="64px" className="object-cover" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
