import type {
  ShippingProvider,
  CreateShipmentInput,
  CreateShipmentResult,
  TrackingResult,
} from "./types";

/**
 * Amazon Shipping (India) adapter — SCAFFOLD.
 *
 * Amazon Shipping uses the Amazon Shipping API (v2) which requires:
 *   - Merchant onboarding/approval for Amazon Shipping in India
 *   - Login with Amazon (LWA) OAuth: client id/secret + refresh token
 *   - AWS SigV4-signed requests to the Shipping endpoint
 *
 * The full flow is: getRates → purchaseShipment (returns a tracking id + label)
 * → getTracking. Because it needs approved credentials that can't be stubbed
 * meaningfully, this adapter implements the ShippingProvider interface but
 * returns null (fail-silent) until the env credentials below are provided and
 * the API calls are wired:
 *   AMAZON_SHIPPING_CLIENT_ID
 *   AMAZON_SHIPPING_CLIENT_SECRET
 *   AMAZON_SHIPPING_REFRESH_TOKEN
 *
 * Keeping it behind the interface means the admin UI lists Amazon as an option
 * and the rest of the system (DB shipment record, tracking page, notifications)
 * works unchanged the moment the API calls are completed.
 */
function isConfigured() {
  return Boolean(
    process.env.AMAZON_SHIPPING_CLIENT_ID &&
    process.env.AMAZON_SHIPPING_CLIENT_SECRET &&
    process.env.AMAZON_SHIPPING_REFRESH_TOKEN
  );
}

async function createShipment(
  input: CreateShipmentInput
): Promise<CreateShipmentResult | null> {
  if (!isConfigured()) return null;
  // TODO: LWA token exchange → getRates → purchaseShipment → map response.
  console.warn(
    `Amazon Shipping createShipment not yet implemented (order ${input.orderNumber}); configure credentials and wire the API.`
  );
  return null;
}

async function track(awb: string): Promise<TrackingResult | null> {
  if (!isConfigured()) return null;
  // TODO: call Amazon Shipping getTracking and map activities.
  console.warn(`Amazon Shipping track not yet implemented (awb ${awb}).`);
  return null;
}

function trackingUrl(awb: string) {
  return `https://track.amazon.in/tracking/${awb}`;
}

export const amazonProvider: ShippingProvider = {
  id: "amazon",
  label: "Amazon Shipping",
  isConfigured,
  createShipment,
  track,
  trackingUrl,
};
