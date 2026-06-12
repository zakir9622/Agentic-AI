import { db } from "@/lib/db";

export interface DiscountCheck {
  valid: boolean;
  reason?: string;
  discountCodeId?: string;
  /** Computed discount in paise for the given subtotal */
  amount?: number;
  freeShipping?: boolean;
}

export async function validateDiscountCode(
  code: string,
  subtotal: number,
  userId?: string | null
): Promise<DiscountCheck> {
  const dc = await db.discountCode.findUnique({ where: { code: code.toUpperCase() } });
  if (!dc || !dc.isActive) return { valid: false, reason: "Invalid discount code" };

  const now = new Date();
  if (dc.startsAt && dc.startsAt > now) return { valid: false, reason: "This code is not active yet" };
  if (dc.expiresAt && dc.expiresAt < now) return { valid: false, reason: "This code has expired" };
  if (dc.usageLimit != null && dc.usedCount >= dc.usageLimit)
    return { valid: false, reason: "This code has reached its usage limit" };
  if (dc.minOrderAmount != null && subtotal < dc.minOrderAmount)
    return { valid: false, reason: `Minimum order value not met for this code` };

  if (userId && dc.perCustomerLimit != null) {
    const used = await db.discountUsage.count({
      where: { discountCodeId: dc.id, userId },
    });
    if (used >= dc.perCustomerLimit)
      return { valid: false, reason: "You've already used this code" };
  }

  if (userId && dc.isFirstOrderOnly) {
    const orders = await db.order.count({
      where: { userId, paymentStatus: "PAID" },
    });
    if (orders > 0) return { valid: false, reason: "This code is for first orders only" };
  }

  let amount = 0;
  let freeShipping = false;
  switch (dc.type) {
    case "PERCENT":
      amount = Math.round((subtotal * dc.value) / 100);
      if (dc.maxDiscountAmount != null) amount = Math.min(amount, dc.maxDiscountAmount);
      break;
    case "FIXED":
      amount = Math.min(dc.value, subtotal);
      break;
    case "FREE_SHIPPING":
      freeShipping = true;
      break;
    case "BOGO":
      // BOGO handled at line-item level in a later milestone; treat as invalid for now
      return { valid: false, reason: "This code can't be applied to your cart" };
  }

  return { valid: true, discountCodeId: dc.id, amount, freeShipping };
}
