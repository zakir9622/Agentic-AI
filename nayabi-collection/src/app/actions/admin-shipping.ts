"use server";

import { z } from "zod";
import { db } from "@/lib/db";
import { requireAdminSession } from "@/lib/admin-auth";
import { revalidatePath } from "next/cache";
import { notifyOrderShipped } from "@/lib/msg91";
import { getProvider, type CreateShipmentResult } from "@/lib/shipping";

export interface ShipState {
  ok?: boolean;
  message?: string;
}

const schema = z.object({
  orderId: z.string().min(1),
  // a CarrierId ("shiprocket"|"delhivery"|"amazon") or "manual"
  carrier: z.string().min(1),
  // manual entry (used when carrier === "manual", or to override auto AWB)
  manualAwb: z.string().optional(),
  manualCarrierName: z.string().optional(),
  manualTrackingUrl: z.string().optional(),
});

/**
 * Ship a confirmed order: either create a shipment with a carrier's API
 * (Shiprocket/Delhivery/Amazon) or record a manually-entered AWB. Records the
 * Shipment, marks the order SHIPPED + FULFILLED, and notifies the customer by
 * SMS + WhatsApp. Carrier API failures fall back to a manual-style record so
 * the admin is never blocked.
 */
export async function shipOrderAction(
  _prev: ShipState,
  formData: FormData
): Promise<ShipState> {
  await requireAdminSession();

  const parsed = schema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return { message: "Invalid data" };
  const { orderId, carrier, manualAwb, manualCarrierName, manualTrackingUrl } =
    parsed.data;

  const order = await db.order.findUnique({
    where: { id: orderId },
    include: {
      user: true,
      address: true,
      items: { include: { variant: { select: { sku: true } } } },
    },
  });
  if (!order) return { message: "Order not found" };
  if (!order.address) return { message: "Order has no shipping address" };

  let result: CreateShipmentResult | null = null;

  if (carrier !== "manual") {
    const provider = getProvider(carrier);
    if (!provider) return { message: "Unknown carrier" };
    if (!provider.isConfigured()) {
      return {
        message: `${provider.label} isn't configured. Add its API credentials, or use Manual entry.`,
      };
    }
    result = await provider.createShipment({
      orderNumber: order.orderNumber,
      orderDate: order.createdAt.toISOString(),
      address: {
        name: order.address.name,
        phone: order.address.phone,
        email: order.user?.email ?? order.guestEmail ?? "",
        line1: order.address.line1,
        line2: order.address.line2,
        city: order.address.city,
        state: order.address.state,
        pincode: order.address.pincode,
      },
      items: order.items.map((i) => ({
        name: i.productName,
        sku: i.variant?.sku ?? i.variantId,
        units: i.quantity,
        sellingPrice: Math.round(i.totalPrice / i.quantity / 100),
      })),
      paymentMethod: order.paymentMethod === "COD" ? "COD" : "Prepaid",
      subtotal: Math.round(order.subtotal / 100),
      codAmount:
        order.paymentMethod === "COD" ? Math.round(order.total / 100) : undefined,
    });

    if (!result && !manualAwb) {
      return {
        message: `${provider.label} couldn't create the shipment. Check credentials/pickup location, or enter an AWB manually.`,
      };
    }
  }

  // Resolve final shipment fields (carrier result, else manual entry)
  const provider = carrier !== "manual" ? getProvider(carrier) : null;
  const carrierName =
    result?.carrierLabel ?? manualCarrierName ?? provider?.label ?? "Manual";
  const awb = result?.awbCode ?? manualAwb ?? null;
  const trackingUrl =
    result?.trackingUrl ??
    manualTrackingUrl ??
    (awb && provider ? provider.trackingUrl(awb) : null);

  await db.shipment.upsert({
    where: { orderId },
    create: {
      orderId,
      carrier: carrierName,
      trackingNumber: awb,
      trackingUrl,
      status: "SHIPPED",
      shippedAt: new Date(),
      shiprocketShipmentId:
        result?.carrier === "shiprocket" ? result.providerShipmentId : null,
    },
    update: {
      carrier: carrierName,
      trackingNumber: awb,
      trackingUrl,
      status: "SHIPPED",
      shippedAt: new Date(),
      ...(result?.carrier === "shiprocket" && result.providerShipmentId
        ? { shiprocketShipmentId: result.providerShipmentId }
        : {}),
    },
  });

  await db.order.update({
    where: { id: orderId },
    data: { orderStatus: "SHIPPED", fulfillmentStatus: "FULFILLED" },
  });

  // Notify the customer (fail-silent inside the helper)
  const phone = order.address.phone || order.user?.phone || null;
  notifyOrderShipped(phone, order.orderNumber, trackingUrl ?? "").catch(() => {});

  revalidatePath("/admin/shipping");
  revalidatePath("/admin/orders");
  revalidatePath(`/admin/orders/${orderId}`);

  return {
    ok: true,
    message: awb
      ? `Order ${order.orderNumber} shipped via ${carrierName} (AWB ${awb}). Customer notified.`
      : `Order ${order.orderNumber} marked shipped via ${carrierName}. Customer notified.`,
  };
}
