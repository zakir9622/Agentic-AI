import Link from "next/link";
import { Package, Truck } from "lucide-react";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import { listProviders } from "@/lib/shipping";
import { ShipForm } from "./ship-form";

export const metadata = { title: "Ready to Ship | Admin" };
export const dynamic = "force-dynamic";

interface ReadyOrder {
  id: string;
  orderNumber: string;
  createdAt: Date;
  total: number;
  paymentMethod: string;
  orderStatus: string;
  customer: string;
  destination: string;
  itemCount: number;
  firstItem: string;
}

async function getReadyOrders(): Promise<ReadyOrder[]> {
  try {
    const orders = await db.order.findMany({
      where: {
        orderStatus: { in: ["CONFIRMED", "PROCESSING"] },
        fulfillmentStatus: { in: ["UNFULFILLED", "PARTIALLY_FULFILLED"] },
      },
      include: {
        user: { select: { name: true } },
        address: { select: { city: true, state: true } },
        items: { select: { productName: true, quantity: true } },
      },
      orderBy: { createdAt: "asc" },
      take: 50,
    });
    return orders.map((o) => ({
      id: o.id,
      orderNumber: o.orderNumber,
      createdAt: o.createdAt,
      total: o.total,
      paymentMethod: o.paymentMethod,
      orderStatus: o.orderStatus,
      customer: o.user?.name ?? o.guestEmail ?? "Guest",
      destination: o.address ? `${o.address.city}, ${o.address.state}` : "—",
      itemCount: o.items.reduce((s, i) => s + i.quantity, 0),
      firstItem:
        o.items[0]?.productName +
        (o.items.length > 1 ? ` +${o.items.length - 1} more` : ""),
    }));
  } catch {
    return [];
  }
}

export default async function ReadyToShipPage() {
  const orders = await getReadyOrders();
  const carriers = listProviders().map((p) => ({
    id: p.id,
    label: p.label,
    configured: p.isConfigured(),
  }));
  const configuredCount = carriers.filter((c) => c.configured).length;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-2xl font-semibold text-[var(--color-text-primary)]">
            <Truck className="h-6 w-6 text-[var(--color-gold)]" aria-hidden="true" />
            Ready to Ship
          </h2>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Confirmed orders awaiting fulfillment. Shipping creates the AWB, marks the
            order shipped, and texts the customer.
          </p>
        </div>
        <div className="flex flex-wrap gap-1.5">
          {carriers.map((c) => (
            <GlassBadge key={c.id} variant={c.configured ? "success" : "neutral"}>
              {c.label}: {c.configured ? "connected" : "off"}
            </GlassBadge>
          ))}
        </div>
      </div>

      {configuredCount === 0 && (
        <GlassCard padding="md" className="border-[var(--color-warning)]/30">
          <p className="text-sm text-[var(--color-text-secondary)]">
            No carrier API is configured yet, so automatic AWB creation is unavailable.
            You can still ship using{" "}
            <strong className="text-[var(--color-text-primary)]">Manual entry</strong>{" "}
            (type the AWB from your courier). Add Shiprocket / Delhivery / Amazon
            credentials in your environment to enable one-click shipping — see{" "}
            <code className="text-[var(--color-gold)]">docs/DEPLOYMENT.md</code>.
          </p>
        </GlassCard>
      )}

      {orders.length === 0 ? (
        <GlassCard padding="lg">
          <div className="flex flex-col items-center gap-3 py-10 text-center">
            <Package
              className="h-10 w-10 text-[var(--color-text-muted)]"
              aria-hidden="true"
            />
            <p className="text-sm text-[var(--color-text-secondary)]">
              No orders are waiting to ship right now.
            </p>
            <Link
              href="/admin/orders"
              className="text-sm text-[var(--color-gold)] underline-offset-4 hover:underline"
            >
              View all orders →
            </Link>
          </div>
        </GlassCard>
      ) : (
        <ul className="flex flex-col gap-4" role="list">
          {orders.map((o) => (
            <li key={o.id}>
              <GlassCard padding="md">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <Link
                        href={`/admin/orders/${o.id}`}
                        className="font-medium text-[var(--color-gold)] hover:underline"
                      >
                        {o.orderNumber}
                      </Link>
                      <GlassBadge
                        variant={o.orderStatus === "PROCESSING" ? "info" : "gold"}
                      >
                        {o.orderStatus}
                      </GlassBadge>
                      <span className="text-xs text-[var(--color-text-muted)]">
                        {o.paymentMethod}
                      </span>
                    </div>
                    <p className="mt-1 truncate text-sm text-[var(--color-text-secondary)]">
                      {o.customer} · {o.destination}
                    </p>
                    <p className="mt-0.5 truncate text-xs text-[var(--color-text-muted)]">
                      {o.itemCount} item{o.itemCount === 1 ? "" : "s"} · {o.firstItem} ·{" "}
                      {formatPrice(o.total)} ·{" "}
                      {new Date(o.createdAt).toLocaleDateString("en-IN")}
                    </p>
                  </div>

                  <div className="lg:w-[460px] lg:shrink-0">
                    <ShipForm orderId={o.id} carriers={carriers} />
                  </div>
                </div>
              </GlassCard>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
