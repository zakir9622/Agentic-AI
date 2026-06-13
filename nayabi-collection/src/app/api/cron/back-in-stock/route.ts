import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { sendSMS, sendWhatsApp } from "@/lib/msg91";
import { formatPrice } from "@/lib/utils";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  const authHeader = req.headers.get("authorization");
  if (authHeader !== `Bearer ${process.env.CRON_SECRET}`) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // Find variants that are now in stock and have pending alerts that haven't been notified
  const alerts = await db.backInStockAlert.findMany({
    where: { notifiedAt: null },
    include: {
      variant: {
        include: { product: { select: { name: true, slug: true, price: true } } },
      },
    },
  });

  // Filter to only variants currently in stock
  const inStock = alerts.filter((a) => a.variant.stock > 0);

  let notified = 0;
  for (const alert of inStock) {
    const { variant } = alert;
    const price = variant.price ?? variant.product.price;
    const productName = variant.product.name;
    const variantLabel = [variant.color, variant.size, variant.fabric].filter(Boolean).join(" / ");
    const label = variantLabel ? `${productName} (${variantLabel})` : productName;
    const priceStr = formatPrice(price);
    const url = `${process.env.NEXT_PUBLIC_APP_URL}/products/${variant.product.slug}`;

    try {
      if (alert.phone) {
        await sendSMS(alert.phone, process.env.MSG91_SMS_TEMPLATE_BACK_IN_STOCK ?? "", {
          var1: label,
          var2: priceStr,
          var3: url,
        });
        await sendWhatsApp(alert.phone, "back_in_stock", [label, priceStr, url]);
      }

      await db.backInStockAlert.update({
        where: { id: alert.id },
        data: { notifiedAt: new Date() },
      });
      notified++;
    } catch (e) {
      console.error("Back-in-stock notification failed:", e);
    }
  }

  return NextResponse.json({ processed: inStock.length, notified });
}
