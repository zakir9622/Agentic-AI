import { db } from "@/lib/db";
import { GlassCard } from "@/components/ui";
import { formatPrice } from "@/lib/utils";

export const metadata = { title: "Analytics | Admin" };
export const revalidate = 300;

async function getAnalytics() {
  const now = new Date();
  const months = Array.from({ length: 6 }, (_, i) => {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    return d;
  }).reverse();

  const monthlyRevenue = await Promise.all(
    months.map(async (start) => {
      const end = new Date(start.getFullYear(), start.getMonth() + 1, 1);
      const agg = await db.order.aggregate({
        _sum: { total: true },
        where: {
          createdAt: { gte: start, lt: end },
          orderStatus: { notIn: ["CANCELLED", "REFUNDED"] },
        },
      });
      const count = await db.order.count({
        where: { createdAt: { gte: start, lt: end } },
      });
      return {
        label: start.toLocaleDateString("en-IN", { month: "short", year: "2-digit" }),
        revenue: agg._sum.total ?? 0,
        orders: count,
      };
    })
  );

  const startOfYear = new Date(now.getFullYear(), 0, 1);
  const [ytdRevenue, ytdOrders, totalRevenue, totalOrders, topProducts] = await Promise.all([
    db.order.aggregate({
      _sum: { total: true },
      where: { createdAt: { gte: startOfYear }, orderStatus: { notIn: ["CANCELLED", "REFUNDED"] } },
    }),
    db.order.count({ where: { createdAt: { gte: startOfYear } } }),
    db.order.aggregate({
      _sum: { total: true, taxAmount: true },
      where: { orderStatus: { notIn: ["CANCELLED", "REFUNDED"] } },
    }),
    db.order.count(),
    db.orderItem.groupBy({
      by: ["productId", "productName"],
      _sum: { totalPrice: true, quantity: true },
      orderBy: { _sum: { totalPrice: "desc" } },
      take: 10,
    }),
  ]);

  const maxRevenue = Math.max(...monthlyRevenue.map((m) => m.revenue), 1);

  return {
    monthlyRevenue,
    maxRevenue,
    ytdRevenue: ytdRevenue._sum.total ?? 0,
    ytdOrders,
    totalRevenue: totalRevenue._sum.total ?? 0,
    totalGST: totalRevenue._sum.taxAmount ?? 0,
    totalOrders,
    topProducts,
  };
}

export default async function AdminAnalyticsPage() {
  const data = await getAnalytics();

  return (
    <div className="flex flex-col gap-6">
      <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Analytics</h2>

      {/* Summary KPIs */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { label: "YTD Revenue", value: formatPrice(data.ytdRevenue) },
          { label: "YTD Orders", value: data.ytdOrders.toString() },
          { label: "All-time Revenue", value: formatPrice(data.totalRevenue) },
          { label: "Total GST Collected", value: formatPrice(data.totalGST) },
        ].map((kpi) => (
          <GlassCard key={kpi.label} padding="md">
            <p className="text-2xl font-bold text-[var(--color-gold)]">{kpi.value}</p>
            <p className="text-xs text-[var(--color-text-muted)] mt-0.5">{kpi.label}</p>
          </GlassCard>
        ))}
      </div>

      {/* Revenue chart (CSS bar chart) */}
      <GlassCard padding="md">
        <h3 className="mb-6 text-sm font-semibold text-[var(--color-text-primary)]">
          Monthly revenue (last 6 months)
        </h3>
        <div className="flex items-end gap-3 h-40" role="img" aria-label="Monthly revenue bar chart">
          {data.monthlyRevenue.map((m) => {
            const pct = data.maxRevenue > 0 ? (m.revenue / data.maxRevenue) * 100 : 0;
            return (
              <div key={m.label} className="flex flex-col items-center gap-2 flex-1">
                <span className="text-xs text-[var(--color-text-muted)]" aria-hidden="true">
                  {formatPrice(m.revenue)}
                </span>
                <div
                  className="w-full rounded-t-lg bg-[var(--color-gold)]/60 hover:bg-[var(--color-gold)] transition-colors"
                  style={{ height: `${Math.max(4, pct)}%` }}
                  role="presentation"
                  title={`${m.label}: ${formatPrice(m.revenue)} (${m.orders} orders)`}
                />
                <span className="text-xs text-[var(--color-text-muted)]">{m.label}</span>
              </div>
            );
          })}
        </div>
      </GlassCard>

      {/* Top products */}
      <GlassCard padding="md">
        <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
          Top products by revenue
        </h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Top products table">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                <th scope="col" className="pb-2 pr-4 text-left text-xs font-medium text-[var(--color-text-muted)]">Product</th>
                <th scope="col" className="pb-2 pr-4 text-right text-xs font-medium text-[var(--color-text-muted)]">Units sold</th>
                <th scope="col" className="pb-2 text-right text-xs font-medium text-[var(--color-text-muted)]">Revenue</th>
              </tr>
            </thead>
            <tbody>
              {data.topProducts.map((p, idx) => (
                <tr key={p.productId} className="border-b border-[var(--color-glass-border)] last:border-0">
                  <td className="py-2.5 pr-4">
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-[var(--color-text-muted)] w-5">#{idx + 1}</span>
                      <span className="text-[var(--color-text-primary)]">{p.productName}</span>
                    </div>
                  </td>
                  <td className="py-2.5 pr-4 text-right text-[var(--color-text-secondary)]">
                    {p._sum.quantity ?? 0}
                  </td>
                  <td className="py-2.5 text-right font-semibold text-[var(--color-gold)]">
                    {formatPrice(p._sum.totalPrice ?? 0)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </GlassCard>

      {/* GST Summary */}
      <GlassCard padding="md">
        <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
          GST Report (IGST @5%)
        </h3>
        <p className="text-sm text-[var(--color-text-secondary)] mb-4">
          All textiles (HSN 6302, 6214) attract 5% GST. Prices on the site are inclusive of GST.
        </p>
        <dl className="grid gap-2 sm:grid-cols-3 text-sm">
          <div className="rounded-lg border border-[var(--color-glass-border)] p-3">
            <dt className="text-xs text-[var(--color-text-muted)]">Total taxable value (all-time)</dt>
            <dd className="mt-1 text-lg font-bold text-[var(--color-text-primary)]">
              {formatPrice(data.totalRevenue - data.totalGST)}
            </dd>
          </div>
          <div className="rounded-lg border border-[var(--color-glass-border)] p-3">
            <dt className="text-xs text-[var(--color-text-muted)]">Total GST collected</dt>
            <dd className="mt-1 text-lg font-bold text-[var(--color-gold)]">
              {formatPrice(data.totalGST)}
            </dd>
          </div>
          <div className="rounded-lg border border-[var(--color-glass-border)] p-3">
            <dt className="text-xs text-[var(--color-text-muted)]">Total revenue (inclusive)</dt>
            <dd className="mt-1 text-lg font-bold text-[var(--color-text-primary)]">
              {formatPrice(data.totalRevenue)}
            </dd>
          </div>
        </dl>
        <p className="mt-4 text-xs text-[var(--color-text-muted)]">
          ⚠ This is a simplified summary. Consult your CA for official GSTR-1 and GSTR-3B filings.
          Export detailed order data for reconciliation.
        </p>
      </GlassCard>
    </div>
  );
}
