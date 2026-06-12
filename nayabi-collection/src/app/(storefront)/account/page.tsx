import Link from "next/link";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice, relativeTime } from "@/lib/utils";

export const metadata = { title: "My Account" };

const statusVariant = {
  PENDING: "neutral", CONFIRMED: "info", PROCESSING: "info", SHIPPED: "gold",
  DELIVERED: "success", CANCELLED: "error", RETURNED: "warning", REFUNDED: "warning",
} as const;

export default async function AccountPage() {
  const session = await auth();
  let orders: Awaited<ReturnType<typeof fetchOrders>> = [];
  try {
    orders = await fetchOrders(session!.user.id);
  } catch {
    orders = [];
  }

  return (
    <div className="flex flex-col gap-6">
      <GlassCard padding="md">
        <h2 className="text-base font-semibold !font-[var(--font-body)]">Recent orders</h2>
        {orders.length === 0 ? (
          <p className="mt-3 text-sm text-[var(--color-text-secondary)]">
            No orders yet.{" "}
            <Link href="/shop" className="text-[var(--color-gold)] underline underline-offset-2">
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
                    {o.items.length} item{o.items.length === 1 ? "" : "s"} · {relativeTime(o.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <GlassBadge variant={statusVariant[o.orderStatus]}>{o.orderStatus}</GlassBadge>
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
