import Link from "next/link";

const footerColumns = [
  {
    heading: "Shop",
    links: [
      { label: "Hijabs", href: "/shop?category=hijabs" },
      { label: "Abayas", href: "/shop?category=abayas" },
      { label: "Namaz Scarfs", href: "/shop?category=namaz-scarfs" },
      { label: "All Products", href: "/shop" },
    ],
  },
  {
    heading: "Help",
    links: [
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

export function Footer() {
  return (
    <footer className="mt-auto border-t border-[var(--color-glass-border)] bg-[var(--color-bg-mid)]">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-10 py-12">
        <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
          <div className="col-span-2 md:col-span-1">
            <p className="font-display text-xl text-[var(--color-text-primary)]">
              Nayabi<span className="text-[var(--color-gold)]"> Collection</span>
            </p>
            <p className="mt-3 text-sm text-[var(--color-text-secondary)] leading-relaxed">
              Premium modest wear, thoughtfully crafted. Delivered across India.
            </p>
          </div>

          {footerColumns.map((col) => (
            <nav key={col.heading} aria-label={col.heading}>
              <h2 className="text-sm font-semibold text-[var(--color-text-primary)] !font-[var(--font-body)]">
                {col.heading}
              </h2>
              <ul className="mt-4 space-y-2.5" role="list">
                {col.links.map((l) => (
                  <li key={l.href}>
                    <Link
                      href={l.href}
                      className="text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-gold)] transition-colors"
                    >
                      {l.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-[var(--color-glass-border)] pt-6 sm:flex-row">
          <p className="text-xs text-[var(--color-text-muted)]">
            © {new Date().getFullYear()} Nayabi Collection. All rights reserved.
          </p>
          <p className="text-xs text-[var(--color-text-muted)]">
            UPI · Cards · Net Banking · Wallets · Cash on Delivery
          </p>
        </div>
      </div>
    </footer>
  );
}
