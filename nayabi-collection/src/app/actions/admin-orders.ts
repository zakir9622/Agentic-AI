"use server";

import { z } from "zod";
import { db } from "@/lib/db";
import { requireAdminSession } from "@/lib/admin-auth";
import { revalidatePath } from "next/cache";
import { sendOrderConfirmationEmail } from "@/lib/mail";
import { notifyOrderShipped } from "@/lib/msg91";

const updateStatusSchema = z.object({
  orderId: z.string(),
  status: z.enum(["CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED"]),
  trackingNumber: z.string().optional(),
  carrier: z.string().optional(),
  trackingUrl: z.string().optional(),
});

export async function updateOrderStatusAction(
  _prev: { ok?: boolean; message?: string },
  formData: FormData
) {
  await requireAdminSession();

  const parsed = updateStatusSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return { message: "Invalid data" };

  const { orderId, status, trackingNumber, carrier, trackingUrl } = parsed.data;

  const order = await db.order.findUnique({
    where: { id: orderId },
    include: { user: true },
  });
  if (!order) return { message: "Order not found" };

  await db.order.update({ where: { id: orderId }, data: { orderStatus: status } });

  if (status === "SHIPPED" && trackingNumber) {
    await db.shipment.upsert({
      where: { orderId },
      create: {
        orderId,
        carrier: carrier ?? null,
        trackingNumber,
        trackingUrl: trackingUrl ?? null,
        status: "SHIPPED",
        shippedAt: new Date(),
      },
      update: {
        carrier: carrier ?? undefined,
        trackingNumber,
        trackingUrl: trackingUrl ?? undefined,
        status: "SHIPPED",
        shippedAt: new Date(),
      },
    });

    if (order.user?.phone) {
      notifyOrderShipped(
        order.user.phone,
        order.orderNumber,
        trackingUrl ?? ""
      ).catch(() => {});
    }
  }

  revalidatePath(`/admin/orders/${orderId}`);
  revalidatePath("/admin/orders");
  return { ok: true, message: `Order updated to ${status}` };
}

export async function processReturnAction(
  _prev: { ok?: boolean; message?: string },
  formData: FormData
) {
  await requireAdminSession();

  const returnId = formData.get("returnId") as string;
  const action = formData.get("action") as "APPROVED" | "REJECTED";
  const adminNote = formData.get("adminNote") as string | null;

  const ret = await db.returnRequest.findUnique({
    where: { id: returnId },
    include: { order: true },
  });
  if (!ret) return { message: "Return request not found" };

  await db.returnRequest.update({
    where: { id: returnId },
    data: {
      status: action === "APPROVED" ? "APPROVED" : "REJECTED",
      adminNote: adminNote ?? undefined,
    },
  });

  revalidatePath("/admin/returns");
  revalidatePath(`/admin/orders/${ret.orderId}`);
  return { ok: true, message: `Return ${action.toLowerCase()}` };
}
