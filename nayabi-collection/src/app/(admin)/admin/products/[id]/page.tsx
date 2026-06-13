import { notFound } from "next/navigation";
import { db } from "@/lib/db";
import { ProductForm } from "./product-form";

export const metadata = { title: "Edit Product | Admin" };

export default async function EditProductPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  const [product, categories] = await Promise.all([
    db.product.findUnique({
      where: { id },
      include: {
        variants: {
          orderBy: { createdAt: "asc" },
        },
      },
    }),
    db.category.findMany({ where: { isActive: true }, orderBy: { name: "asc" } }),
  ]);

  if (!product) notFound();

  return (
    <div className="flex flex-col gap-4">
      <div>
        <a href="/admin/products" className="text-xs text-[var(--color-text-muted)] hover:text-[var(--color-gold)]">
          ← Products
        </a>
        <h2 className="mt-1 text-2xl font-semibold text-[var(--color-text-primary)]">
          {product.name}
        </h2>
      </div>
      <ProductForm product={product} categories={categories} />
    </div>
  );
}
