import { db } from "@/lib/db";
import { formatPrice } from "@/lib/utils";
import { sendOrderConfirmationEmail } from "@/lib/mail";
import { notifyOrderPlaced } from "@/lib/msg91";

/** Confirm an order: decrement stock, record discount usage, mark abandoned
    cart converted, fire notifications. Idempotent via orderStatus guard. */
export async function confirmOrder(orderId: string) {
  const order = await db.order.findUnique({
    where: { id: orderId },
    include: { items: true, user: { select: { email: true, phone: true } } },
  });
  if (!order || order.orderStatus !== "PENDING") return;

  await db.$transaction(async (tx) => {
    for (const item of order.items) {
      await tx.productVariant.update({
        where: { id: item.variantId },
        data: { stock: { decrement: item.quantity } },
      });
    }
    if (order.discountCodeId) {
      await tx.discountCode.update({
        where: { id: order.discountCodeId },
        data: { usedCount: { increment: 1 } },
      });
      await tx.discountUsage.create({
        data: {
          discountCodeId: order.discountCodeId,
          orderId: order.id,
          userId: order.userId,
        },
      });
    }
    await tx.order.update({
      where: { id: order.id },
      data: { orderStatus: "CONFIRMED" },
    });
    await tx.notification.create({
      data: {
        type: "NEW_ORDER",
        title: "New order received",
        message: `${order.orderNumber} — ${formatPrice(order.total)} (${order.paymentMethod})`,
        entityId: order.id,
        entityType: "Order",
      },
    });
  });

  const email = order.user?.email ?? order.guestEmail;
  const phone = order.user?.phone ?? order.guestPhone;
  const isCOD = order.paymentMethod === "COD";

  // Mark abandoned cart converted (matched by email)
  if (email) {
    await db.abandonedCart
      .updateMany({
        where: { email, convertedAt: null },
        data: { convertedAt: new Date() },
      })
      .catch(() => {});
  }

  // Fire-and-forget notifications
  void Promise.allSettled([
    email
      ? sendOrderConfirmationEmail({
          to: email,
          orderNumber: order.orderNumber,
          items: order.items.map((i) => ({
            name: i.productName,
            qty: i.quantity,
            price: formatPrice(i.totalPrice),
          })),
          total: formatPrice(order.total),
          isCOD,
        })
      : Promise.resolve(),
    notifyOrderPlaced(phone, order.orderNumber, formatPrice(order.total), isCOD),
  ]);
}
