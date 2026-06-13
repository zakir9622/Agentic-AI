"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassBadge, GlassInput } from "@/components/ui";
import { updateOrderStatusAction } from "@/app/actions/admin-orders";
import { formatPrice } from "@/lib/utils";

interface OrderDetail {
  id: string;
  orderNumber: string;
  orderStatus: string;
  paymentStatus: string;
  paymentMethod: string;
  total: number;
  subtotal: number;
  discountAmount: number;
  shippingCost: number;
  codCharge: number;
  taxAmount: number;
  createdAt: string;
  user: { name: string; email: string } | null;
  guestEmail: string | null;
  address: {
    name: string; phone: string; line1: string; line2: string | null;
    city: string; state: string; pincode: string;
  } | null;
  items: { id: string; productName: string; variantDetails: string; quantity: number; unitPrice: number; totalPrice: number; imageUrl: string | null }[];
  shipment: { trackingNumber: string | null; carrier: string | null; trackingUrl: string | null; status: string } | null;
  payment: { razorpayPaymentId: string | null; method: string; status: string } | null;
}

const STATUS_FLOW: Record<string, string[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PROCESSING", "CANCELLED"],
  PROCESSING: ["SHIPPED", "CANCELLED"],
  SHIPPED: ["DELIVERED"],
  DELIVERED: [],
  CANCELLED: [],
  RETURNED: [],
  REFUNDED: [],
};

const statusVariant: Record<string, "neutral" | "info" | "gold" | "success" | "error" | "warning"> = {
  PENDING: "neutral", CONFIRMED: "info", PROCESSING: "info", SHIPPED: "gold",
  DELIVERED: "success", CANCELLED: "error", RETURNED: "warning", REFUNDED: "warning",
};

export default function AdminOrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const resolvedParams = React.use(params);
  const [order, setOrder] = React.useState<OrderDetail | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [statusState, statusAction, statusPending] = React.useActionState<
    { ok?: boolean; message?: string },
    FormData
  >(updateOrderStatusAction, { ok: undefined, message: undefined });
  const [showShipForm, setShowShipForm] = React.useState(false);

  React.useEffect(() => {
    fetch(`/api/admin/orders/${resolvedParams.id}`)
      .then((r) => r.json())
      .then((data) => { setOrder(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [resolvedParams.id, statusState]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="h-8 w-8 rounded-full border-2 border-[var(--color-gold)] border-t-transparent animate-spin" aria-label="Loading" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="text-center text-[var(--color-text-muted)] mt-16">Order not found</div>
    );
  }

  const nextStatuses = STATUS_FLOW[order.orderStatus] ?? [];

  return (
    <div className="flex flex-col gap-6 max-w-4xl">
      {/* Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <a href="/admin/orders" className="text-xs text-[var(--color-text-muted)] hover:text-[var(--color-gold)]">
            ← Orders
          </a>
          <h2 className="mt-1 text-xl font-semibold text-[var(--color-text-primary)]">
            {order.orderNumber}
          </h2>
          <p className="text-sm text-[var(--color-text-muted)]">
            {new Date(order.createdAt).toLocaleDateString("en-IN", {
              day: "numeric", month: "long", year: "numeric", hour: "2-digit", minute: "2-digit"
            })}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <GlassBadge variant={statusVariant[order.orderStatus]}>{order.orderStatus}</GlassBadge>
          <GlassBadge variant={order.paymentStatus === "PAID" ? "success" : "neutral"}>
            {order.paymentStatus}
          </GlassBadge>
        </div>
      </div>

      {/* Status update */}
      {nextStatuses.length > 0 && (
        <GlassCard padding="md">
          <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
            Update order status
          </h3>
          {statusState?.message && (
            <p className={`mb-3 text-sm ${statusState.ok ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
              {statusState.message}
            </p>
          )}
          <div className="flex flex-wrap gap-3">
            {nextStatuses.map((status) => (
              <div key={status}>
                {status === "SHIPPED" ? (
                  <GlassButton
                    type="button"
                    size="sm"
                    variant="secondary"
                    onClick={() => setShowShipForm((v) => !v)}
                  >
                    Mark as Shipped
                  </GlassButton>
                ) : (
                  <form action={statusAction}>
                    <input type="hidden" name="orderId" value={order.id} />
                    <input type="hidden" name="status" value={status} />
                    <GlassButton
                      type="submit"
                      size="sm"
                      variant={status === "CANCELLED" ? "danger" : "secondary"}
                      loading={statusPending}
                    >
                      Mark as {status}
                    </GlassButton>
                  </form>
                )}
              </div>
            ))}
          </div>

          {showShipForm && (
            <form action={statusAction} className="mt-4 grid gap-3 sm:grid-cols-3">
              <input type="hidden" name="orderId" value={order.id} />
              <input type="hidden" name="status" value="SHIPPED" />
              <GlassInput label="Tracking number" name="trackingNumber" required />
              <GlassInput label="Carrier" name="carrier" placeholder="Delhivery, Ekart..." />
              <GlassInput label="Tracking URL (optional)" name="trackingUrl" />
              <div className="sm:col-span-3">
                <GlassButton type="submit" size="sm" loading={statusPending}>
                  Confirm shipment
                </GlassButton>
              </div>
            </form>
          )}
        </GlassCard>
      )}

      {/* Items */}
      <GlassCard padding="md">
        <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">Items</h3>
        <ul className="flex flex-col divide-y divide-[var(--color-glass-border)]" role="list">
          {order.items.map((item) => (
            <li key={item.id} className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
              {item.imageUrl && (
                <img src={item.imageUrl} alt={item.productName} className="h-14 w-14 rounded-lg object-cover shrink-0" />
              )}
              <div className="flex-1">
                <p className="text-sm font-medium text-[var(--color-text-primary)]">{item.productName}</p>
                <p className="text-xs text-[var(--color-text-muted)]">{item.variantDetails}</p>
                <p className="text-xs text-[var(--color-text-secondary)]">Qty: {item.quantity} × {formatPrice(item.unitPrice)}</p>
              </div>
              <p className="text-sm font-semibold text-[var(--color-gold)] shrink-0">
                {formatPrice(item.totalPrice)}
              </p>
            </li>
          ))}
        </ul>
        <dl className="mt-4 flex flex-col gap-1.5 border-t border-[var(--color-glass-border)] pt-4 text-sm">
          {[
            { label: "Subtotal", value: formatPrice(order.subtotal) },
            order.discountAmount > 0 && { label: "Discount", value: `−${formatPrice(order.discountAmount)}` },
            { label: "Shipping", value: order.shippingCost === 0 ? "Free" : formatPrice(order.shippingCost) },
            order.codCharge > 0 && { label: "COD charge", value: formatPrice(order.codCharge) },
            order.taxAmount > 0 && { label: "GST", value: formatPrice(order.taxAmount) },
            { label: "Total", value: formatPrice(order.total), bold: true },
          ].filter(Boolean).map((row: { label: string; value: string; bold?: boolean } | false) => {
            if (!row) return null;
            return (
              <div key={row.label} className="flex justify-between">
                <dt className={row.bold ? "font-semibold text-[var(--color-text-primary)]" : "text-[var(--color-text-secondary)]"}>
                  {row.label}
                </dt>
                <dd className={row.bold ? "font-bold text-[var(--color-gold)]" : "text-[var(--color-text-primary)]"}>
                  {row.value}
                </dd>
              </div>
            );
          })}
        </dl>
      </GlassCard>

      <div className="grid gap-4 sm:grid-cols-2">
        {/* Customer */}
        <GlassCard padding="md">
          <h3 className="mb-2 text-sm font-semibold text-[var(--color-text-primary)]">Customer</h3>
          <p className="text-sm text-[var(--color-text-primary)]">
            {order.user?.name ?? "Guest"}
          </p>
          <p className="text-xs text-[var(--color-text-secondary)]">
            {order.user?.email ?? order.guestEmail}
          </p>
        </GlassCard>

        {/* Address */}
        {order.address && (
          <GlassCard padding="md">
            <h3 className="mb-2 text-sm font-semibold text-[var(--color-text-primary)]">
              Shipping address
            </h3>
            <address className="not-italic text-xs text-[var(--color-text-secondary)] leading-relaxed">
              {order.address.name}<br />
              {order.address.line1}{order.address.line2 && `, ${order.address.line2}`}<br />
              {order.address.city}, {order.address.state} – {order.address.pincode}<br />
              +91 {order.address.phone}
            </address>
          </GlassCard>
        )}

        {/* Payment */}
        {order.payment && (
          <GlassCard padding="md">
            <h3 className="mb-2 text-sm font-semibold text-[var(--color-text-primary)]">
              Payment
            </h3>
            <p className="text-xs text-[var(--color-text-secondary)]">Method: {order.payment.method}</p>
            <p className="text-xs text-[var(--color-text-secondary)]">Status: {order.payment.status}</p>
            {order.payment.razorpayPaymentId && (
              <p className="text-xs text-[var(--color-text-muted)] font-mono break-all mt-1">
                {order.payment.razorpayPaymentId}
              </p>
            )}
          </GlassCard>
        )}

        {/* Shipment */}
        {order.shipment && (
          <GlassCard padding="md">
            <h3 className="mb-2 text-sm font-semibold text-[var(--color-text-primary)]">
              Shipment
            </h3>
            <p className="text-xs text-[var(--color-text-secondary)]">
              Carrier: {order.shipment.carrier ?? "—"}
            </p>
            <p className="text-xs text-[var(--color-text-secondary)]">
              Tracking: {order.shipment.trackingNumber ?? "—"}
            </p>
            {order.shipment.trackingUrl && (
              <a
                href={order.shipment.trackingUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs text-[var(--color-gold)] underline mt-1 block"
              >
                Track shipment ↗
              </a>
            )}
          </GlassCard>
        )}
      </div>
    </div>
  );
}
