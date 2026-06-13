import Link from "next/link";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { Star } from "lucide-react";
import { ProductGallery } from "@/components/storefront/product-gallery";
import { ProductActions } from "@/components/storefront/product-actions";
import { ProductCard } from "@/components/storefront/product-card";
import { GlassCard } from "@/components/ui";
import { getProductBySlug, getRelatedProducts } from "@/lib/catalog";
import { formatPrice } from "@/lib/utils";
import { auth } from "@/lib/auth";
import { ReviewForm } from "@/components/storefront/review-form";

interface PDPProps {
  params: Promise<{ slug: string }>;
}

export async function generateMetadata({ params }: PDPProps): Promise<Metadata> {
  const { slug } = await params;
  const product = await getProductBySlug(slug);
  if (!product) return { title: "Product not found" };
  return {
    title: product.seoTitle ?? product.name,
    description: product.seoDescription ?? product.description.slice(0, 160),
    openGraph: { images: product.images.slice(0, 1) },
  };
}

export default async function ProductPage({ params }: PDPProps) {
  const { slug } = await params;
  const product = await getProductBySlug(slug);
  if (!product) notFound();

  const [related, session] = await Promise.all([
    getRelatedProducts(product.id, product.categoryId, 4),
    auth(),
  ]);
  const ratings = product.reviews.map((r) => r.rating);
  const avgRating = ratings.length
    ? Math.round((ratings.reduce((a, b) => a + b, 0) / ratings.length) * 10) / 10
    : null;

  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "Product",
    name: product.name,
    image: product.images,
    description: product.description.slice(0, 300),
    offers: {
      "@type": "Offer",
      priceCurrency: "INR",
      price: (product.price / 100).toFixed(2),
      availability: product.variants.some((v) => v.stock > 0)
        ? "https://schema.org/InStock"
        : "https://schema.org/OutOfStock",
    },
    ...(avgRating && {
      aggregateRating: {
        "@type": "AggregateRating",
        ratingValue: avgRating,
        reviewCount: ratings.length,
      },
    }),
  };

  return (
    <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 py-10">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="text-xs text-[var(--color-text-muted)]">
        <ol className="flex flex-wrap gap-1.5" role="list">
          <li><Link href="/" className="hover:text-[var(--color-gold)]">Home</Link></li>
          <li aria-hidden="true">/</li>
          <li>
            <Link href={`/shop?category=${product.category.slug}`} className="hover:text-[var(--color-gold)]">
              {product.category.name}
            </Link>
          </li>
          <li aria-hidden="true">/</li>
          <li aria-current="page" className="text-[var(--color-text-secondary)]">{product.name}</li>
        </ol>
      </nav>

      <div className="mt-6 grid gap-10 lg:grid-cols-2">
        <ProductGallery images={product.images} name={product.name} />

        <div>
          <h1 className="text-3xl sm:text-4xl text-[var(--color-text-primary)]">{product.name}</h1>

          {avgRating && (
            <p className="mt-2 flex items-center gap-1.5 text-sm text-[var(--color-text-secondary)]">
              <Star className="h-4 w-4 fill-[var(--color-gold)] text-[var(--color-gold)]" aria-hidden="true" />
              {avgRating} · {ratings.length} review{ratings.length === 1 ? "" : "s"}
            </p>
          )}

          {product.comparePrice && product.comparePrice > product.price && (
            <p className="mt-2 text-sm text-[var(--color-text-muted)]">
              MRP <s>{formatPrice(product.comparePrice)}</s>{" "}
              <span className="text-[var(--color-success)]">
                ({Math.round(((product.comparePrice - product.price) / product.comparePrice) * 100)}% off)
              </span>
            </p>
          )}

          <div className="mt-6">
            <ProductActions
              productId={product.id}
              productName={product.name}
              basePrice={product.price}
              baseImage={product.images[0] ?? ""}
              variants={product.variants.map((v) => ({
                id: v.id,
                color: v.color,
                size: v.size,
                fabric: v.fabric,
                stock: v.stock,
                price: v.price,
                images: v.images,
              }))}
            />
          </div>

          {/* Description */}
          <GlassCard padding="md" className="mt-8">
            <h2 className="text-base font-semibold !font-[var(--font-body)] text-[var(--color-text-primary)]">
              Description
            </h2>
            <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-[var(--color-text-secondary)]">
              {product.description}
            </p>
          </GlassCard>

          {/* FAQs */}
          {product.faqs.length > 0 && (
            <div className="mt-6 flex flex-col gap-3">
              <h2 className="text-base font-semibold !font-[var(--font-body)] text-[var(--color-text-primary)]">
                Frequently asked
              </h2>
              {product.faqs.map((f) => (
                <details key={f.id} className="glass glass-sm group p-4">
                  <summary className="cursor-pointer text-sm font-medium text-[var(--color-text-primary)] list-none">
                    {f.question}
                  </summary>
                  <p className="mt-2 text-sm text-[var(--color-text-secondary)]">{f.answer}</p>
                </details>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Reviews */}
      <section aria-labelledby="reviews-heading" className="mt-16">
        <h2 id="reviews-heading" className="text-2xl text-[var(--color-text-primary)]">
          Customer Reviews
        </h2>
        {session && (
          <div className="mt-6">
            <ReviewForm productId={product.id} />
          </div>
        )}
        {product.reviews.length > 0 && (
          <ul className="mt-6 grid gap-4 md:grid-cols-2" role="list">
            {product.reviews.map((r) => (
              <li key={r.id}>
                <GlassCard padding="md" className="h-full">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-[var(--color-text-primary)]">{r.user.name}</p>
                    <p className="flex gap-0.5" aria-label={`Rated ${r.rating} out of 5`}>
                      {Array.from({ length: 5 }, (_, i) => (
                        <Star
                          key={i}
                          aria-hidden="true"
                          className={
                            i < r.rating
                              ? "h-3.5 w-3.5 fill-[var(--color-gold)] text-[var(--color-gold)]"
                              : "h-3.5 w-3.5 text-[var(--color-text-muted)]"
                          }
                        />
                      ))}
                    </p>
                  </div>
                  {r.isVerifiedPurchase && (
                    <p className="mt-1 text-xs text-[var(--color-success)]">✓ Verified purchase</p>
                  )}
                  {r.title && (
                    <p className="mt-2 text-sm font-medium text-[var(--color-text-primary)]">{r.title}</p>
                  )}
                  {r.body && (
                    <p className="mt-1 text-sm text-[var(--color-text-secondary)]">{r.body}</p>
                  )}
                </GlassCard>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Related */}
      {related.length > 0 && (
        <section aria-labelledby="related-heading" className="mt-16">
          <h2 id="related-heading" className="text-2xl text-[var(--color-text-primary)]">
            You may also like
          </h2>
          <ul className="mt-6 grid grid-cols-2 gap-4 sm:gap-6 lg:grid-cols-4" role="list">
            {related.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} />
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
