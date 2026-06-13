import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { db } from "@/lib/db";
import { computePricing } from "@/lib/pricing";
import { validateDiscountCode } from "@/lib/discount";
import { createRazorpayOrder } from "@/lib/razorpay";
import { generateOrderNumber, isValidPincode, isValidPhone } from "@/lib/utils";
import { INDIAN_STATES } from "@/lib/constants";
import { confirmOrder } from "@/lib/orders";

const schema = z.object({
  items: z
    .array(z.object({ variantId: z.string().min(1), quantity: z.number().int().min(1).max(20) }))
    .min(1)
    .max(50),
  contact: z.object({
    name: z.string().min(2).max(100),
    email: z.email(),
    phone: z.string().refine(isValidPhone, "Invalid Indian mobile number"),
  }),
  address: z.object({
    line1: z.string().min(3).max(200),
    line2: z.string().max(200).optional(),
    city: z.string().min(2).max(100),
    state: z.enum(INDIAN_STATES),
    pincode: z.string().refine(isValidPincode, "Invalid pincode"),
  }),
  paymentMethod: z.enum(["RAZORPAY", "COD"]),
  discountCode: z.string().max(40).optional(),
  notes: z.string().max(500).optional(),
  giftWrap: z.boolean().optional(),
  giftMessage: z.string().max(300).optional(),
});

export async function POST(req: NextRequest) {
  try {
    const { checkoutRatelimit, ratelimitCheck } = await import("@/lib/ratelimit");
    const ip = req.headers.get("x-forwarded-for")?.split(",")[0] ?? "anon";
    const allowed = await ratelimitCheck(checkoutRatelimit, ip);
    if (!allowed) {
      return NextResponse.json({ success: false, error: "Too many requests. Please try again later." }, { status: 429 });
    }

    const parsed = schema.safeParse(await req.json());
    if (!parsed.success) {
      return NextResponse.json(
        { success: false, error: parsed.error.issues[0]?.message ?? "Invalid request" },
        { status: 400 }
      );
    }
    const data = parsed.data;

    // Load variants with live stock + prices (never trust client prices)
    const variants = await db.productVariant.findMany({
      where: { id: { in: data.items.map((i) => i.variantId) }, isActive: true },
      include: { product: { select: { id: true, name: true, price: true, images: true } } },
    });
    const byId = new Map(variants.map((v) => [v.id, v]));

    for (const item of data.items) {
      const v = byId.get(item.variantId);
      if (!v)
        return NextResponse.json(
          { success: false, error: "One of the items is no longer available" },
          { status: 409 }
        );
      if (v.stock < item.quantity)
        return NextResponse.json(
          { success: false, error: `Insufficient stock for ${v.product.name}` },
          { status: 409 }
        );
    }

    const lineItems = data.items.map((i) => {
      const v = byId.get(i.variantId)!;
      const unitPrice = v.price ?? v.product.price;
      return {
        productId: v.product.id,
        variantId: v.id,
        productName: v.product.name,
        variantDetails: [v.color, v.size, v.fabric].filter(Boolean).join(" · "),
        imageUrl: v.images[0] ?? v.product.images[0] ?? null,
        quantity: i.quantity,
        unitPrice,
        totalPrice: unitPrice * i.quantity,
      };
    });
    const subtotal = lineItems.reduce((s, i) => s + i.totalPrice, 0);

    // Discount
    let discountAmount = 0;
    let discountCodeId: string | undefined;
    let freeShipping = false;
    if (data.discountCode) {
      const check = await validateDiscountCode(data.discountCode, subtotal);
      if (!check.valid)
        return NextResponse.json({ success: false, error: check.reason }, { status: 400 });
      discountAmount = check.amount ?? 0;
      discountCodeId = check.discountCodeId;
      freeShipping = check.freeShipping ?? false;
    }

    const isCOD = data.paymentMethod === "COD";
    const pricing = computePricing({
      subtotal,
      discountAmount,
      isCOD,
      isGiftWrapped: data.giftWrap,
    });
    if (freeShipping) {
      pricing.total -= pricing.shippingCost;
      pricing.shippingCost = 0;
    }

    const orderNumber = generateOrderNumber();

    // Link user by email if an account exists; otherwise it's a guest order
    const user = await db.user.findUnique({ where: { email: data.contact.email } });

    const savedAddress = user
      ? await db.address.create({
          data: {
            userId: user.id,
            name: data.contact.name,
            phone: data.contact.phone,
            ...data.address,
          },
        })
      : null;

    const order = await db.order.create({
      data: {
        orderNumber,
        userId: user?.id,
        addressId: savedAddress?.id,
        guestEmail: user ? null : data.contact.email,
        guestPhone: data.contact.phone,
        subtotal: pricing.subtotal,
        discountAmount: pricing.discountAmount,
        shippingCost: pricing.shippingCost,
        codCharge: pricing.codCharge,
        taxAmount: pricing.taxAmount,
        total: pricing.total,
        paymentMethod: isCOD ? "COD" : "UPI", // refined on payment capture
        notes: data.notes,
        giftMessage: data.giftMessage,
        isGiftWrapped: data.giftWrap ?? false,
        discountCodeId,
        items: { create: lineItems },
      },
    });

    // Stash guest address in notes-free way: create address only for users; guests keep it on shipment later.
    // For guests, persist address inside payment record metadata is overkill — store as JSON note:
    if (!user) {
      await db.order.update({
        where: { id: order.id },
        data: {
          notes: [
            data.notes,
            `SHIP TO: ${data.contact.name}, ${data.address.line1}${data.address.line2 ? ", " + data.address.line2 : ""}, ${data.address.city}, ${data.address.state} ${data.address.pincode}, Ph: ${data.contact.phone}`,
          ]
            .filter(Boolean)
            .join("\n"),
        },
      });
    }

    if (isCOD) {
      await db.payment.create({
        data: { orderId: order.id, method: "COD", amount: pricing.total, status: "PENDING" },
      });
      await db.cODRecord.create({
        data: { orderId: order.id, expectedAmount: pricing.total },
      });
      await confirmOrder(order.id); // decrement stock, notify, mark CONFIRMED
      return NextResponse.json({ success: true, orderNumber, cod: true });
    }

    // Prepaid: create Razorpay order
    const rzpOrder = await createRazorpayOrder({ amount: pricing.total, receipt: orderNumber });
    await db.payment.create({
      data: {
        orderId: order.id,
        razorpayOrderId: rzpOrder.id,
        method: "UPI",
        amount: pricing.total,
        status: "PENDING",
      },
    });

    return NextResponse.json({
      success: true,
      orderNumber,
      razorpayOrderId: rzpOrder.id,
      amount: pricing.total,
      keyId: process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID,
      prefill: { name: data.contact.name, email: data.contact.email, contact: data.contact.phone },
    });
  } catch (e) {
    console.error("Checkout failed:", e);
    return NextResponse.json(
      { success: false, error: "Checkout failed. Please try again." },
      { status: 500 }
    );
  }
}
