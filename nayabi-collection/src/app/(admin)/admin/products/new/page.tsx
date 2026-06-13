import { db } from "@/lib/db";
import { ProductForm } from "../[id]/product-form";

export const metadata = { title: "New Product | Admin" };

export default async function NewProductPage() {
  const categories = await db.category.findMany({
    where: { isActive: true },
    orderBy: { name: "asc" },
  });

  return (
    <div className="flex flex-col gap-4">
      <div>
        <a href="/admin/products" className="text-xs text-[var(--color-text-muted)] hover:text-[var(--color-gold)]">
          ← Products
        </a>
        <h2 className="mt-1 text-2xl font-semibold text-[var(--color-text-primary)]">
          New product
        </h2>
      </div>
      <ProductForm categories={categories} />
    </div>
  );
}
