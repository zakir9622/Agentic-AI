import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { nanoid } from "nanoid";
import { db } from "@/lib/db";

const schema = z.object({
  email: z.email(),
  phone: z.string().optional(),
  items: z
    .array(z.object({ variantId: z.string(), quantity: z.number().int().min(1) }))
    .min(1)
    .max(50),
});

/** Called when a shopper enters contact details at checkout but hasn't paid yet.
    Records the cart so the recovery sequence can fire if they never complete. */
export async function POST(req: NextRequest) {
  try {
    const parsed = schema.safeParse(await req.json());
    if (!parsed.success)
      return NextResponse.json({ success: false }, { status: 400 });
    const { email, phone, items } = parsed.data;

    const existing = await db.abandonedCart.findFirst({
      where: { email, convertedAt: null },
      include: { cart: true },
    });

    if (existing) {
      await db.cartItem.deleteMany({ where: { cartId: existing.cartId } });
      await db.cartItem.createMany({
        data: items.map((i) => ({ cartId: existing.cartId, ...i })),
        skipDuplicates: true,
      });
      await db.cart.update({ where: { id: existing.cartId }, data: { updatedAt: new Date() } });
    } else {
      const cart = await db.cart.create({
        data: {
          sessionId: `abandon_${nanoid()}`,
          items: { create: items },
        },
      });
      await db.abandonedCart.create({
        data: { cartId: cart.id, email, phone },
      });
    }

    return NextResponse.json({ success: true });
  } catch {
    return NextResponse.json({ success: false }, { status: 500 });
  }
}
