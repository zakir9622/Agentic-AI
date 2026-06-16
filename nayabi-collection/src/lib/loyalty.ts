import { db } from "@/lib/db";

/** Earn 1 point per ₹10 spent. */
export const POINTS_PER_RUPEE = 0.1;

export const LOYALTY_TIERS = [
  { name: "BRONZE", min: 0, perk: "Member pricing & early access to sales" },
  { name: "SILVER", min: 1000, perk: "Free shipping on every order" },
  { name: "GOLD", min: 5000, perk: "Priority support + a birthday gift" },
] as const;

export interface LoyaltySummary {
  points: number;
  lifetimePoints: number;
  tier: string;
  tierPerk: string;
  nextTier: string | null;
  pointsToNext: number | null;
  demo?: boolean;
}

function summarize(points: number, lifetimePoints: number, demo = false): LoyaltySummary {
  let tierIdx = 0;
  for (let i = LOYALTY_TIERS.length - 1; i >= 0; i--) {
    if (lifetimePoints >= LOYALTY_TIERS[i].min) {
      tierIdx = i;
      break;
    }
  }
  const tier = LOYALTY_TIERS[tierIdx];
  const next = LOYALTY_TIERS[tierIdx + 1] ?? null;
  return {
    points,
    lifetimePoints,
    tier: tier.name,
    tierPerk: tier.perk,
    nextTier: next?.name ?? null,
    pointsToNext: next ? Math.max(0, next.min - lifetimePoints) : null,
    demo,
  };
}

/** Loyalty summary for a user. Returns a sample for signed-out/demo preview. */
export async function getLoyalty(userId: string | null): Promise<LoyaltySummary> {
  if (!userId) return summarize(0, 0);
  try {
    const acc = await db.loyaltyAccount.findUnique({ where: { userId } });
    if (!acc) return summarize(0, 0);
    return summarize(acc.points, acc.lifetimePoints);
  } catch {
    return summarize(0, 0);
  }
}

/**
 * Award points for an order total (paise) and upsert the user's loyalty
 * account. Fail-silent — call after a successful order. Returns points earned.
 */
export async function awardOrderPoints(
  userId: string | null,
  orderTotalPaise: number,
  orderId?: string
): Promise<number> {
  if (!userId) return 0;
  const earned = Math.floor((orderTotalPaise / 100) * POINTS_PER_RUPEE);
  if (earned <= 0) return 0;
  try {
    await db.loyaltyAccount.upsert({
      where: { userId },
      create: {
        userId,
        points: earned,
        lifetimePoints: earned,
        transactions: { create: { points: earned, reason: "ORDER", orderId } },
      },
      update: {
        points: { increment: earned },
        lifetimePoints: { increment: earned },
        transactions: { create: { points: earned, reason: "ORDER", orderId } },
      },
    });
    return earned;
  } catch {
    return 0;
  }
}
