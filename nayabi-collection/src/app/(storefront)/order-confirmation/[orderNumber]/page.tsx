import Link from "next/link";
import { notFound } from "next/navigation";
import { CheckCircle, Package, Truck } from "lucide-react";
import { GlassButton, GlassCard, GlassBadge } from "@/components/ui";
import { db } from "@/lib/db";
import { formatPrice } from "@/lib/utils";

export const metadata = { title: "Order Confirmed" };

export default async function OrderConfirmationPage({
  params,
}: {
  params: Promise<{ orderNumber: string }>;
}) {
  const { orderNumber } = await params;

  let order;
  try {
    order = await db.order.findUnique({
      where: { orderNumber },
      include: { items: true },
    });
  } catch {
    order = null;
  }
  if (!order) notFound();

  const isCOD = order.paymentMethod === "COD";

  return (
    <div className="mesh-bg">
      <div className="relative z-10 mx-auto max-w-2xl px-4 py-16">
        <div className="text-center">
          <CheckCircle className="mx-auto h-14 w-14 text-[var(--color-success)]" aria-hidden="true" />
          <h1 className="mt-4 text-3xl sm:text-4xl text-[var(--color-text-primary)]">
            Shukran! Order confirmed
          </h1>
          <p className="mt-2 text-[var(--color-text-secondary)]">
            Order <strong className="text-[var(--color-gold)]">{order.orderNumber}</strong>
            {isCOD
              ? ` — keep ${formatPrice(order.total)} ready at delivery.`
              : " — payment received."}
          </p>
        </div>

        <GlassCard padding="lg" className="mt-8">
          <ul className="flex flex-col gap-4" role="list">
            {order.items.map((i) => (
              <li key={i.id} className="flex justify-between gap-4 text-sm">
                <span className="text-[var(--color-text-primary)]">
                  {i.productName}
                  <span className="text-[var(--color-text-muted)]"> · {i.variantDetails} × {i.quantity}</span>
                </span>
                <span className="text-[var(--color-text-secondary)]">{formatPrice(i.totalPrice)}</span>
              </li>
            ))}
          </ul>

          <dl className="mt-5 flex flex-col gap-1.5 border-t border-[var(--color-glass-border)] pt-4 text-sm">
            <div className="flex justify-between">
              <dt className="text-[var(--color-text-secondary)]">Subtotal</dt>
              <dd>{formatPrice(order.subtotal)}</dd>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between text-[var(--color-success)]">
                <dt>Discount</dt>
                <dd>−{formatPrice(order.discountAmount)}</dd>
              </div>
            )}
            <div className="flex justify-between">
              <dt className="text-[var(--color-text-secondary)]">Shipping</dt>
              <dd>{order.shippingCost === 0 ? "Free" : formatPrice(order.shippingCost)}</dd>
            </div>
            {order.codCharge > 0 && (
              <div className="flex justify-between">
                <dt className="text-[var(--color-text-secondary)]">COD fee</dt>
                <dd>{formatPrice(order.codCharge)}</dd>
              </div>
            )}
            <div className="mt-2 flex justify-between border-t border-[var(--color-glass-border)] pt-3 text-base">
              <dt className="font-semibold">Total</dt>
              <dd className="font-bold text-[var(--color-gold)]">{formatPrice(order.total)}</dd>
            </div>
          </dl>

          <div className="mt-5 flex items-center gap-2">
            <GlassBadge variant={isCOD ? "warning" : "success"}>
              {isCOD ? "Cash on Delivery" : "Paid"}
            </GlassBadge>
            <GlassBadge variant="info">{order.orderStatus}</GlassBadge>
          </div>
        </GlassCard>

        <GlassCard padding="md" className="mt-4">
          <div className="flex items-start gap-3 text-sm text-[var(--color-text-secondary)]">
            <Package className="mt-0.5 h-5 w-5 shrink-0 text-[var(--color-gold)]" aria-hidden="true" />
            <p>
              We&apos;ll email and WhatsApp you when your order ships.
              Estimated delivery: <strong className="text-[var(--color-text-primary)]">3–7 business days</strong>.
            </p>
          </div>
          <div className="mt-3 flex items-start gap-3 text-sm text-[var(--color-text-secondary)]">
            <Truck className="mt-0.5 h-5 w-5 shrink-0 text-[var(--color-gold)]" aria-hidden="true" />
            <p>7-day easy returns from the day of delivery.</p>
          </div>
        </GlassCard>

        <div className="mt-8 text-center">
          <Link href="/shop">
            <GlassButton variant="secondary">Continue shopping</GlassButton>
          </Link>
        </div>
      </div>
    </div>
  );
}
