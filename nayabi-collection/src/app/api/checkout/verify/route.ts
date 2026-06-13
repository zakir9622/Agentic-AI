import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { db } from "@/lib/db";
import { verifyPaymentSignature } from "@/lib/razorpay";
import { confirmOrder } from "@/lib/orders";

const schema = z.object({
  razorpayOrderId: z.string().min(1),
  razorpayPaymentId: z.string().min(1),
  razorpaySignature: z.string().min(1),
});

export async function POST(req: NextRequest) {
  try {
    const parsed = schema.safeParse(await req.json());
    if (!parsed.success)
      return NextResponse.json({ success: false, error: "Invalid request" }, { status: 400 });

    const { razorpayOrderId, razorpayPaymentId, razorpaySignature } = parsed.data;

    if (!verifyPaymentSignature({ razorpayOrderId, razorpayPaymentId, razorpaySignature })) {
      return NextResponse.json(
        { success: false, error: "Payment verification failed" },
        { status: 400 }
      );
    }

    const payment = await db.payment.findUnique({
      where: { razorpayOrderId },
      include: { order: { select: { id: true, orderNumber: true } } },
    });
    if (!payment)
      return NextResponse.json({ success: false, error: "Order not found" }, { status: 404 });

    if (payment.status !== "PAID") {
      await db.payment.update({
        where: { id: payment.id },
        data: {
          razorpayPaymentId,
          razorpaySignature,
          status: "PAID",
          paidAt: new Date(),
        },
      });
      await db.order.update({
        where: { id: payment.orderId },
        data: { paymentStatus: "PAID" },
      });
      await confirmOrder(payment.orderId);
    }

    return NextResponse.json({ success: true, orderNumber: payment.order.orderNumber });
  } catch (e) {
    console.error("Payment verify failed:", e);
    return NextResponse.json({ success: false, error: "Server error" }, { status: 500 });
  }
}
