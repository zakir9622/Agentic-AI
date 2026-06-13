import { GlassCard } from "@/components/ui";

export const metadata = {
  title: "Privacy Policy",
  description: "How Nayabi Collection collects, uses, and protects your personal data.",
};

export default function PrivacyPage() {
  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-3xl px-4 sm:px-6 py-16">
        <div className="mb-10">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            Privacy <span className="text-[var(--color-gold)]">Policy</span>
          </h1>
          <p className="mt-2 text-sm text-[var(--color-text-muted)]">
            Last updated: June 2026
          </p>
        </div>

        <GlassCard padding="lg" className="prose prose-sm max-w-none">
          <div className="space-y-8 text-sm text-[var(--color-text-secondary)] leading-relaxed">
            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                1. Information we collect
              </h2>
              <p>When you use Nayabi Collection, we collect:</p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>Account information: name, email, password (hashed), phone number</li>
                <li>Order details: shipping address, items purchased, payment method</li>
                <li>Device data: browser type, IP address, pages visited</li>
                <li>Communications: emails, messages you send us</li>
              </ul>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                2. How we use your information
              </h2>
              <ul className="list-disc list-inside space-y-1">
                <li>To process and fulfil your orders</li>
                <li>To send order confirmations, shipping updates, and receipts</li>
                <li>To handle returns, exchanges, and customer support</li>
                <li>To send promotional emails (only with your consent)</li>
                <li>To improve our website and product offerings</li>
                <li>To comply with Indian tax and legal requirements (GST)</li>
              </ul>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                3. Sharing your information
              </h2>
              <p>
                We do not sell your personal data. We share it only with trusted third parties
                necessary to operate our business:
              </p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>
                  <strong className="text-[var(--color-text-primary)]">Razorpay</strong> — payment
                  processing
                </li>
                <li>
                  <strong className="text-[var(--color-text-primary)]">Shiprocket</strong> — order
                  shipping
                </li>
                <li>
                  <strong className="text-[var(--color-text-primary)]">MSG91</strong> — SMS/WhatsApp
                  notifications
                </li>
                <li>
                  <strong className="text-[var(--color-text-primary)]">Resend</strong> — transactional
                  emails
                </li>
              </ul>
              <p className="mt-2">
                All partners are contractually bound to protect your data and use it only for
                the stated purpose.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                4. Data retention
              </h2>
              <p>
                We retain your account data for as long as your account is active. Order records
                are kept for 7 years as required by Indian tax law. You may request deletion of
                your account at any time.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                5. Your rights (DPDP Act, 2023)
              </h2>
              <p>Under the Digital Personal Data Protection Act, 2023, you have the right to:</p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>Access the personal data we hold about you</li>
                <li>Correct inaccurate personal data</li>
                <li>Erase your personal data (subject to legal requirements)</li>
                <li>Withdraw consent for marketing communications</li>
                <li>Nominate a representative for data purposes</li>
              </ul>
              <p className="mt-2">
                To exercise your rights, email{" "}
                <a
                  href="mailto:privacy@nayabicollection.com"
                  className="text-[var(--color-gold)] underline underline-offset-2"
                >
                  privacy@nayabicollection.com
                </a>
                . We will respond within 30 days.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                6. Cookies
              </h2>
              <p>
                We use strictly necessary cookies for session management and cart functionality.
                With your consent, we also use analytics cookies to understand how you use our
                site. You can manage cookie preferences via the banner on your first visit.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                7. Security
              </h2>
              <p>
                We use industry-standard security measures including HTTPS, encrypted passwords
                (bcrypt), rate limiting, and signed webhooks. No method is 100% secure, but we
                take every reasonable precaution to protect your data.
              </p>
            </section>

            <section>
              <h2 className="text-base font-semibold text-[var(--color-text-primary)] mb-2">
                8. Contact
              </h2>
              <p>
                For privacy-related queries, contact our Data Protection Officer at{" "}
                <a
                  href="mailto:privacy@nayabicollection.com"
                  className="text-[var(--color-gold)] underline underline-offset-2"
                >
                  privacy@nayabicollection.com
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
