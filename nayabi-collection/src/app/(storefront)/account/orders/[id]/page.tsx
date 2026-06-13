import { notFound } from "next/navigation";
import Link from "next/link";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard, GlassBadge, GlassButton } from "@/components/ui";
import { formatPrice } from "@/lib/utils";
import { RETURN_WINDOW_DAYS } from "@/lib/constants";
import { Package, Truck, CheckCircle, XCircle, Clock, RotateCcw } from "lucide-react";

export const metadata = { title: "Order Details" };

const statusVariant = {
  PENDING: "neutral", CONFIRMED: "info", PROCESSING: "info", SHIPPED: "gold",
  DELIVERED: "success", CANCELLED: "error", RETURNED: "warning", REFUNDED: "warning",
} as const;

const timelineSteps = [
  { status: "PENDING", label: "Order placed", icon: Clock },
  { status: "CONFIRMED", label: "Confirmed", icon: CheckCircle },
  { status: "PROCESSING", label: "Processing", icon: Package },
  { status: "SHIPPED", label: "Shipped", icon: Truck },
  { status: "DELIVERED", label: "Delivered", icon: CheckCircle },
] as const;

const statusOrder = ["PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED"];

export default async function OrderDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await auth();

  const order = await db.order.findFirst({
    where: { id, userId: session!.user.id },
    include: {
      items: {
        include: {
          product: { select: { slug: true } },
          variant: { select: { color: true, size: true, fabric: true } },
        },
      },
      address: true,
      shipment: { include: { events: { orderBy: { timestamp: "desc" } } } },
      payment: true,
      returnRequests: { include: { items: true } },
    },
  });

  if (!order) notFound();

  const currentStep = statusOrder.indexOf(order.orderStatus);
  const returnWindowMs = RETURN_WINDOW_DAYS * 24 * 60 * 60 * 1000;
  const canReturn =
    order.orderStatus === "DELIVERED" &&
    Date.now() - order.updatedAt.getTime() < returnWindowMs &&
    order.returnRequests.filter((r) => r.status !== "REJECTED").length === 0;

  const existingReturn = order.returnRequests[0];

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">
            {order.orderNumber}
          </h2>
          <p className="mt-1 text-sm text-[var(--color-text-muted)]">
            Placed on{" "}
            {new Date(order.createdAt).toLocaleDateString("en-IN", {
              day: "numeric",
              month: "long",
              year: "numeric",
            })}
          </p>
        </div>
        <GlassBadge variant={statusVariant[order.orderStatus]}>{order.orderStatus}</GlassBadge>
      </div>

      {/* Timeline */}
      {!["CANCELLED", "RETURNED", "REFUNDED"].includes(order.orderStatus) && (
        <GlassCard padding="md">
          <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            Order tracking
          </h3>
          <ol className="relative flex flex-col gap-0" aria-label="Order progress">
            {timelineSteps.map((step, idx) => {
              const done = idx <= currentStep;
              const active = idx === currentStep;
              const Icon = done ? CheckCircle : step.icon;
              return (
                <li key={step.status} className="flex items-start gap-3 pb-4 last:pb-0">
                  <div className="flex flex-col items-center">
                    <div
                      className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border ${
                        done
                          ? "border-[var(--color-gold)] bg-[var(--color-gold)]/20 text-[var(--color-gold)]"
                          : "border-[var(--color-glass-border)] text-[var(--color-text-muted)]"
                      }`}
                      aria-current={active ? "step" : undefined}
                    >
                      <Icon className="h-4 w-4" aria-hidden="true" />
                    </div>
                    {idx < timelineSteps.length - 1 && (
                      <div
                        className={`my-1 w-0.5 flex-1 ${done ? "bg-[var(--color-gold)]/40" : "bg-[var(--color-glass-border)]"}`}
                        style={{ minHeight: 16 }}
                      />
                    )}
                  </div>
                  <div className="pt-1">
                    <p
                      className={`text-sm font-medium ${done ? "text-[var(--color-text-primary)]" : "text-[var(--color-text-muted)]"}`}
                    >
                      {step.label}
                    </p>
                    {order.shipment && step.status === "SHIPPED" && done && (
                      <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
                        {order.shipment.carrier} · {order.shipment.trackingNumber}
                      </p>
                    )}
                  </div>
                </li>
              );
            })}
          </ol>

          {order.shipment?.trackingUrl && (
            <a
              href={order.shipment.trackingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-4 inline-flex items-center gap-1.5 text-sm text-[var(--color-gold)] underline underline-offset-2"
            >
              <Truck className="h-4 w-4" aria-hidden="true" />
              Track shipment
            </a>
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
                <img
                  src={item.imageUrl}
                  alt={item.productName}
                  className="h-16 w-16 rounded-lg object-cover"
                />
              )}
              <div className="flex-1 min-w-0">
                <Link
                  href={`/products/${item.product.slug}`}
                  className="text-sm font-medium text-[var(--color-text-primary)] hover:text-[var(--color-gold)] transition-colors"
                >
                  {item.productName}
                </Link>
                <p className="text-xs text-[var(--color-text-muted)] mt-0.5">
                  {item.variantDetails}
                </p>
                <p className="text-xs text-[var(--color-text-secondary)] mt-0.5">
                  Qty: {item.quantity}
                </p>
              </div>
              <p className="text-sm font-semibold text-[var(--color-gold)] shrink-0">
                {formatPrice(item.totalPrice)}
              </p>
            </li>
          ))}
        </ul>
      </GlassCard>

      {/* Price summary */}
      <GlassCard padding="md">
        <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
          Payment summary
        </h3>
        <dl className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-[var(--color-text-secondary)]">Subtotal</dt>
            <dd className="text-[var(--color-text-primary)]">{formatPrice(order.subtotal)}</dd>
          </div>
          {order.discountAmount > 0 && (
            <div className="flex justify-between">
              <dt className="text-[var(--color-text-secondary)]">Discount</dt>
              <dd className="text-[var(--color-success)]">−{formatPrice(order.discountAmount)}</dd>
            </div>
          )}
          <div className="flex justify-between">
            <dt className="text-[var(--color-text-secondary)]">Shipping</dt>
            <dd className="text-[var(--color-text-primary)]">
              {order.shippingCost === 0 ? "Free" : formatPrice(order.shippingCost)}
            </dd>
          </div>
          {order.codCharge > 0 && (
            <div className="flex justify-between">
              <dt className="text-[var(--color-text-secondary)]">COD charge</dt>
              <dd className="text-[var(--color-text-primary)]">{formatPrice(order.codCharge)}</dd>
            </div>
          )}
          {order.taxAmount > 0 && (
            <div className="flex justify-between">
              <dt className="text-[var(--color-text-secondary)]">GST (5%)</dt>
              <dd className="text-[var(--color-text-primary)]">{formatPrice(order.taxAmount)}</dd>
            </div>
          )}
          <div className="flex justify-between border-t border-[var(--color-glass-border)] pt-2 mt-1">
            <dt className="font-semibold text-[var(--color-text-primary)]">Total</dt>
            <dd className="font-bold text-[var(--color-gold)]">{formatPrice(order.total)}</dd>
          </div>
          <div className="flex justify-between mt-1">
            <dt className="text-[var(--color-text-secondary)]">Payment</dt>
            <dd className="text-[var(--color-text-primary)]">{order.paymentMethod}</dd>
          </div>
        </dl>
      </GlassCard>

      {/* Shipping address */}
      {order.address && (
        <GlassCard padding="md">
          <h3 className="mb-3 text-sm font-semibold text-[var(--color-text-primary)]">
            Shipping address
          </h3>
          <address className="not-italic text-sm text-[var(--color-text-secondary)] leading-relaxed">
            {order.address.name}
            <br />
            {order.address.line1}
            {order.address.line2 && <>, {order.address.line2}</>}
            <br />
            {order.address.city}, {order.address.state} – {order.address.pincode}
            <br />
            +91 {order.address.phone}
          </address>
        </GlassCard>
      )}

      {/* Return request status */}
      {existingReturn && (
        <GlassCard padding="md">
          <div className="flex items-center gap-2">
            <RotateCcw className="h-4 w-4 text-[var(--color-gold)]" aria-hidden="true" />
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
              Return / Exchange
            </h3>
          </div>
          <div className="mt-3 flex items-center justify-between">
            <p className="text-sm text-[var(--color-text-secondary)]">
              Request status:{" "}
              <span className="text-[var(--color-text-primary)] font-medium">
                {existingReturn.status}
              </span>
            </p>
            {existingReturn.status === "REJECTED" && (
              <GlassBadge variant="error">Rejected</GlassBadge>
            )}
          </div>
          {existingReturn.adminNote && (
            <p className="mt-2 text-xs text-[var(--color-text-muted)]">
              Note: {existingReturn.adminNote}
            </p>
          )}
        </GlassCard>
      )}

      {/* Return / Cancel actions */}
      <div className="flex flex-wrap gap-3">
        {canReturn && (
          <Link href={`/account/orders/${order.id}/return`}>
            <GlassButton variant="secondary" size="sm">
              <RotateCcw className="h-4 w-4 mr-1.5" aria-hidden="true" />
              Return / Exchange
            </GlassButton>
          </Link>
        )}
        {order.orderStatus === "CANCELLED" && (
          <div className="flex items-center gap-2 text-sm text-[var(--color-error)]">
            <XCircle className="h-4 w-4" aria-hidden="true" />
            Order was cancelled
          </div>
        )}
      </div>
    </div>
  );
}
