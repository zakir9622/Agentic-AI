"use server";

import { db } from "@/lib/db";

export interface TrackEvent {
  status: string;
  location: string | null;
  message: string | null;
  timestamp: string;
}

export interface TrackedOrder {
  orderNumber: string;
  orderStatus: string;
  fulfillmentStatus: string;
  placedAt: string;
  total: number;
  paymentMethod: string;
  items: { name: string; variant: string; quantity: number; image: string | null }[];
  shipment: {
    carrier: string | null;
    trackingNumber: string | null;
    trackingUrl: string | null;
    status: string;
    shippedAt: string | null;
    deliveredAt: string | null;
    events: TrackEvent[];
  } | null;
}

export interface TrackResult {
  found: boolean;
  order?: TrackedOrder;
  error?: string;
  demo?: boolean;
}

const GENERIC_NOT_FOUND =
  "We couldn't find an order with those details. Double-check the order number and the email used at checkout.";

/**
 * Public order lookup. Requires BOTH the order number and the email used at
 * checkout, and returns a generic message on any mismatch so order existence
 * isn't leaked. `NAYABI-DEMO` returns a sample timeline so the tracking UI can
 * be previewed without a seeded database.
 */
export async function lookupOrder(
  _prev: TrackResult,
  formData: FormData
): Promise<TrackResult> {
  const orderNumber = String(formData.get("orderNumber") ?? "").trim();
  const email = String(formData.get("email") ?? "")
    .trim()
    .toLowerCase();

  if (!orderNumber || !email) {
    return { found: false, error: "Please enter your order number and email." };
  }

  if (orderNumber.toUpperCase() === "NAYABI-DEMO") {
    return { found: true, demo: true, order: sampleOrder() };
  }

  try {
    const order = await db.order.findUnique({
      where: { orderNumber },
      include: {
        user: { select: { email: true } },
        items: {
          select: {
            productName: true,
            variantDetails: true,
            quantity: true,
            imageUrl: true,
          },
        },
        shipment: { include: { events: { orderBy: { timestamp: "desc" } } } },
      },
    });

    if (!order) return { found: false, error: GENERIC_NOT_FOUND };

    const orderEmail = (order.guestEmail ?? order.user?.email ?? "").toLowerCase();
    if (!orderEmail || orderEmail !== email) {
      return { found: false, error: GENERIC_NOT_FOUND };
    }

    return {
      found: true,
      order: {
        orderNumber: order.orderNumber,
        orderStatus: order.orderStatus,
        fulfillmentStatus: order.fulfillmentStatus,
        placedAt: order.createdAt.toISOString(),
        total: order.total,
        paymentMethod: order.paymentMethod,
        items: order.items.map((i) => ({
          name: i.productName,
          variant: i.variantDetails,
          quantity: i.quantity,
          image: i.imageUrl,
        })),
        shipment: order.shipment
          ? {
              carrier: order.shipment.carrier,
              trackingNumber: order.shipment.trackingNumber,
              trackingUrl: order.shipment.trackingUrl,
              status: order.shipment.status,
              shippedAt: order.shipment.shippedAt?.toISOString() ?? null,
              deliveredAt: order.shipment.deliveredAt?.toISOString() ?? null,
              events: order.shipment.events.map((e) => ({
                status: e.status,
                location: e.location,
                message: e.message,
                timestamp: e.timestamp.toISOString(),
              })),
            }
          : null,
      },
    };
  } catch {
    return {
      found: false,
      error: "Order tracking is temporarily unavailable. Please try again shortly.",
    };
  }
}

function sampleOrder(): TrackedOrder {
  const now = Date.now();
  const day = 86_400_000;
  return {
    orderNumber: "NAYABI-DEMO",
    orderStatus: "SHIPPED",
    fulfillmentStatus: "FULFILLED",
    placedAt: new Date(now - 3 * day).toISOString(),
    total: 149800,
    paymentMethod: "PREPAID",
    items: [
      {
        name: "Silk Georgette Hijab",
        variant: "Rose Gold · One size",
        quantity: 2,
        image:
          "https://images.unsplash.com/photo-1611507929918-08e9e7da2dd4?auto=format&fit=crop&w=400&q=85",
      },
      {
        name: "Premium Prayer Scarf Set",
        variant: "Ivory",
        quantity: 1,
        image:
          "https://images.unsplash.com/photo-1591100063942-9b1e89d2d0b1?auto=format&fit=crop&w=400&q=85",
      },
    ],
    shipment: {
      carrier: "Delhivery",
      trackingNumber: "DL1234567890IN",
      trackingUrl: "https://www.delhivery.com/track/package/DL1234567890IN",
      status: "IN_TRANSIT",
      shippedAt: new Date(now - 2 * day).toISOString(),
      deliveredAt: null,
      events: [
        {
          status: "IN_TRANSIT",
          location: "Bengaluru Hub",
          message: "Shipment in transit to destination city",
          timestamp: new Date(now - 0.5 * day).toISOString(),
        },
        {
          status: "PICKED_UP",
          location: "Mumbai Warehouse",
          message: "Picked up by courier partner",
          timestamp: new Date(now - 2 * day).toISOString(),
        },
        {
          status: "MANIFESTED",
          location: "Mumbai Warehouse",
          message: "Shipping label generated, awaiting pickup",
          timestamp: new Date(now - 2.2 * day).toISOString(),
        },
      ],
    },
  };
}
