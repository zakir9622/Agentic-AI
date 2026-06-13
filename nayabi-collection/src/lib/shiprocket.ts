/* Shiprocket API integration — fail-silent, matches msg91 pattern. */

const BASE = "https://apiv2.shiprocket.in/v1/external";

let _token: string | null = null;
let _tokenExpiry = 0;

async function getToken(): Promise<string | null> {
  const email = process.env.SHIPROCKET_EMAIL;
  const password = process.env.SHIPROCKET_PASSWORD;
  if (!email || !password) return null;

  if (_token && Date.now() < _tokenExpiry) return _token;

  try {
    const res = await fetch(`${BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) return null;
    const data = await res.json();
    _token = data.token ?? null;
    _tokenExpiry = Date.now() + 9 * 60 * 60 * 1000; // 9h (token valid 10h)
    return _token;
  } catch (e) {
    console.error("Shiprocket auth failed:", e);
    return null;
  }
}

async function sr<T>(path: string, body: Record<string, unknown>): Promise<T | null> {
  const token = await getToken();
  if (!token) return null;
  try {
    const res = await fetch(`${BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      console.error(`Shiprocket ${path} failed:`, res.status, await res.text());
      return null;
    }
    return res.json();
  } catch (e) {
    console.error(`Shiprocket ${path} error:`, e);
    return null;
  }
}

async function srGet<T>(path: string): Promise<T | null> {
  const token = await getToken();
  if (!token) return null;
  try {
    const res = await fetch(`${BASE}${path}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) return null;
    return res.json();
  } catch (e) {
    console.error(`Shiprocket GET ${path} error:`, e);
    return null;
  }
}

export interface ShiprocketOrderPayload {
  orderNumber: string;
  orderDate: string; // ISO date string
  customerName: string;
  customerPhone: string;
  customerEmail: string;
  address: {
    line1: string;
    line2?: string | null;
    city: string;
    state: string;
    pincode: string;
  };
  items: Array<{
    name: string;
    sku: string;
    units: number;
    sellingPrice: number; // in rupees (not paise)
  }>;
  paymentMethod: "Prepaid" | "COD";
  subtotal: number; // rupees
  length?: number; // cm
  breadth?: number; // cm
  height?: number; // cm
  weight?: number; // kg
}

export interface ShiprocketOrderResult {
  orderId: number;
  shipmentId: number;
  awbCode: string | null;
  channelOrderId: string;
  status: string;
}

export async function createShiprocketOrder(
  payload: ShiprocketOrderPayload
): Promise<ShiprocketOrderResult | null> {
  const body = {
    order_id: payload.orderNumber,
    order_date: payload.orderDate,
    pickup_location: "Primary",
    channel_id: "",
    billing_customer_name: payload.customerName,
    billing_last_name: "",
    billing_address: payload.address.line1,
    billing_address_2: payload.address.line2 ?? "",
    billing_city: payload.address.city,
    billing_pincode: payload.address.pincode,
    billing_state: payload.address.state,
    billing_country: "India",
    billing_email: payload.customerEmail,
    billing_phone: payload.customerPhone.replace(/\D/g, "").slice(-10),
    shipping_is_billing: true,
    order_items: payload.items.map((i) => ({
      name: i.name,
      sku: i.sku,
      units: i.units,
      selling_price: i.sellingPrice,
    })),
    payment_method: payload.paymentMethod,
    sub_total: payload.subtotal,
    length: payload.length ?? 20,
    breadth: payload.breadth ?? 15,
    height: payload.height ?? 5,
    weight: payload.weight ?? 0.5,
  };

  const data = await sr<{
    order_id: number;
    shipment_id: number;
    awb_code?: string;
    channel_order_id: string;
    status: string;
  }>("/orders/create/adhoc", body);

  if (!data) return null;
  return {
    orderId: data.order_id,
    shipmentId: data.shipment_id,
    awbCode: data.awb_code ?? null,
    channelOrderId: data.channel_order_id,
    status: data.status,
  };
}

export interface ShiprocketTracking {
  awbCode: string;
  currentStatus: string;
  deliveredDate: string | null;
  etd: string | null;
  activities: Array<{
    date: string;
    activity: string;
    location: string;
  }>;
}

export async function getShiprocketTracking(
  awbCode: string
): Promise<ShiprocketTracking | null> {
  const data = await srGet<{
    tracking_data?: {
      track_status?: number;
      shipment_track_activities?: Array<{
        date: string;
        activity: string;
        location: string;
      }>;
      shipment_track?: Array<{
        current_status: string;
        delivered_date?: string;
        etd?: string;
        awb_code: string;
      }>;
    };
  }>(`/courier/track/awb/${awbCode}`);

  if (!data?.tracking_data) return null;

  const track = data.tracking_data.shipment_track?.[0];
  return {
    awbCode,
    currentStatus: track?.current_status ?? "Unknown",
    deliveredDate: track?.delivered_date ?? null,
    etd: track?.etd ?? null,
    activities: data.tracking_data.shipment_track_activities ?? [],
  };
}

export async function cancelShiprocketOrder(ids: number[]): Promise<boolean> {
  const data = await sr<{ message: string }>("/orders/cancel", { ids });
  return data !== null;
}
