import Link from "next/link";
import Image from "next/image";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge, GlassButton } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import { Plus } from "lucide-react";

export const metadata = { title: "Products | Admin" };

export default async function AdminProductsPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; category?: string }>;
}) {
  const { q, category } = await searchParams;

  const where: Record<string, unknown> = {};
  if (q) where.name = { contains: q, mode: "insensitive" };
  if (category) where.categoryId = category;

  const [products, categories] = await Promise.all([
    db.product.findMany({
      where,
      include: {
        category: { select: { name: true } },
        variants: { select: { stock: true, isActive: true } },
      },
      orderBy: { createdAt: "desc" },
      take: 50,
    }),
    db.category.findMany({ where: { isActive: true }, orderBy: { name: "asc" } }),
  ]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Products</h2>
        <Link href="/admin/products/new">
          <GlassButton size="sm">
            <Plus className="h-4 w-4 mr-1.5" aria-hidden="true" />
            New product
          </GlassButton>
        </Link>
      </div>

      {/* Filter bar */}
      <form method="GET" className="flex gap-2">
        <input name="q" defaultValue={q} placeholder="Search products…" className="glass-input text-sm flex-1 max-w-xs" />
        <select name="category" defaultValue={category ?? ""} className="glass-input text-sm">
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
        <button type="submit" className="glass px-3 py-2 text-sm rounded-lg text-[var(--color-text-primary)]">
          Filter
        </button>
      </form>

      <GlassCard padding="md">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Products table">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                {["Product", "Category", "Price", "Stock", "Status", ""].map((h) => (
                  <th key={h} scope="col" className="pb-2 pr-4 last:pr-0 text-left text-xs font-medium text-[var(--color-text-muted)]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {products.map((product) => {
                const totalStock = product.variants.reduce((s, v) => s + (v.isActive ? v.stock : 0), 0);
                const isOutOfStock = product.variants.every((v) => v.stock === 0);
                return (
                  <tr key={product.id} className="border-b border-[var(--color-glass-border)] last:border-0 hover:bg-[var(--color-glass-bg)] transition-colors">
                    <td className="py-3 pr-4">
                      <div className="flex items-center gap-3">
                        {product.images[0] && (
                          <div className="relative h-10 w-10 shrink-0 overflow-hidden rounded-lg">
                            <Image
                              src={product.images[0]}
                              alt={product.name}
                              fill
                              className="object-cover"
                              sizes="40px"
                            />
                          </div>
                        )}
                        <div>
                          <p className="font-medium text-[var(--color-text-primary)]">
                            {product.name}
                          </p>
                          <p className="text-xs text-[var(--color-text-muted)]">
                            {product.variants.length} variants
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 pr-4 text-[var(--color-text-secondary)]">
                      {product.category.name}
                    </td>
                    <td className="py-3 pr-4 font-semibold text-[var(--color-gold)]">
                      {formatPrice(product.price)}
                    </td>
                    <td className="py-3 pr-4">
                      <span className={isOutOfStock ? "text-[var(--color-error)]" : "text-[var(--color-text-primary)]"}>
                        {totalStock}
                      </span>
                    </td>
                    <td className="py-3 pr-4">
                      <GlassBadge variant={product.isActive ? "success" : "neutral"}>
                        {product.isActive ? "Active" : "Draft"}
                      </GlassBadge>
                    </td>
                    <td className="py-3">
                      <Link href={`/admin/products/${product.id}`} className="text-xs text-[var(--color-gold)] hover:underline">
                        Edit →
                      </Link>
                    </td>
                  </tr>
                );
              })}
              {products.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                    No products found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </GlassCard>
    </div>
  );
}
