"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Search, ShoppingBag, User, Menu, X, Heart } from "lucide-react";
import { useCart } from "@/store/cart";
import { ThemeToggle, Logo } from "@/components/ui";
import { cn } from "@/lib/utils";

const navLinks = [
  { label: "Shop All", href: "/shop" },
  { label: "Hijabs", href: "/shop?category=hijabs" },
  { label: "Abayas", href: "/shop?category=abayas" },
  { label: "Namaz Scarfs", href: "/shop?category=namaz-scarfs" },
  { label: "About", href: "/about" },
];

function isNavActive(pathname: string, href: string): boolean {
  const base = href.split("?")[0];
  // Query-param links (category pages) don't show active — only exact path matches
  if (href.includes("?")) return false;
  if (base === "/") return pathname === "/";
  return pathname === base || pathname.startsWith(base + "/");
}

export function Navbar() {
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [searchOpen, setSearchOpen] = React.useState(false);
  const [scrolled, setScrolled] = React.useState(false);
  const [query, setQuery] = React.useState("");
  const pathname = usePathname();
  const router = useRouter();
  const cartCount = useCart((s) => s.items.reduce((n, i) => n + i.quantity, 0));
  const openCart = useCart((s) => s.open);

  // Close mobile menu on navigation
  const [prevPath, setPrevPath] = React.useState(pathname);
  if (prevPath !== pathname) {
    setPrevPath(pathname);
    setMobileOpen(false);
  }

  // Scroll-driven shadow/blur intensification
  React.useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  function submitSearch(e: React.FormEvent) {
    e.preventDefault();
    if (query.trim()) {
      router.push(`/shop?q=${encodeURIComponent(query.trim())}`);
      setSearchOpen(false);
      setQuery("");
    }
  }

  return (
    <header className="sticky top-0 z-40">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded-lg focus:bg-[var(--color-gold)] focus:px-4 focus:py-2 focus:text-[var(--color-text-inverse)]"
      >
        Skip to content
      </a>

      <nav
        aria-label="Main navigation"
        className={cn(
          "glass glass-elevated !rounded-none border-x-0 border-t-0 px-4 sm:px-6 lg:px-10",
          "transition-[box-shadow,background] duration-300",
          scrolled && "shadow-[0_8px_32px_rgba(0,0,0,0.45)]"
        )}
      >
        <div className="mx-auto flex h-[4.5rem] max-w-7xl items-center justify-between gap-3 sm:gap-6">
          {/* Left cluster: mobile menu + logo */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Mobile menu button — muted resting state so it never competes with the wordmark */}
            <button
              className="nav-action -ml-1.5 text-[var(--color-text-secondary)] lg:hidden"
              onClick={() => setMobileOpen((v) => !v)}
              aria-expanded={mobileOpen}
              aria-controls="mobile-nav"
              aria-label={mobileOpen ? "Close menu" : "Open menu"}
            >
              {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>

            {/* Logo */}
            <Link
              href="/"
              aria-label="Nayabi Collection — home"
              className="rounded-lg transition-opacity hover:opacity-90"
            >
              {/* Wordmark hides below `xs` (380px) so the header never overflows
                  on the smallest phones — the monogram alone stays as the brand. */}
              <Logo
                markClassName="h-8 w-8"
                wordmarkClassName="hidden xs:inline text-lg sm:text-2xl"
              />
            </Link>
          </div>

          {/* Desktop links — centered */}
          <ul className="hidden items-center gap-9 lg:flex" role="list">
            {navLinks.map((l) => {
              const active = isNavActive(pathname, l.href);
              return (
                <li key={l.href}>
                  <Link
                    href={l.href}
                    data-active={active}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "nav-underline text-sm tracking-wide transition-colors",
                      active
                        ? "text-[var(--color-gold)]"
                        : "text-[var(--color-text-secondary)] hover:text-[var(--color-gold)]"
                    )}
                  >
                    {l.label}
                  </Link>
                </li>
              );
            })}
          </ul>

          {/* Actions — utility group (search/theme) · divider · account group */}
          <div className="flex items-center gap-1.5 sm:gap-2.5">
            {/* Desktop: input-like search affordance */}
            <button
              onClick={() => setSearchOpen((v) => !v)}
              aria-expanded={searchOpen}
              aria-label="Search products"
              className="search-trigger hidden lg:inline-flex"
            >
              <Search className="h-4 w-4" aria-hidden="true" />
              <span>Search products</span>
            </button>
            {/* Mobile/tablet: compact search icon */}
            <button
              onClick={() => setSearchOpen((v) => !v)}
              aria-expanded={searchOpen}
              aria-label="Search products"
              className="nav-action lg:hidden"
            >
              <Search className="h-5 w-5" aria-hidden="true" />
            </button>

            <ThemeToggle />

            <span className="nav-divider hidden sm:block" aria-hidden="true" />

            <Link
              href="/account/wishlist"
              aria-label="Wishlist"
              className="nav-action hidden sm:inline-flex"
            >
              <Heart className="h-5 w-5" aria-hidden="true" />
            </Link>
            <Link href="/account" aria-label="My account" className="nav-action">
              <User className="h-5 w-5" aria-hidden="true" />
            </Link>
            <button
              onClick={openCart}
              aria-label={`Shopping bag, ${cartCount} item${cartCount === 1 ? "" : "s"}`}
              className="nav-action relative"
            >
              <ShoppingBag className="h-5 w-5" aria-hidden="true" />
              {cartCount > 0 && (
                <span
                  aria-hidden="true"
                  className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--color-gold)] px-1 text-[10px] font-bold text-[var(--color-text-inverse)]"
                >
                  {cartCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Search bar */}
        {searchOpen && (
          <form onSubmit={submitSearch} className="mx-auto max-w-7xl pb-4" role="search">
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
          className={cn(
            "overflow-hidden lg:hidden",
            mobileOpen ? "nav-mobile-open pb-4" : "hidden"
          )}
        >
          <ul className="flex flex-col gap-0.5" role="list">
            {navLinks.map((l) => {
              const active = isNavActive(pathname, l.href);
              return (
                <li key={l.href}>
                  <Link
                    href={l.href}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "block rounded-lg px-3 py-2.5 text-sm transition-colors",
                      active
                        ? "bg-[var(--color-glass-bg)] text-[var(--color-gold)]"
                        : "text-[var(--color-text-secondary)] hover:bg-[var(--color-glass-bg)] hover:text-[var(--color-text-primary)]"
                    )}
                  >
                    {l.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      </nav>
    </header>
  );
}
