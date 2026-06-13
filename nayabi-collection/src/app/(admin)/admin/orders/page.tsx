import Link from "next/link";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge } from "@/components/ui";
import { formatPrice } from "@/lib/utils";

export const metadata = { title: "Orders | Admin" };

const statusVariant = {
  PENDING: "neutral", CONFIRMED: "info", PROCESSING: "info", SHIPPED: "gold",
  DELIVERED: "success", CANCELLED: "error", RETURNED: "warning", REFUNDED: "warning",
} as const;

const STATUS_OPTIONS = [
  "ALL", "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED", "REFUNDED",
];

export default async function AdminOrdersPage({
  searchParams,
}: {
  searchParams: Promise<{ status?: string; q?: string; page?: string }>;
}) {
  const { status, q, page: pageStr } = await searchParams;
  const page = Math.max(1, parseInt(pageStr ?? "1", 10));
  const perPage = 20;

  const where: Record<string, unknown> = {};
  if (status && status !== "ALL") where.orderStatus = status;
  if (q) {
    where.OR = [
      { orderNumber: { contains: q, mode: "insensitive" } },
      { guestEmail: { contains: q, mode: "insensitive" } },
      { user: { email: { contains: q, mode: "insensitive" } } },
    ];
  }

  const [orders, total] = await Promise.all([
    db.order.findMany({
      where,
      include: {
        user: { select: { name: true, email: true } },
        items: { select: { productName: true } },
      },
      orderBy: { createdAt: "desc" },
      take: perPage,
      skip: (page - 1) * perPage,
    }),
    db.order.count({ where }),
  ]);

  const totalPages = Math.ceil(total / perPage);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Orders</h2>
        <form method="GET" className="flex gap-2">
          <input
            name="q"
            defaultValue={q}
            placeholder="Search order # or email…"
            className="glass-input text-sm w-48"
          />
          <select name="status" defaultValue={status ?? "ALL"} className="glass-input text-sm">
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <button type="submit" className="glass px-3 py-2 text-sm rounded-lg text-[var(--color-text-primary)]">
            Filter
          </button>
        </form>
      </div>

      <GlassCard padding="md">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Orders table">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                {["Order", "Customer", "Items", "Total", "Payment", "Status", "Date", ""].map((h) => (
                  <th key={h} scope="col" className="pb-2 pr-4 last:pr-0 text-left text-xs font-medium text-[var(--color-text-muted)] whitespace-nowrap">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id} className="border-b border-[var(--color-glass-border)] last:border-0 hover:bg-[var(--color-glass-bg)] transition-colors">
                  <td className="py-3 pr-4">
                    <Link href={`/admin/orders/${order.id}`} className="text-[var(--color-gold)] hover:underline font-medium">
                      {order.orderNumber}
                    </Link>
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)] max-w-[140px] truncate">
                    {order.user?.name ?? order.guestEmail ?? "Guest"}
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)] max-w-[140px] truncate">
                    {order.items[0]?.productName}
                    {order.items.length > 1 && ` +${order.items.length - 1}`}
                  </td>
                  <td className="py-3 pr-4 font-semibold text-[var(--color-text-primary)] whitespace-nowrap">
                    {formatPrice(order.total)}
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)]">
                    {order.paymentMethod}
                  </td>
                  <td className="py-3 pr-4">
                    <GlassBadge variant={statusVariant[order.orderStatus]}>
                      {order.orderStatus}
                    </GlassBadge>
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-muted)] whitespace-nowrap text-xs">
                    {new Date(order.createdAt).toLocaleDateString("en-IN")}
                  </td>
                  <td className="py-3">
                    <Link href={`/admin/orders/${order.id}`} className="text-xs text-[var(--color-text-secondary)] hover:text-[var(--color-gold)] transition-colors">
                      View →
                    </Link>
                  </td>
                </tr>
              ))}
              {orders.length === 0 && (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                    No orders found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-[var(--color-glass-border)]">
            <p className="text-xs text-[var(--color-text-muted)]">
              Showing {(page - 1) * perPage + 1}–{Math.min(page * perPage, total)} of {total}
            </p>
            <div className="flex gap-2">
              {page > 1 && (
                <Link
                  href={`/admin/orders?${new URLSearchParams({ ...(status ? { status } : {}), ...(q ? { q } : {}), page: String(page - 1) })}`}
                  className="px-3 py-1.5 text-xs glass rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-gold)]"
                >
                  Previous
                </Link>
              )}
              {page < totalPages && (
                <Link
                  href={`/admin/orders?${new URLSearchParams({ ...(status ? { status } : {}), ...(q ? { q } : {}), page: String(page + 1) })}`}
                  className="px-3 py-1.5 text-xs glass rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-gold)]"
                >
                  Next
                </Link>
              )}
            </div>
          </div>
        )}
      </GlassCard>
    </div>
  );
}
