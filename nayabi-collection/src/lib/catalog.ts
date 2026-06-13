import { db } from "@/lib/db";
import type { ProductCard } from "@/types";

/* Serializable product-card mapper shared by all listing surfaces */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function toCard(p: any): ProductCard {
  const totalStock = p.variants?.reduce((s: number, v: { stock: number }) => s + v.stock, 0) ?? 0;
  const ratings: number[] = p.reviews?.map((r: { rating: number }) => r.rating) ?? [];
  return {
    id: p.id,
    name: p.name,
    slug: p.slug,
    price: p.price,
    comparePrice: p.comparePrice,
    images: p.images,
    category: { name: p.category.name, slug: p.category.slug },
    rating: ratings.length
      ? Math.round((ratings.reduce((a, b) => a + b, 0) / ratings.length) * 10) / 10
      : undefined,
    reviewCount: ratings.length,
    isLowStock: totalStock > 0 && totalStock <= 5,
    isOutOfStock: totalStock === 0,
  };
}

const cardInclude = {
  category: { select: { name: true, slug: true } },
  variants: { select: { stock: true } },
  reviews: { where: { isApproved: true }, select: { rating: true } },
} as const;

export async function getActiveCategories() {
  try {
    return await db.category.findMany({
      where: { isActive: true, parentId: null },
      orderBy: { sortOrder: "asc" },
    });
  } catch {
    return [];
  }
}

export async function getNewArrivals(limit = 8): Promise<ProductCard[]> {
  try {
    const rows = await db.product.findMany({
      where: { isActive: true },
      include: cardInclude,
      orderBy: { createdAt: "desc" },
      take: limit,
    });
    return rows.map(toCard);
  } catch {
    return [];
  }
}

export async function getBestsellers(limit = 8): Promise<ProductCard[]> {
  try {
    const rows = await db.product.findMany({
      where: { isActive: true },
      include: { ...cardInclude, orderItems: { select: { quantity: true } } },
      take: 50,
    });
    return rows
      .map((p) => ({
        card: toCard(p),
        sold: p.orderItems.reduce((s, i) => s + i.quantity, 0),
      }))
      .sort((a, b) => b.sold - a.sold)
      .slice(0, limit)
      .map((x) => x.card);
  } catch {
    return [];
  }
}

export interface ShopFilters {
  category?: string;
  color?: string;
  size?: string;
  fabric?: string;
  minPrice?: number; // paise
  maxPrice?: number; // paise
  sort?: "newest" | "price-asc" | "price-desc" | "rating";
  page?: number;
  q?: string;
}

const PAGE_SIZE = 12;

export async function getProducts(filters: ShopFilters) {
  const page = Math.max(1, filters.page ?? 1);
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const where: any = { isActive: true };
    if (filters.category) where.category = { slug: filters.category };
    if (filters.q)
      where.OR = [
        { name: { contains: filters.q, mode: "insensitive" } },
        { description: { contains: filters.q, mode: "insensitive" } },
        { tags: { has: filters.q.toLowerCase() } },
      ];
    if (filters.minPrice != null || filters.maxPrice != null)
      where.price = {
        ...(filters.minPrice != null && { gte: filters.minPrice }),
        ...(filters.maxPrice != null && { lte: filters.maxPrice }),
      };
    const variantFilter = {
      ...(filters.color && { color: { equals: filters.color, mode: "insensitive" as const } }),
      ...(filters.size && { size: filters.size }),
      ...(filters.fabric && { fabric: { equals: filters.fabric, mode: "insensitive" as const } }),
    };
    if (Object.keys(variantFilter).length) where.variants = { some: variantFilter };

    const orderBy =
      filters.sort === "price-asc"
        ? { price: "asc" as const }
        : filters.sort === "price-desc"
          ? { price: "desc" as const }
          : { createdAt: "desc" as const };

    const [rows, total] = await Promise.all([
      db.product.findMany({
        where,
        include: cardInclude,
        orderBy,
        skip: (page - 1) * PAGE_SIZE,
        take: PAGE_SIZE,
      }),
      db.product.count({ where }),
    ]);

    return {
      items: rows.map(toCard),
      total,
      page,
      totalPages: Math.max(1, Math.ceil(total / PAGE_SIZE)),
    };
  } catch {
    return { items: [], total: 0, page, totalPages: 1 };
  }
}

export async function getProductBySlug(slug: string) {
  try {
    return await db.product.findUnique({
      where: { slug, isActive: true },
      include: {
        category: { select: { name: true, slug: true } },
        variants: { where: { isActive: true }, orderBy: { createdAt: "asc" } },
        faqs: { orderBy: { sortOrder: "asc" } },
        reviews: {
          where: { isApproved: true },
          include: { user: { select: { name: true } } },
          orderBy: { createdAt: "desc" },
          take: 10,
        },
      },
    });
  } catch {
    return null;
  }
}

export async function getRelatedProducts(
  productId: string,
  categoryId: string,
  limit = 4
): Promise<ProductCard[]> {
  try {
    const rows = await db.product.findMany({
      where: { isActive: true, categoryId, id: { not: productId } },
      include: cardInclude,
      take: limit,
    });
    return rows.map(toCard);
  } catch {
    return [];
  }
}

/* Filter facets for the shop sidebar */
export async function getFilterFacets() {
  try {
    const variants = await db.productVariant.findMany({
      where: { isActive: true, product: { isActive: true } },
      select: { color: true, size: true, fabric: true },
    });
    const uniq = (xs: (string | null)[]) =>
      [...new Set(xs.filter((x): x is string => !!x))].sort();
    return {
      colors: uniq(variants.map((v) => v.color)),
      sizes: uniq(variants.map((v) => v.size)),
      fabrics: uniq(variants.map((v) => v.fabric)),
    };
  } catch {
    return { colors: [], sizes: [], fabrics: [] };
  }
}
