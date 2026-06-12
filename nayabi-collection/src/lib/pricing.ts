import {
  FREE_SHIPPING_THRESHOLD,
  FLAT_SHIPPING_RATE,
  COD_CHARGE,
  GIFT_WRAP_CHARGE,
  GST_RATE,
} from "@/lib/constants";

export interface PricingInput {
  subtotal: number; // paise, sum of line items at DB prices
  discountAmount?: number;
  isCOD?: boolean;
  isGiftWrapped?: boolean;
}

export interface PricingResult {
  subtotal: number;
  discountAmount: number;
  shippingCost: number;
  codCharge: number;
  giftWrapCharge: number;
  taxAmount: number; // GST portion already included in prices (informational)
  total: number;
}

export function computePricing(input: PricingInput): PricingResult {
  const subtotal = input.subtotal;
  const discountAmount = Math.min(input.discountAmount ?? 0, subtotal);
  const afterDiscount = subtotal - discountAmount;

  const shippingCost = afterDiscount >= FREE_SHIPPING_THRESHOLD ? 0 : FLAT_SHIPPING_RATE;
  const codCharge = input.isCOD ? COD_CHARGE : 0;
  const giftWrapCharge = input.isGiftWrapped ? GIFT_WRAP_CHARGE : 0;

  // GST is included in listed prices: extract the included portion for the invoice
  const taxAmount = Math.round((afterDiscount * GST_RATE) / (100 + GST_RATE));

  const total = afterDiscount + shippingCost + codCharge + giftWrapCharge;

  return { subtotal, discountAmount, shippingCost, codCharge, giftWrapCharge, taxAmount, total };
}
