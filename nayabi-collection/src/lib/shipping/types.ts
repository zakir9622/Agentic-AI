/**
 * Carrier-agnostic shipping abstraction. Each courier (Shiprocket, Delhivery,
 * Amazon Shipping) implements ShippingProvider, so the admin fulfillment flow
 * and tracking work the same regardless of which carrier ships an order.
 */

export type CarrierId = "shiprocket" | "delhivery" | "amazon";

export interface ShipmentAddress {
  name: string;
  phone: string;
  email: string;
  line1: string;
  line2?: string | null;
  city: string;
  state: string;
  pincode: string;
}

export interface ShipmentItem {
  name: string;
  sku: string;
  units: number;
  sellingPrice: number; // rupees (not paise)
}

export interface CreateShipmentInput {
  orderNumber: string;
  orderDate: string; // ISO date string
  address: ShipmentAddress;
  items: ShipmentItem[];
  paymentMethod: "Prepaid" | "COD";
  subtotal: number; // rupees
  codAmount?: number; // rupees, for COD
  weightKg?: number;
}

export interface CreateShipmentResult {
  carrier: CarrierId;
  carrierLabel: string;
  providerShipmentId: string | null;
  awbCode: string | null;
  trackingUrl: string | null;
  status: string;
}

export interface TrackingActivity {
  date: string;
  activity: string;
  location: string;
}

export interface TrackingResult {
  awbCode: string;
  currentStatus: string;
  deliveredDate: string | null;
  etd: string | null;
  activities: TrackingActivity[];
}

export interface ShippingProvider {
  id: CarrierId;
  label: string;
  /** True when the provider's required env credentials are present. */
  isConfigured(): boolean;
  /** Create a shipment with the carrier. Returns null on failure (fail-silent). */
  createShipment(input: CreateShipmentInput): Promise<CreateShipmentResult | null>;
  /** Fetch live tracking for an AWB/waybill. Null on failure. */
  track(awbCode: string): Promise<TrackingResult | null>;
  /** Customer-facing tracking URL for an AWB. */
  trackingUrl(awbCode: string): string;
}
