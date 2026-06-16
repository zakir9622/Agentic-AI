"use client";

import * as React from "react";
import Link from "next/link";
import { Logo } from "@/components/ui";
import { BRAND } from "@/lib/constants";

function IconInstagram({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="2" y="2" width="20" height="20" rx="5" ry="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="0.5" fill="currentColor" stroke="none" />
    </svg>
  );
}

function IconWhatsApp({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z" />
      <path d="M12 2C6.477 2 2 6.477 2 12c0 1.89.525 3.66 1.438 5.168L2 22l4.979-1.399A9.935 9.935 0 0012 22c5.523 0 10-4.477 10-10S17.523 2 12 2zm0 18.214a8.214 8.214 0 01-4.19-1.148l-.3-.178-3.106.871.872-3.042-.196-.313A8.214 8.214 0 113.786 12 8.214 8.214 0 0112 20.214z" />
    </svg>
  );
}

function IconYoutube({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M23.498 6.186a3.016 3.016 0 00-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 00.502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 002.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 002.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
    </svg>
  );
}

function IconFacebook({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
    </svg>
  );
}

const footerColumns = [
  {
    heading: "Shop",
    links: [
      { label: "Hijabs", href: "/shop?category=hijabs" },
      { label: "Abayas", href: "/shop?category=abayas" },
      { label: "Namaz Scarfs", href: "/shop?category=namaz-scarfs" },
      { label: "All Products", href: "/shop" },
      { label: "Gift Cards", href: "/gift-cards" },
    ],
  },
  {
    heading: "Help",
    links: [
      { label: "Track Order", href: "/track" },
      { label: "Shipping & Returns", href: "/shipping-returns" },
      { label: "Size Guide", href: "/size-guide" },
      { label: "FAQ", href: "/faq" },
      { label: "Contact Us", href: "/contact" },
    ],
  },
  {
    heading: "Company",
    links: [
      { label: "About Us", href: "/about" },
      { label: "Privacy Policy", href: "/privacy" },
      { label: "Terms of Service", href: "/terms" },
    ],
  },
];

const socialLinks = [
  { Icon: IconInstagram, href: BRAND.instagramUrl, label: "Instagram" },
  {
    Icon: IconWhatsApp,
    href: BRAND.whatsappUrl("Hi Nayabi Collection, I'd like to know more."),
    label: "WhatsApp",
  },
  {
    Icon: IconFacebook,
    href: "https://facebook.com/nayabi.collection",
    label: "Facebook",
  },
  { Icon: IconYoutube, href: "https://youtube.com/@nayabi_collection", label: "YouTube" },
];

const paymentMethods = ["UPI", "Visa", "MC", "RuPay", "COD"];

export function Footer() {
  const [email, setEmail] = React.useState("");
  const [subscribed, setSubscribed] = React.useState(false);

  function handleNewsletter(e: React.FormEvent) {
    e.preventDefault();
    if (email.trim()) {
      setSubscribed(true);
      setEmail("");
    }
  }

  return (
    <footer className="relative mt-auto overflow-hidden border-t border-[var(--color-glass-border)]">
      {/* Glassmorphic backdrop — theme-aware surface (see .footer-surface) */}
      <div className="footer-glass footer-surface absolute inset-0" aria-hidden="true" />

      {/* Subtle aurora tint at top */}
      <div
        className="absolute top-0 right-0 left-0 h-px"
        aria-hidden="true"
        style={{
          background:
            "linear-gradient(90deg, transparent, rgba(201,169,110,0.6) 30%, rgba(201,169,110,0.6) 70%, transparent)",
        }}
      />

      <div className="relative z-10 mx-auto max-w-7xl px-4 pt-16 pb-8 sm:px-6 lg:px-10">
        <div className="grid grid-cols-2 gap-10 md:grid-cols-[1.6fr_1fr_1fr_1fr]">
          {/* ── Brand column ── */}
          <div className="col-span-2 md:col-span-1">
            <Link
              href="/"
              aria-label="Nayabi Collection — home"
              className="inline-block transition-opacity hover:opacity-90"
            >
              <Logo markClassName="h-9 w-9" wordmarkClassName="text-2xl" />
            </Link>
            <p className="mt-3 max-w-xs text-sm leading-relaxed text-[var(--color-text-secondary)]">
              Premium modest wear, thoughtfully crafted for Muslim women across India.
              Faith and style, beautifully united.
            </p>

            {/* Contact */}
            <div className="mt-4 flex flex-col gap-1.5 text-sm text-[var(--color-text-secondary)]">
              <a
                href={BRAND.whatsappUrl()}
                target="_blank"
                rel="noopener noreferrer"
                className="transition-colors hover:text-[var(--color-gold)]"
              >
                WhatsApp / Call:{" "}
                <span className="text-[var(--color-text-primary)]">
                  {BRAND.phoneDisplay}
                </span>
              </a>
              <a
                href={BRAND.instagramUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="transition-colors hover:text-[var(--color-gold)]"
              >
                Instagram:{" "}
                <span className="text-[var(--color-text-primary)]">
                  @{BRAND.instagramHandle}
                </span>
              </a>
            </div>

            {/* Newsletter */}
            <div className="mt-6">
              <p className="mb-2.5 text-xs font-semibold tracking-widest text-[var(--color-text-muted)] uppercase">
                Stay updated
              </p>
              {subscribed ? (
                <p className="text-sm text-[var(--color-success)]">
                  Jazak Allah Khair — you&apos;re on the list!
                </p>
              ) : (
                <form onSubmit={handleNewsletter} className="flex max-w-xs gap-2">
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@email.com"
                    aria-label="Email address for newsletter"
                    className="glass-input flex-1 !py-2 !text-sm"
                    required
                  />
                  <button
                    type="submit"
                    className="btn-gold shrink-0 rounded-lg px-4 py-2 text-sm font-semibold"
                  >
                    Join
                  </button>
                </form>
              )}
            </div>

            {/* Social links */}
            <div className="mt-5 flex gap-2.5" aria-label="Social media">
              {socialLinks.map(({ Icon, href, label }) => (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={label}
                  className="social-icon"
                >
                  <Icon className="h-4 w-4" aria-hidden="true" />
                </a>
              ))}
            </div>
          </div>

          {/* ── Nav columns ── */}
          {footerColumns.map((col) => (
            <nav key={col.heading} aria-label={col.heading}>
              <h3 className="mb-4 text-sm !font-[var(--font-body)] font-semibold text-[var(--color-text-primary)]">
                {col.heading}
              </h3>
              <ul className="space-y-2.5" role="list">
                {col.links.map((l) => (
                  <li key={l.href}>
                    <Link
                      href={l.href}
                      className="text-sm text-[var(--color-text-secondary)] transition-colors hover:text-[var(--color-gold)]"
                    >
                      {l.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>

        {/* ── Divider ── */}
        <div className="gold-divider mt-12" aria-hidden="true" />

        {/* ── Bottom bar ── */}
        <div className="mt-6 flex flex-col items-center justify-between gap-4 sm:flex-row">
          <p className="text-xs text-[var(--color-text-muted)]">
            © {new Date().getFullYear()} Nayabi Collection &mdash; Made with care in India
          </p>

          {/* Payment methods */}
          <div
            className="flex items-center gap-1.5"
            aria-label="Accepted payment methods"
          >
            {paymentMethods.map((m) => (
              <span key={m} className="payment-badge">
                {m}
              </span>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
