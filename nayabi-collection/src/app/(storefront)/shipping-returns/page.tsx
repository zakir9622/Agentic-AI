import { GlassCard } from "@/components/ui";
import { Truck, RotateCcw, Clock, IndianRupee } from "lucide-react";
import { formatPrice } from "@/lib/utils";
import { FREE_SHIPPING_THRESHOLD, FLAT_SHIPPING_RATE, RETURN_WINDOW_DAYS } from "@/lib/constants";

export const metadata = {
  title: "Shipping & Returns",
  description: "Shipping rates, delivery timelines, and return policy for Nayabi Collection.",
};

export default function ShippingReturnsPage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-16">
        <div className="mb-10 text-center">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            Shipping &{" "}
            <span className="text-[var(--color-gold)]">Returns</span>
          </h1>
        </div>

        {/* Shipping */}
        <GlassCard padding="lg" className="mb-6">
          <div className="flex items-center gap-3 mb-5">
            <Truck className="h-5 w-5 text-[var(--color-gold)]" aria-hidden="true" />
            <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">
              Shipping
            </h2>
          </div>
          <div className="space-y-4 text-sm text-[var(--color-text-secondary)] leading-relaxed">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-lg border border-[var(--color-glass-border)] p-4">
                <div className="flex items-center gap-2 mb-2">
                  <IndianRupee className="h-4 w-4 text-[var(--color-gold)]" aria-hidden="true" />
                  <span className="text-xs font-semibold text-[var(--color-text-primary)] uppercase tracking-wide">
                    Standard shipping
                  </span>
                </div>
                <p className="text-[var(--color-text-primary)] font-medium">
                  {formatPrice(FLAT_SHIPPING_RATE)}
                </p>
                <p className="text-xs mt-1">Orders below {formatPrice(FREE_SHIPPING_THRESHOLD)}</p>
              </div>
              <div className="rounded-lg border border-[var(--color-gold)]/40 bg-[var(--color-gold)]/5 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <Truck className="h-4 w-4 text-[var(--color-gold)]" aria-hidden="true" />
                  <span className="text-xs font-semibold text-[var(--color-gold)] uppercase tracking-wide">
                    Free shipping
                  </span>
                </div>
                <p className="text-[var(--color-gold)] font-medium">FREE</p>
                <p className="text-xs mt-1 text-[var(--color-text-secondary)]">
                  On orders above {formatPrice(FREE_SHIPPING_THRESHOLD)}
                </p>
              </div>
            </div>
            <div className="flex items-start gap-2">
              <Clock className="h-4 w-4 mt-0.5 shrink-0 text-[var(--color-gold)]" aria-hidden="true" />
              <div>
                <p className="font-medium text-[var(--color-text-primary)]">Delivery timeline</p>
                <p>Metro cities: 3–5 business days</p>
                <p>Rest of India: 5–8 business days</p>
                <p>Remote areas: 7–10 business days</p>
              </div>
            </div>
            <p>
              We ship via Shiprocket-integrated partners including Delhivery, Ekart, and
              Blue Dart. You&apos;ll receive a tracking link via SMS and WhatsApp once your order ships.
            </p>
            <p>
              <strong className="text-[var(--color-text-primary)]">Cash on Delivery (COD)</strong>{" "}
              is available across most pincodes. A convenience charge of ₹50 applies to COD orders.
            </p>
          </div>
        </GlassCard>

        {/* Returns */}
        <GlassCard padding="lg" className="mb-6">
          <div className="flex items-center gap-3 mb-5">
            <RotateCcw className="h-5 w-5 text-[var(--color-gold)]" aria-hidden="true" />
            <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">
              Returns & Exchanges
            </h2>
          </div>
          <div className="space-y-4 text-sm text-[var(--color-text-secondary)] leading-relaxed">
            <p>
              We accept returns and exchanges within{" "}
              <strong className="text-[var(--color-text-primary)]">
                {RETURN_WINDOW_DAYS} days
              </strong>{" "}
              of delivery for most items.
            </p>
            <div>
              <p className="font-medium text-[var(--color-text-primary)] mb-1">
                Items eligible for return:
              </p>
              <ul className="list-disc list-inside space-y-1">
                <li>Unused, unwashed, and in original condition</li>
                <li>Tags and packaging intact</li>
                <li>Received in the last {RETURN_WINDOW_DAYS} days</li>
              </ul>
            </div>
            <div>
              <p className="font-medium text-[var(--color-text-primary)] mb-1">
                Items NOT eligible for return:
              </p>
              <ul className="list-disc list-inside space-y-1">
                <li>Innerwear, underscarf caps (hygiene reasons)</li>
                <li>Items marked as &quot;Final Sale&quot;</li>
                <li>Gift cards</li>
              </ul>
            </div>
            <div>
              <p className="font-medium text-[var(--color-text-primary)] mb-2">
                How to return:
              </p>
              <ol className="list-decimal list-inside space-y-2">
                <li>Go to My Account → Orders → Select order → &quot;Return / Exchange&quot;</li>
                <li>Select items and provide reason</li>
                <li>Choose refund method (original payment, store credit, or bank transfer)</li>
                <li>We&apos;ll arrange a free pickup within 48 hours of approval</li>
                <li>Refund processed within 5–7 business days after we receive the item</li>
              </ol>
            </div>
          </div>
        </GlassCard>

        {/* Damage & Wrong item */}
        <GlassCard padding="md">
          <h2 className="text-sm font-semibold text-[var(--color-text-primary)] mb-2">
            Received a damaged or wrong item?
          </h2>
          <p className="text-sm text-[var(--color-text-secondary)]">
            Please contact us within 48 hours at{" "}
            <a
              href="mailto:support@nayabicollection.com"
              className="text-[var(--color-gold)] underline underline-offset-2"
            >
              support@nayabicollection.com
            </a>{" "}
            with your order number and photos. We&apos;ll make it right immediately.
          </p>
        </GlassCard>
      </div>
    </div>
  );
}
