import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { formatPrice } from "@/lib/utils";
import { sendAbandonedCartEmail } from "@/lib/mail";
import { sendWhatsApp } from "@/lib/msg91";

/* Recovery sequence delays (ms after cart creation) */
const SEQUENCE: { seq: 1 | 2 | 3; afterMs: number; discountCode?: string }[] = [
  { seq: 1, afterMs: 1 * 60 * 60 * 1000 },        // 1 hour
  { seq: 2, afterMs: 24 * 60 * 60 * 1000 },       // 24 hours
  { seq: 3, afterMs: 48 * 60 * 60 * 1000, discountCode: "COMEBACK5" }, // 48 hours
];

/** Vercel Cron target — schedule e.g. every 30 minutes.
    Protected by CRON_SECRET bearer token. */
export async function GET(req: NextRequest) {
  const auth = req.headers.get("authorization");
  if (process.env.CRON_SECRET && auth !== `Bearer ${process.env.CRON_SECRET}`) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  try {
    const carts = await db.abandonedCart.findMany({
      where: { convertedAt: null, email: { not: null } },
      include: {
        reminderEmails: true,
        cart: {
          include: {
            items: { include: { variant: { include: { product: true } } } },
          },
        },
      },
      take: 100,
    });

    let sent = 0;
    const now = Date.now();

    for (const ac of carts) {
      if (ac.cart.items.length === 0) continue;
      const age = now - ac.createdAt.getTime();
      const sentSeqs = new Set(ac.reminderEmails.filter((r) => r.channel === "email").map((r) => r.sequence));

      const due = SEQUENCE.filter((s) => age >= s.afterMs && !sentSeqs.has(s.seq)).at(-1);
      if (!due) continue;

      const items = ac.cart.items.map((i) => ({
        name: i.variant.product.name,
        price: formatPrice(i.variant.price ?? i.variant.product.price),
      }));

      await sendAbandonedCartEmail({
        to: ac.email!,
        sequence: due.seq,
        items,
        discountCode: due.discountCode,
      });
      await db.abandonedCartEmail.create({
        data: { abandonedCartId: ac.id, sequence: due.seq, channel: "email" },
      });

      if (ac.phone) {
        await sendWhatsApp(ac.phone, "abandoned_cart", [
          items[0].name,
          due.discountCode ?? "",
        ]);
        await db.abandonedCartEmail.create({
          data: { abandonedCartId: ac.id, sequence: due.seq, channel: "whatsapp" },
        });
      }
      sent++;
    }

    return NextResponse.json({ success: true, processed: carts.length, sent });
  } catch (e) {
    console.error("Abandoned cart cron failed:", e);
    return NextResponse.json({ success: false }, { status: 500 });
  }
}
