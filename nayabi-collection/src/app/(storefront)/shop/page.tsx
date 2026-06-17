import Link from "next/link";
import type { Metadata } from "next";
import { ProductCard } from "@/components/storefront/product-card";
import { ShopFilters } from "@/components/storefront/shop-filters";
import { getProducts, getFilterFacets, getActiveCategories } from "@/lib/catalog";

export const metadata: Metadata = {
  title: "Shop All",
  description:
    "Browse our full collection of premium Hijabs, Abayas and Namaz Scarfs.",
};

interface ShopPageProps {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

export default async function ShopPage({ searchParams }: ShopPageProps) {
  const sp = await searchParams;
  const str = (v: string | string[] | undefined) => (Array.isArray(v) ? v[0] : v);

  const filters = {
    category: str(sp.category),
    color: str(sp.color),
    size: str(sp.size),
    fabric: str(sp.fabric),
    q: str(sp.q),
    sort: (str(sp.sort) as "newest" | "price-asc" | "price-desc") ?? "newest",
    page: Number(str(sp.page)) || 1,
  };

  const [result, facets, categories] = await Promise.all([
    getProducts(filters),
    getFilterFacets(),
    getActiveCategories(),
  ]);

  const activeCategory = categories.find((c) => c.slug === filters.category);

  return (
    <div className="mesh-bg min-h-screen">
      <div className="relative z-10 mx-auto max-w-7xl px-4 py-10 sm:px-6 sm:py-12 lg:px-10">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="text-xs text-[var(--color-text-muted)]">
        <ol className="flex gap-1.5" role="list">
          <li>
            <Link href="/" className="hover:text-[var(--color-gold)]">Home</Link>
          </li>
          <li aria-hidden="true">/</li>
          <li aria-current="page" className="text-[var(--color-text-secondary)]">
            {activeCategory?.name ?? "Shop All"}
          </li>
        </ol>
      </nav>

      <p className="section-label mt-5">Catalogue</p>
      <h1 className="mt-2 text-3xl text-[var(--color-text-primary)] sm:text-4xl">
        {filters.q
          ? `Search: "${filters.q}"`
          : (activeCategory?.name ?? "All Products")}
      </h1>
      <p className="mt-2 text-sm text-[var(--color-text-secondary)]" aria-live="polite">
        {result.total} {result.total === 1 ? "product" : "products"}
      </p>

      <div className="mt-8 grid gap-10 lg:grid-cols-[240px_1fr]">
        <aside aria-label="Product filters">
          <ShopFilters
            facets={{
              ...facets,
              categories: categories.map((c) => ({ name: c.name, slug: c.slug })),
            }}
          />
        </aside>

        <div>
          {result.items.length === 0 ? (
            <div className="glass glass-md p-12 text-center">
              <p className="font-display text-xl text-[var(--color-text-primary)]">
                No products found
              </p>
              <p className="mt-2 text-sm text-[var(--color-text-secondary)]">
                Try adjusting your filters or{" "}
                <Link href="/shop" className="text-[var(--color-gold)] underline underline-offset-2">
                  browse everything
                </Link>
                .
              </p>
            </div>
          ) : (
            <>
              <ul className="stagger-enter grid grid-cols-2 gap-4 sm:gap-6 xl:grid-cols-3" role="list">
                {result.items.map((p) => (
                  <li key={p.id}>
                    <ProductCard product={p} />
                  </li>
                ))}
              </ul>

              {/* Pagination */}
              {result.totalPages > 1 && (
                <nav aria-label="Pagination" className="mt-10 flex justify-center gap-2">
                  {Array.from({ length: result.totalPages }, (_, i) => i + 1).map((n) => {
                    const next = new URLSearchParams();
                    Object.entries(filters).forEach(([k, v]) => {
                      if (v && k !== "page" && v !== "newest") next.set(k, String(v));
                    });
                    if (n > 1) next.set("page", String(n));
                    return (
                      <Link
                        key={n}
                        href={`/shop?${next.toString()}`}
                        aria-current={n === result.page ? "page" : undefined}
                        className={
                          n === result.page
                            ? "rounded-lg bg-[var(--color-gold)] px-3.5 py-1.5 text-sm font-medium text-[var(--color-text-inverse)]"
                            : "glass glass-sm rounded-lg px-3.5 py-1.5 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"
                        }
                      >
                        {n}
                      </Link>
                    );
                  })}
                </nav>
              )}
            </>
          )}
        </div>
      </div>
      </div>
    </div>
  );
}
