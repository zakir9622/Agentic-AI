import { db } from "@/lib/db";
import { formatPrice } from "@/lib/utils";
import { GlassCard } from "@/components/ui";
import { ShoppingBag, TrendingUp, Users, RotateCcw } from "lucide-react";

export const metadata = { title: "Admin Dashboard" };
export const revalidate = 60;

async function getKPIs() {
  const now = new Date();
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);

  const [
    ordersThisMonth,
    ordersLastMonth,
    revenueThis,
    revenueLast,
    totalCustomers,
    pendingReturns,
    recentOrders,
    lowStock,
  ] = await Promise.all([
    db.order.count({ where: { createdAt: { gte: startOfMonth } } }),
    db.order.count({
      where: { createdAt: { gte: startOfLastMonth, lt: startOfMonth } },
    }),
    db.order.aggregate({
      _sum: { total: true },
      where: {
        createdAt: { gte: startOfMonth },
        orderStatus: { notIn: ["CANCELLED", "REFUNDED"] },
      },
    }),
    db.order.aggregate({
      _sum: { total: true },
      where: {
        createdAt: { gte: startOfLastMonth, lt: startOfMonth },
        orderStatus: { notIn: ["CANCELLED", "REFUNDED"] },
      },
    }),
    db.user.count(),
    db.returnRequest.count({ where: { status: "PENDING" } }),
    db.order.findMany({
      take: 8,
      orderBy: { createdAt: "desc" },
      include: { items: { select: { productName: true } } },
    }),
    db.productVariant.count({ where: { stock: { gt: 0, lte: 5 }, isActive: true } }),
  ]);

  const revThis = revenueThis._sum.total ?? 0;
  const revLast = revenueLast._sum.total ?? 0;
  const revGrowth = revLast > 0 ? ((revThis - revLast) / revLast) * 100 : 0;
  const ordGrowth = ordersLastMonth > 0
    ? ((ordersThisMonth - ordersLastMonth) / ordersLastMonth) * 100
    : 0;

  return {
    ordersThisMonth, ordGrowth,
    revThis, revGrowth,
    totalCustomers, pendingReturns,
    recentOrders, lowStock,
  };
}

const orderStatusColors: Record<string, string> = {
  PENDING: "text-[var(--color-text-muted)]",
  CONFIRMED: "text-[var(--color-info)]",
  PROCESSING: "text-[var(--color-info)]",
  SHIPPED: "text-[var(--color-gold)]",
  DELIVERED: "text-[var(--color-success)]",
  CANCELLED: "text-[var(--color-error)]",
  RETURNED: "text-[var(--color-warning)]",
  REFUNDED: "text-[var(--color-warning)]",
};

export default async function AdminDashboardPage() {
  const data = await getKPIs();

  const kpis = [
    {
      label: "Revenue this month",
      value: formatPrice(data.revThis),
      growth: data.revGrowth,
      icon: TrendingUp,
    },
    {
      label: "Orders this month",
      value: data.ordersThisMonth.toString(),
      growth: data.ordGrowth,
      icon: ShoppingBag,
    },
    {
      label: "Total customers",
      value: data.totalCustomers.toString(),
      growth: null,
      icon: Users,
    },
    {
      label: "Pending returns",
      value: data.pendingReturns.toString(),
      growth: null,
      icon: RotateCcw,
      alert: data.pendingReturns > 0,
    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Dashboard</h2>
        <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
          {new Date().toLocaleDateString("en-IN", {
            weekday: "long",
            day: "numeric",
            month: "long",
            year: "numeric",
          })}
        </p>
      </div>

      {/* KPI cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {kpis.map((kpi) => {
          const Icon = kpi.icon;
          return (
            <GlassCard key={kpi.label} padding="md">
              <div className="flex items-start justify-between">
                <div
                  className={`flex h-9 w-9 items-center justify-center rounded-full ${kpi.alert ? "bg-[var(--color-error)]/15" : "bg-[var(--color-gold)]/15"}`}
                >
                  <Icon
                    className={`h-4 w-4 ${kpi.alert ? "text-[var(--color-error)]" : "text-[var(--color-gold)]"}`}
                    aria-hidden="true"
                  />
                </div>
                {kpi.growth !== null && (
                  <span
                    className={`text-xs font-medium ${kpi.growth >= 0 ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}
                  >
                    {kpi.growth >= 0 ? "+" : ""}
                    {kpi.growth.toFixed(1)}%
                  </span>
                )}
              </div>
              <div className="mt-3">
                <p className="text-2xl font-bold text-[var(--color-text-primary)]">
                  {kpi.value}
                </p>
                <p className="mt-0.5 text-xs text-[var(--color-text-muted)]">{kpi.label}</p>
              </div>
            </GlassCard>
          );
        })}
      </div>

      {/* Alerts */}
      {data.lowStock > 0 && (
        <GlassCard padding="md" className="border-[var(--color-warning)]/40 bg-[var(--color-warning)]/5">
          <p className="text-sm font-medium text-[var(--color-warning)]">
            ⚠ {data.lowStock} variant{data.lowStock === 1 ? "" : "s"} low on stock (≤ 5 units)
          </p>
          <a
            href="/admin/inventory"
            className="mt-1 text-xs text-[var(--color-text-secondary)] underline underline-offset-2"
          >
            View inventory
          </a>
        </GlassCard>
      )}

      {/* Recent orders */}
      <GlassCard padding="md">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Recent orders
          </h3>
          <a
            href="/admin/orders"
            className="text-xs text-[var(--color-gold)] underline underline-offset-2"
          >
            View all
          </a>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Recent orders">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                <th scope="col" className="pb-2 text-left text-xs font-medium text-[var(--color-text-muted)]">Order</th>
                <th scope="col" className="pb-2 text-left text-xs font-medium text-[var(--color-text-muted)]">Items</th>
                <th scope="col" className="pb-2 text-right text-xs font-medium text-[var(--color-text-muted)]">Total</th>
                <th scope="col" className="pb-2 text-right text-xs font-medium text-[var(--color-text-muted)]">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.recentOrders.map((order) => (
                <tr key={order.id} className="border-b border-[var(--color-glass-border)] last:border-0">
                  <td className="py-2.5 pr-4">
                    <a
                      href={`/admin/orders/${order.id}`}
                      className="text-[var(--color-gold)] hover:underline font-medium"
                    >
                      {order.orderNumber}
                    </a>
                    <p className="text-xs text-[var(--color-text-muted)]">
                      {new Date(order.createdAt).toLocaleDateString("en-IN")}
                    </p>
                  </td>
                  <td className="py-2.5 pr-4 text-[var(--color-text-secondary)] max-w-[160px] truncate">
                    {order.items[0]?.productName}
                    {order.items.length > 1 && ` +${order.items.length - 1}`}
                  </td>
                  <td className="py-2.5 pr-4 text-right font-semibold text-[var(--color-text-primary)]">
                    {formatPrice(order.total)}
                  </td>
                  <td className={`py-2.5 text-right text-xs font-medium ${orderStatusColors[order.orderStatus]}`}>
                    {order.orderStatus}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </GlassCard>
    </div>
  );
}
