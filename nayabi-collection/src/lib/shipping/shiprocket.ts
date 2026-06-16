import { createShiprocketOrder, getShiprocketTracking } from "@/lib/shiprocket";
import type { ShippingProvider } from "./types";

function srTrackingUrl(awb: string) {
  return `https://shiprocket.co/tracking/${awb}`;
}

/** Shiprocket adapter — wraps the existing Shiprocket REST client. */
export const shiprocketProvider: ShippingProvider = {
  id: "shiprocket",
  label: "Shiprocket",

  isConfigured: () =>
    Boolean(process.env.SHIPROCKET_EMAIL && process.env.SHIPROCKET_PASSWORD),

  async createShipment(input) {
    const r = await createShiprocketOrder({
      orderNumber: input.orderNumber,
      orderDate: input.orderDate.slice(0, 10),
      customerName: input.address.name,
      customerPhone: input.address.phone,
      customerEmail: input.address.email,
      address: {
        line1: input.address.line1,
        line2: input.address.line2,
        city: input.address.city,
        state: input.address.state,
        pincode: input.address.pincode,
      },
      items: input.items,
      paymentMethod: input.paymentMethod,
      subtotal: input.subtotal,
      weight: input.weightKg,
    });
    if (!r) return null;
    return {
      carrier: "shiprocket",
      carrierLabel: "Shiprocket",
      providerShipmentId: r.shipmentId ? String(r.shipmentId) : null,
      awbCode: r.awbCode,
      trackingUrl: r.awbCode ? srTrackingUrl(r.awbCode) : null,
      status: r.status || "MANIFESTED",
    };
  },

  async track(awb) {
    const t = await getShiprocketTracking(awb);
    if (!t) return null;
    return {
      awbCode: t.awbCode,
      currentStatus: t.currentStatus,
      deliveredDate: t.deliveredDate,
      etd: t.etd,
      activities: t.activities,
    };
  },

  trackingUrl: srTrackingUrl,
};
