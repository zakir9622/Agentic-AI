import { NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin-auth";
import { db } from "@/lib/db";

export async function GET() {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const variants = await db.productVariant.findMany({
    include: { product: { select: { id: true, name: true } } },
    orderBy: [{ product: { name: "asc" } }, { sku: "asc" }],
  });

  const formatted = variants.map((v) => ({
    id: v.id,
    productId: v.product.id,
    productName: v.product.name,
    sku: v.sku,
    color: v.color,
    size: v.size,
    fabric: v.fabric,
    stock: v.stock,
    lowStockThreshold: v.lowStockThreshold,
  }));

  return NextResponse.json(formatted);
}
