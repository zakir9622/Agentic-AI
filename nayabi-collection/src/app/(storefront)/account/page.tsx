import Link from "next/link";
import { Award, Star } from "lucide-react";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice, relativeTime } from "@/lib/utils";
import { getLoyalty } from "@/lib/loyalty";

export const metadata = { title: "My Account" };

const statusVariant = {
  PENDING: "neutral",
  CONFIRMED: "info",
  PROCESSING: "info",
  SHIPPED: "gold",
  DELIVERED: "success",
  CANCELLED: "error",
  RETURNED: "warning",
  REFUNDED: "warning",
} as const;

export default async function AccountPage() {
  const session = await auth();
  let orders: Awaited<ReturnType<typeof fetchOrders>> = [];
  try {
    orders = await fetchOrders(session!.user.id);
  } catch {
    orders = [];
  }
  const loyalty = await getLoyalty(session?.user?.id ?? null);

  return (
    <div className="flex flex-col gap-6">
      {/* Loyalty / rewards */}
      <GlassCard tier="elevated" padding="md">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="flex h-12 w-12 items-center justify-center rounded-2xl border border-[var(--color-gold)]/25 bg-[var(--color-gold)]/12">
              <Award className="h-6 w-6 text-[var(--color-gold)]" aria-hidden="true" />
            </span>
            <div>
              <p className="text-xs tracking-widest text-[var(--color-text-muted)] uppercase">
                Nayabi Rewards
              </p>
              <p className="text-lg font-semibold text-[var(--color-text-primary)]">
                {loyalty.points} points
                <span className="ml-2 align-middle">
                  <GlassBadge variant="gold">{loyalty.tier}</GlassBadge>
                </span>
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className="flex items-center justify-end gap-1 text-xs text-[var(--color-text-secondary)]">
              <Star className="h-3.5 w-3.5 text-[var(--color-gold)]" aria-hidden="true" />
              {loyalty.tierPerk}
            </p>
            {loyalty.nextTier && (
              <p className="mt-1 text-xs text-[var(--color-text-muted)]">
                {loyalty.pointsToNext} pts to {loyalty.nextTier}
              </p>
            )}
          </div>
        </div>
      </GlassCard>

      <GlassCard padding="md">
        <h2 className="text-base !font-[var(--font-body)] font-semibold">
          Recent orders
        </h2>
        {orders.length === 0 ? (
          <p className="mt-3 text-sm text-[var(--color-text-secondary)]">
            No orders yet.{" "}
            <Link
              href="/shop"
              className="text-[var(--color-gold)] underline underline-offset-2"
            >
              Start shopping
            </Link>
          </p>
        ) : (
          <ul className="mt-4 divide-y divide-[var(--color-glass-border)]" role="list">
            {orders.map((o) => (
              <li key={o.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className="text-sm font-medium text-[var(--color-text-primary)]">
                    {o.orderNumber}
                  </p>
                  <p className="text-xs text-[var(--color-text-muted)]">
                    {o.items.length} item{o.items.length === 1 ? "" : "s"} ·{" "}
                    {relativeTime(o.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <GlassBadge variant={statusVariant[o.orderStatus]}>
                    {o.orderStatus}
                  </GlassBadge>
                  <span className="text-sm font-semibold text-[var(--color-gold)]">
                    {formatPrice(o.total)}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </GlassCard>
    </div>
  );
}

function fetchOrders(userId: string) {
  return db.order.findMany({
    where: { userId },
    include: { items: { select: { id: true } } },
    orderBy: { createdAt: "desc" },
    take: 10,
  });
}
