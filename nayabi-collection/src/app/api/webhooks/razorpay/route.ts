import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { verifyWebhookSignature } from "@/lib/razorpay";
import { confirmOrder } from "@/lib/orders";

export async function POST(req: NextRequest) {
  const rawBody = await req.text();
  const signature = req.headers.get("x-razorpay-signature") ?? "";

  if (!verifyWebhookSignature(rawBody, signature)) {
    return NextResponse.json({ error: "Invalid signature" }, { status: 401 });
  }

  try {
    const event = JSON.parse(rawBody) as {
      event: string;
      payload: {
        payment?: { entity: { id: string; order_id: string; method?: string; error_description?: string } };
        refund?: { entity: { id: string; payment_id: string } };
      };
    };

    switch (event.event) {
      case "payment.captured": {
        const p = event.payload.payment!.entity;
        const payment = await db.payment.findUnique({ where: { razorpayOrderId: p.order_id } });
        if (payment && payment.status !== "PAID") {
          const method =
            p.method === "card" ? "CARD"
            : p.method === "netbanking" ? "NETBANKING"
            : p.method === "wallet" ? "WALLET"
            : "UPI";
          await db.payment.update({
            where: { id: payment.id },
            data: { razorpayPaymentId: p.id, status: "PAID", paidAt: new Date(), method },
          });
          await db.order.update({
            where: { id: payment.orderId },
            data: { paymentStatus: "PAID", paymentMethod: method },
          });
          await confirmOrder(payment.orderId);
        }
        break;
      }
      case "payment.failed": {
        const p = event.payload.payment!.entity;
        const payment = await db.payment.findUnique({ where: { razorpayOrderId: p.order_id } });
        if (payment && payment.status === "PENDING") {
          await db.payment.update({
            where: { id: payment.id },
            data: { status: "FAILED", failureReason: p.error_description ?? "Payment failed" },
          });
          await db.order.update({
            where: { id: payment.orderId },
            data: { paymentStatus: "FAILED" },
          });
          await db.notification.create({
            data: {
              type: "PAYMENT_FAILED",
              title: "Payment failed",
              message: `Razorpay order ${p.order_id}: ${p.error_description ?? "unknown error"}`,
              entityId: payment.orderId,
              entityType: "Order",
            },
          });
        }
        break;
      }
      case "refund.processed": {
        const r = event.payload.refund!.entity;
        await db.refund.updateMany({
          where: { razorpayRefundId: r.id, status: "PENDING" },
          data: { status: "PROCESSED", processedAt: new Date() },
        });
        break;
      }
    }

    return NextResponse.json({ received: true });
  } catch (e) {
    console.error("Razorpay webhook error:", e);
    return NextResponse.json({ error: "Webhook processing failed" }, { status: 500 });
  }
}
