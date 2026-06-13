import Link from "next/link";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import { Package } from "lucide-react";

export const metadata = { title: "My Orders" };

const statusVariant = {
  PENDING: "neutral", CONFIRMED: "info", PROCESSING: "info", SHIPPED: "gold",
  DELIVERED: "success", CANCELLED: "error", RETURNED: "warning", REFUNDED: "warning",
} as const;

export default async function OrdersPage() {
  const session = await auth();
  let orders: Awaited<ReturnType<typeof fetchOrders>> = [];
  try {
    orders = await fetchOrders(session!.user.id);
  } catch {
    orders = [];
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Order History</h2>

      {orders.length === 0 ? (
        <GlassCard padding="lg" className="flex flex-col items-center gap-4 text-center">
          <Package className="h-12 w-12 text-[var(--color-text-muted)]" aria-hidden="true" />
          <div>
            <p className="text-[var(--color-text-primary)] font-medium">No orders yet</p>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              Your purchases will appear here.
            </p>
          </div>
          <Link
            href="/shop"
            className="text-sm text-[var(--color-gold)] underline underline-offset-2"
          >
            Explore the collection
          </Link>
        </GlassCard>
      ) : (
        <ul className="flex flex-col gap-3" role="list">
          {orders.map((order) => (
            <li key={order.id}>
              <GlassCard padding="md" hoverable>
                <Link href={`/account/orders/${order.id}`} className="block">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                      <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                        {order.orderNumber}
                      </p>
                      <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">
                        {new Date(order.createdAt).toLocaleDateString("en-IN", {
                          day: "numeric",
                          month: "long",
                          year: "numeric",
                        })}{" "}
                        · {order.items.length} item{order.items.length === 1 ? "" : "s"}
                      </p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {order.items.slice(0, 2).map((item) => (
                          <span key={item.id} className="text-xs text-[var(--color-text-secondary)]">
                            {item.productName}
                            {order.items.length > 2 && item === order.items[1] && (
                              <> +{order.items.length - 2} more</>
                            )}
                          </span>
                        ))}
                      </div>
                    </div>
                    <div className="flex items-center gap-4 sm:flex-col sm:items-end">
                      <GlassBadge variant={statusVariant[order.orderStatus]}>
                        {order.orderStatus}
                      </GlassBadge>
                      <p className="text-base font-bold text-[var(--color-gold)]">
                        {formatPrice(order.total)}
                      </p>
                    </div>
                  </div>
                </Link>
              </GlassCard>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function fetchOrders(userId: string) {
  return db.order.findMany({
    where: { userId },
    include: {
      items: { select: { id: true, productName: true } },
      shipment: { select: { trackingNumber: true, carrier: true } },
    },
    orderBy: { createdAt: "desc" },
  });
}
