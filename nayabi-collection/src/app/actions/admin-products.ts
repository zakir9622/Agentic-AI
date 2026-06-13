"use server";

import { z } from "zod";
import { db } from "@/lib/db";
import { requireAdminSession } from "@/lib/admin-auth";
import { slugify } from "@/lib/utils";
import { revalidatePath } from "next/cache";

interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

/* ── Product CRUD ─────────────────────────────────────────────────────────────── */

const productSchema = z.object({
  name: z.string().min(2, "Name required"),
  slug: z.string().optional(),
  description: z.string().min(10, "Description required"),
  categoryId: z.string().min(1, "Category required"),
  price: z.coerce.number().min(1, "Price must be > 0"),
  comparePrice: z.coerce.number().optional(),
  seoTitle: z.string().optional(),
  seoDescription: z.string().optional(),
  isActive: z.string().optional(),
  tags: z.string().optional(),
  images: z.string().optional(),
});

export async function saveProductAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  await requireAdminSession();

  const productId = formData.get("productId") as string | null;
  const parsed = productSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const k = String(issue.path[0] ?? "form");
      if (!errors[k]) errors[k] = issue.message;
    }
    return { errors };
  }

  const { name, slug: rawSlug, description, categoryId, price, comparePrice,
    seoTitle, seoDescription, isActive: isActiveStr, tags: tagsStr, images: imagesStr } = parsed.data;

  const slug = rawSlug?.trim() || slugify(name);
  const isActive = isActiveStr === "on";
  const tags = tagsStr ? tagsStr.split(",").map((t) => t.trim()).filter(Boolean) : [];
  const images = imagesStr ? imagesStr.split("\n").map((u) => u.trim()).filter(Boolean) : [];

  const data = {
    name,
    slug,
    description,
    categoryId,
    price: Math.round(price),
    comparePrice: comparePrice ? Math.round(comparePrice) : null,
    seoTitle: seoTitle || null,
    seoDescription: seoDescription || null,
    isActive,
    tags,
    images,
  };

  if (productId) {
    await db.product.update({ where: { id: productId }, data });
  } else {
    await db.product.create({ data });
  }

  revalidatePath("/admin/products");
  revalidatePath("/shop");
  return { ok: true, message: productId ? "Product updated" : "Product created" };
}

export async function deleteProductAction(productId: string): Promise<ActionState> {
  await requireAdminSession();
  await db.product.delete({ where: { id: productId } });
  revalidatePath("/admin/products");
  revalidatePath("/shop");
  return { ok: true, message: "Product deleted" };
}

/* ── Variant CRUD ─────────────────────────────────────────────────────────────── */

const variantSchema = z.object({
  productId: z.string(),
  sku: z.string().min(1, "SKU required"),
  color: z.string().optional(),
  size: z.string().optional(),
  fabric: z.string().optional(),
  stock: z.coerce.number().min(0),
  lowStockThreshold: z.coerce.number().min(0).optional(),
  price: z.coerce.number().optional(),
  isActive: z.string().optional(),
});

export async function saveVariantAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  await requireAdminSession();

  const variantId = formData.get("variantId") as string | null;
  const parsed = variantSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const k = String(issue.path[0] ?? "form");
      if (!errors[k]) errors[k] = issue.message;
    }
    return { errors };
  }

  const { productId, sku, color, size, fabric, stock, lowStockThreshold, price, isActive: isActiveStr } = parsed.data;
  const isActive = isActiveStr !== "off";

  const data = {
    productId,
    sku,
    color: color || null,
    size: size || null,
    fabric: fabric || null,
    stock,
    lowStockThreshold: lowStockThreshold ?? 5,
    price: price ? Math.round(price) : null,
    isActive,
  };

  if (variantId) {
    await db.productVariant.update({ where: { id: variantId }, data });
  } else {
    await db.productVariant.create({ data });
  }

  revalidatePath(`/admin/products/${productId}`);
  return { ok: true, message: "Variant saved" };
}

export async function deleteVariantAction(variantId: string, productId: string): Promise<ActionState> {
  await requireAdminSession();
  await db.productVariant.delete({ where: { id: variantId } });
  revalidatePath(`/admin/products/${productId}`);
  return { ok: true, message: "Variant deleted" };
}

/* ── Category CRUD ───────────────────────────────────────────────────────────── */

const categorySchema = z.object({
  name: z.string().min(2, "Name required"),
  slug: z.string().optional(),
  description: z.string().optional(),
  isActive: z.string().optional(),
});

export async function saveCategoryAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  await requireAdminSession();

  const categoryId = formData.get("categoryId") as string | null;
  const parsed = categorySchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const k = String(issue.path[0] ?? "form");
      if (!errors[k]) errors[k] = issue.message;
    }
    return { errors };
  }

  const { name, slug: rawSlug, description, isActive: isActiveStr } = parsed.data;
  const slug = rawSlug?.trim() || slugify(name);
  const isActive = isActiveStr === "on";

  const data = { name, slug, description: description || null, isActive };

  if (categoryId) {
    await db.category.update({ where: { id: categoryId }, data });
  } else {
    await db.category.create({ data });
  }

  revalidatePath("/admin/categories");
  revalidatePath("/");
  return { ok: true, message: "Category saved" };
}

/* ── Inventory ───────────────────────────────────────────────────────────────── */

export async function updateStockAction(variantId: string, stock: number): Promise<ActionState> {
  await requireAdminSession();
  await db.productVariant.update({ where: { id: variantId }, data: { stock } });
  revalidatePath("/admin/inventory");
  return { ok: true, message: "Stock updated" };
}

/* ── Discounts ───────────────────────────────────────────────────────────────── */

const discountSchema = z.object({
  code: z.string().min(3, "Code must be at least 3 chars").toUpperCase(),
  type: z.enum(["PERCENT", "FIXED", "FREE_SHIPPING"]),
  value: z.coerce.number().min(0),
  minOrderAmount: z.coerce.number().optional(),
  maxDiscountAmount: z.coerce.number().optional(),
  usageLimit: z.coerce.number().optional(),
  perCustomerLimit: z.coerce.number().optional(),
  isFirstOrderOnly: z.string().optional(),
  isActive: z.string().optional(),
  startsAt: z.string().optional(),
  expiresAt: z.string().optional(),
});

export async function saveDiscountAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  await requireAdminSession();

  const discountId = formData.get("discountId") as string | null;
  const parsed = discountSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const k = String(issue.path[0] ?? "form");
      if (!errors[k]) errors[k] = issue.message;
    }
    return { errors };
  }

  const { code, type, value, minOrderAmount, maxDiscountAmount, usageLimit,
    perCustomerLimit, isFirstOrderOnly: firstStr, isActive: isActiveStr, startsAt, expiresAt } = parsed.data;

  const data = {
    code,
    type,
    value: Math.round(value),
    minOrderAmount: minOrderAmount ? Math.round(minOrderAmount) : null,
    maxDiscountAmount: maxDiscountAmount ? Math.round(maxDiscountAmount) : null,
    usageLimit: usageLimit ?? null,
    perCustomerLimit: perCustomerLimit ?? null,
    isFirstOrderOnly: firstStr === "on",
    isActive: isActiveStr === "on",
    startsAt: startsAt ? new Date(startsAt) : null,
    expiresAt: expiresAt ? new Date(expiresAt) : null,
  };

  if (discountId) {
    await db.discountCode.update({ where: { id: discountId }, data });
  } else {
    await db.discountCode.create({ data });
  }

  revalidatePath("/admin/discounts");
  return { ok: true, message: "Discount saved" };
}

export async function deleteDiscountAction(discountId: string): Promise<ActionState> {
  await requireAdminSession();
  await db.discountCode.update({ where: { id: discountId }, data: { isActive: false } });
  revalidatePath("/admin/discounts");
  return { ok: true, message: "Discount deactivated" };
}
