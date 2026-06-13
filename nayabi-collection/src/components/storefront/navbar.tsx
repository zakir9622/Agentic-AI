"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Search, ShoppingBag, User, Menu, X, Heart } from "lucide-react";
import { useCart } from "@/store/cart";
import { cn } from "@/lib/utils";

const navLinks = [
  { label: "Shop All", href: "/shop" },
  { label: "Hijabs", href: "/shop?category=hijabs" },
  { label: "Abayas", href: "/shop?category=abayas" },
  { label: "Namaz Scarfs", href: "/shop?category=namaz-scarfs" },
];

export function Navbar() {
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [searchOpen, setSearchOpen] = React.useState(false);
  const [query, setQuery] = React.useState("");
  const pathname = usePathname();
  const router = useRouter();
  const cartCount = useCart((s) => s.items.reduce((n, i) => n + i.quantity, 0));
  const openCart = useCart((s) => s.open);

  // Close the mobile menu when navigation occurs (state-during-render pattern)
  const [prevPath, setPrevPath] = React.useState(pathname);
  if (prevPath !== pathname) {
    setPrevPath(pathname);
    setMobileOpen(false);
  }

  function submitSearch(e: React.FormEvent) {
    e.preventDefault();
    if (query.trim()) {
      router.push(`/shop?q=${encodeURIComponent(query.trim())}`);
      setSearchOpen(false);
    }
  }

  return (
    <header className="sticky top-0 z-40">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:px-4 focus:py-2 focus:rounded-lg focus:bg-[var(--color-gold)] focus:text-[var(--color-text-inverse)]"
      >
        Skip to content
      </a>

      <nav
        aria-label="Main navigation"
        className="glass glass-elevated !rounded-none border-x-0 border-t-0 px-4 sm:px-6 lg:px-10"
      >
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4">
          {/* Mobile menu button */}
          <button
            className="lg:hidden p-2 -ml-2 text-[var(--color-text-primary)]"
            onClick={() => setMobileOpen((v) => !v)}
            aria-expanded={mobileOpen}
            aria-controls="mobile-nav"
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>

          {/* Logo */}
          <Link href="/" className="font-display text-xl sm:text-2xl tracking-wide text-[var(--color-text-primary)]">
            Nayabi<span className="text-[var(--color-gold)]"> Collection</span>
          </Link>

          {/* Desktop links */}
          <ul className="hidden lg:flex items-center gap-8" role="list">
            {navLinks.map((l) => (
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

          {/* Actions */}
          <div className="flex items-center gap-1 sm:gap-2">
            <button
              onClick={() => setSearchOpen((v) => !v)}
              aria-expanded={searchOpen}
              aria-label="Search products"
              className="p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
            >
              <Search className="h-5 w-5" aria-hidden="true" />
            </button>
            <Link
              href="/account/wishlist"
              aria-label="Wishlist"
              className="hidden sm:block p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
            >
              <Heart className="h-5 w-5" aria-hidden="true" />
            </Link>
            <Link
              href="/account"
              aria-label="My account"
              className="p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
            >
              <User className="h-5 w-5" aria-hidden="true" />
            </Link>
            <button
              onClick={openCart}
              aria-label={`Shopping bag, ${cartCount} item${cartCount === 1 ? "" : "s"}`}
              className="relative p-2 text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] transition-colors"
            >
              <ShoppingBag className="h-5 w-5" aria-hidden="true" />
              {cartCount > 0 && (
                <span
                  aria-hidden="true"
                  className="absolute -top-0.5 -right-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--color-gold)] px-1 text-[10px] font-bold text-[var(--color-text-inverse)]"
                >
                  {cartCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Search bar */}
        {searchOpen && (
          <form
            onSubmit={submitSearch}
            className="mx-auto max-w-7xl pb-4"
            role="search"
          >
            <input
              autoFocus
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search hijabs, abayas, scarfs…"
              aria-label="Search products"
              className="glass-input"
            />
          </form>
        )}

        {/* Mobile nav */}
        <div
          id="mobile-nav"
          className={cn("lg:hidden overflow-hidden", mobileOpen ? "pb-4" : "hidden")}
        >
          <ul className="flex flex-col gap-1" role="list">
            {navLinks.map((l) => (
              <li key={l.href}>
                <Link
                  href={l.href}
                  className="block rounded-lg px-3 py-2.5 text-sm text-[var(--color-text-secondary)] hover:bg-[var(--color-glass-bg)] hover:text-[var(--color-text-primary)]"
                >
                  {l.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </nav>
    </header>
  );
}
