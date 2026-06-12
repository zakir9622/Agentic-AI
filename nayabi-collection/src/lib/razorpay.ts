import crypto from "node:crypto";

const BASE = "https://api.razorpay.com/v1";

function authHeader() {
  const key = process.env.RAZORPAY_KEY_ID;
  const secret = process.env.RAZORPAY_KEY_SECRET;
  if (!key || !secret) throw new Error("Razorpay keys not configured");
  return "Basic " + Buffer.from(`${key}:${secret}`).toString("base64");
}

export async function createRazorpayOrder(params: {
  amount: number; // paise
  receipt: string; // our order number
}): Promise<{ id: string }> {
  const res = await fetch(`${BASE}/orders`, {
    method: "POST",
    headers: { Authorization: authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify({
      amount: params.amount,
      currency: "INR",
      receipt: params.receipt,
    }),
  });
  if (!res.ok) {
    throw new Error(`Razorpay order creation failed: ${res.status} ${await res.text()}`);
  }
  return res.json();
}

/** Verify the signature returned to the client after a successful checkout */
export function verifyPaymentSignature(params: {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}): boolean {
  const secret = process.env.RAZORPAY_KEY_SECRET;
  if (!secret) return false;
  const expected = crypto
    .createHmac("sha256", secret)
    .update(`${params.razorpayOrderId}|${params.razorpayPaymentId}`)
    .digest("hex");
  return crypto.timingSafeEqual(
    Buffer.from(expected),
    Buffer.from(params.razorpaySignature)
  );
}

/** Verify an incoming webhook payload signature */
export function verifyWebhookSignature(rawBody: string, signature: string): boolean {
  const secret = process.env.RAZORPAY_WEBHOOK_SECRET;
  if (!secret || !signature) return false;
  const expected = crypto.createHmac("sha256", secret).update(rawBody).digest("hex");
  try {
    return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signature));
  } catch {
    return false;
  }
}

export async function createRazorpayRefund(paymentId: string, amount: number) {
  const res = await fetch(`${BASE}/payments/${paymentId}/refund`, {
    method: "POST",
    headers: { Authorization: authHeader(), "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
  });
  if (!res.ok) throw new Error(`Razorpay refund failed: ${res.status} ${await res.text()}`);
  return res.json() as Promise<{ id: string }>;
}
