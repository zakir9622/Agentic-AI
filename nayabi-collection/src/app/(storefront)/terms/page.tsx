import { GlassCard } from "@/components/ui";

export const metadata = {
  title: "Terms & Conditions",
  description: "Terms and conditions for using Nayabi Collection.",
};

export default function TermsPage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-16">
        <div className="mb-10">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            Terms &{" "}
            <span className="text-[var(--color-gold)]">Conditions</span>
          </h1>
          <p className="mt-2 text-sm text-[var(--color-text-muted)]">
            Last updated: June 2026
          </p>
        </div>

        <GlassCard padding="lg">
          <div className="space-y-8 text-sm text-[var(--color-text-secondary)] leading-relaxed">
            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                1. Acceptance of terms
              </h2>
              <p>
                By accessing and using nayabicollection.com, you agree to be bound by these
                Terms and Conditions. If you do not agree, please do not use our website.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                2. Products and pricing
              </h2>
              <p>
                All prices are listed in Indian Rupees (INR) and include applicable GST. We
                reserve the right to change prices at any time. Product descriptions and images
                are as accurate as possible, but minor variations in colour may occur due to
                screen settings.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                3. Orders and payment
              </h2>
              <p>
                An order confirmation email does not constitute acceptance of your order.
                Acceptance occurs when your order ships. We reserve the right to cancel orders
                for reasons including but not limited to stock unavailability, pricing errors,
                or suspected fraud.
              </p>
              <p className="mt-2">
                Payments are processed securely by Razorpay. We do not store payment card
                details on our servers.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                4. Shipping and delivery
              </h2>
              <p>
                Delivery timelines are estimates and not guaranteed. We are not responsible
                for delays caused by courier partners, natural disasters, or other events
                beyond our control.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                5. Returns and refunds
              </h2>
              <p>
                Our return policy is detailed on the{" "}
                <a href="/shipping-returns" className="text-[var(--color-gold)] underline underline-offset-2">
                  Shipping &amp; Returns
                </a>{" "}
                page and forms part of these terms.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                6. Intellectual property
              </h2>
              <p>
                All content on this website, including text, images, logos, and design, is the
                property of Nayabi Collection and protected by copyright. You may not reproduce
                or distribute any content without our written permission.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                7. Limitation of liability
              </h2>
              <p>
                To the maximum extent permitted by Indian law, Nayabi Collection shall not be
                liable for indirect, incidental, or consequential damages arising from the use
                of our website or products.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                8. Governing law
              </h2>
              <p>
                These terms are governed by the laws of India. Any disputes shall be subject to
                the exclusive jurisdiction of courts in Mumbai, Maharashtra.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                9. Contact
              </h2>
              <p>
                For queries, contact us at{" "}
                <a
                  href="mailto:support@nayabicollection.com"
                  className="text-[var(--color-gold)] underline underline-offset-2"
                >
                  support@nayabicollection.com
                </a>
                .
              </p>
            </section>
          </div>
        </GlassCard>
      </div>
    </div>
  );
}
