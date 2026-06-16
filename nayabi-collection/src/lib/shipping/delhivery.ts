import type {
  ShippingProvider,
  CreateShipmentInput,
  CreateShipmentResult,
  TrackingResult,
} from "./types";

/**
 * Delhivery adapter. Uses Delhivery's REST API (token auth). Requires:
 *   DELHIVERY_API_TOKEN     — API token from the Delhivery dashboard
 *   DELHIVERY_PICKUP_NAME   — your registered warehouse/pickup name
 *   DELHIVERY_BASE          — optional; defaults to the production host
 *
 * Endpoints follow Delhivery's documented One API. All calls are fail-silent
 * (log + return null) so a courier outage never breaks the admin flow.
 */
const BASE = () => process.env.DELHIVERY_BASE || "https://track.delhivery.com";
const TOKEN = () => process.env.DELHIVERY_API_TOKEN;
const PICKUP = () => process.env.DELHIVERY_PICKUP_NAME || "Primary";

function delhiveryTrackingUrl(awb: string) {
  return `https://www.delhivery.com/track/package/${awb}`;
}

async function createShipment(
  input: CreateShipmentInput
): Promise<CreateShipmentResult | null> {
  const token = TOKEN();
  if (!token) return null;

  const payload = {
    pickup_location: { name: PICKUP() },
    shipments: [
      {
        name: input.address.name,
        add: [input.address.line1, input.address.line2].filter(Boolean).join(", "),
        pin: input.address.pincode,
        city: input.address.city,
        state: input.address.state,
        country: "India",
        phone: input.address.phone.replace(/\D/g, "").slice(-10),
        order: input.orderNumber,
        payment_mode: input.paymentMethod === "COD" ? "COD" : "Prepaid",
        cod_amount:
          input.paymentMethod === "COD" ? (input.codAmount ?? input.subtotal) : 0,
        total_amount: input.subtotal,
        quantity: input.items.reduce((s, i) => s + i.units, 0),
        products_desc: input.items
          .map((i) => i.name)
          .join(", ")
          .slice(0, 250),
        weight: Math.round((input.weightKg ?? 0.5) * 1000), // grams
        shipment_width: 15,
        shipment_height: 5,
        shipment_length: 20,
        waybill: "",
      },
    ],
  };

  try {
    const res = await fetch(`${BASE()}/api/cmu/create.json`, {
      method: "POST",
      headers: {
        Authorization: `Token ${token}`,
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body: `format=json&data=${encodeURIComponent(JSON.stringify(payload))}`,
    });
    if (!res.ok) {
      console.error("Delhivery create failed:", res.status, await res.text());
      return null;
    }
    const data = await res.json();
    const pkg = data?.packages?.[0];
    const awb: string | null = pkg?.waybill ?? null;
    if (!awb || pkg?.status === "Fail") {
      console.error(
        "Delhivery create returned no waybill:",
        JSON.stringify(data).slice(0, 400)
      );
      return null;
    }
    return {
      carrier: "delhivery",
      carrierLabel: "Delhivery",
      providerShipmentId: awb,
      awbCode: awb,
      trackingUrl: delhiveryTrackingUrl(awb),
      status: "MANIFESTED",
    };
  } catch (e) {
    console.error("Delhivery create error:", e);
    return null;
  }
}

async function track(awb: string): Promise<TrackingResult | null> {
  const token = TOKEN();
  if (!token) return null;
  try {
    const res = await fetch(`${BASE()}/api/v1/packages/json/?waybill=${awb}`, {
      headers: { Authorization: `Token ${token}`, Accept: "application/json" },
    });
    if (!res.ok) return null;
    const data = await res.json();
    const shipment = data?.ShipmentData?.[0]?.Shipment;
    if (!shipment) return null;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const scans: any[] = shipment.Scans ?? [];
    return {
      awbCode: awb,
      currentStatus: shipment.Status?.Status ?? "Unknown",
      deliveredDate:
        shipment.Status?.Status === "Delivered"
          ? (shipment.Status?.StatusDateTime ?? null)
          : null,
      etd: shipment.ExpectedDeliveryDate ?? null,
      activities: scans.map((s) => ({
        date: s.ScanDetail?.ScanDateTime ?? "",
        activity: s.ScanDetail?.Instructions ?? s.ScanDetail?.Scan ?? "",
        location: s.ScanDetail?.ScannedLocation ?? "",
      })),
    };
  } catch (e) {
    console.error("Delhivery track error:", e);
    return null;
  }
}

export const delhiveryProvider: ShippingProvider = {
  id: "delhivery",
  label: "Delhivery",
  isConfigured: () => Boolean(TOKEN()),
  createShipment,
  track,
  trackingUrl: delhiveryTrackingUrl,
};
