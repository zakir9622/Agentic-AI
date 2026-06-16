# Nayabi Collection — Project Structure

A map of the codebase so anyone can find their way around quickly. The app is a
**Next.js 16 (App Router)** project written in TypeScript, with Prisma/Postgres,
Tailwind v4, and a glassmorphic design system.

```
nayabi-collection/
├── docs/                      # Project documentation (this folder)
│   ├── DEPLOYMENT.md          # Go-live, hosting, costs, services
│   ├── RISK-REPORT.md         # Technology & dependency risk assessment
│   └── PROJECT-STRUCTURE.md   # This file
├── prisma/
│   ├── schema.prisma          # Single source of truth for the data model
│   └── seed.ts                # Starter categories/products (npm run db:seed)
├── public/                    # Static assets
├── src/
│   ├── app/                   # App Router: routes, layouts, API, server actions
│   │   ├── (storefront)/      # Customer-facing pages (route group)
│   │   ├── (admin)/           # Admin dashboard (route group, gated by proxy)
│   │   ├── actions/           # Server actions ("use server")
│   │   ├── api/               # Route handlers (webhooks, checkout, cron, …)
│   │   ├── layout.tsx         # Root layout (fonts, ThemeProvider, toaster)
│   │   ├── providers.tsx      # Client providers (next-themes)
│   │   ├── globals.css        # Design tokens + glass system + themes
│   │   └── icon.svg           # Favicon (brand mark)
│   ├── components/
│   │   ├── ui/                # Reusable primitives (Glass*, Logo, ThemeToggle)
│   │   ├── storefront/        # Storefront components (navbar, footer, cards…)
│   │   ├── admin/             # Admin shell/layout
│   │   └── auth/              # Auth UI (shell, inputs, captcha)
│   ├── lib/                   # Domain logic & integrations (server-side)
│   │   ├── catalog.ts         # Product/category queries (+ demo fallbacks)
│   │   ├── orders.ts          # Order confirmation pipeline
│   │   ├── pricing.ts         # Cart/price math
│   │   ├── gift-cards.ts      # Gift card lookup/codes
│   │   ├── loyalty.ts         # Loyalty tiers + points
│   │   ├── shipping/          # Multi-carrier abstraction (see below)
│   │   ├── razorpay.ts        # Payments
│   │   ├── shiprocket.ts      # Shiprocket REST client
│   │   ├── msg91.ts           # SMS + WhatsApp notifications
│   │   ├── mail.ts            # Resend email
│   │   ├── auth.ts            # next-auth config
│   │   ├── admin-auth.ts      # Admin session helpers
│   │   ├── db.ts              # Prisma client (PrismaPg adapter)
│   │   ├── constants.ts       # Brand + business constants (client-safe)
│   │   └── utils.ts           # Pure helpers (formatPrice, cn, …)
│   ├── store/                 # Client state (zustand): cart, recently-viewed
│   ├── types/                 # Shared TS types
│   ├── generated/prisma/      # Generated Prisma client (gitignored, regen on build)
│   └── proxy.ts               # Next 16 "proxy" (was middleware) — admin gating
├── next.config.ts             # Images, security headers, poweredByHeader
├── vercel.json                # Cron jobs + function durations
└── .env.example               # All required/optional environment variables
```

## Route groups

- `(storefront)` — home, shop, product, cart/checkout, account, track, gift-cards,
  plus content pages (about, contact, faq, privacy, terms, …). Wrapped by a layout
  with the navbar, footer, cart drawer, announcement bar, cookie consent.
- `(admin)` — dashboard, orders, **Ready to Ship**, products, inventory,
  categories, customers, discounts, **gift cards**, returns, reviews, analytics,
  settings. Access is gated by `src/proxy.ts` (session cookie) + the admin layout.

## Shipping abstraction (`src/lib/shipping/`)

```
shipping/
├── types.ts        # ShippingProvider interface + shared types
├── shiprocket.ts   # Shiprocket adapter (wraps lib/shiprocket.ts)
├── delhivery.ts    # Delhivery adapter (One API)
├── amazon.ts       # Amazon Shipping adapter (scaffold, pending approval)
└── index.ts        # Registry: getProvider / listProviders / defaultProvider
```

Adding a carrier = implement `ShippingProvider` and register it in `index.ts`.
The admin "Ready to Ship" queue and the customer tracking page work unchanged.

## Conventions

- **Money** is stored and computed in **paise** (₹1 = 100). Format with
  `formatPrice()` from `lib/utils`.
- **Colors/spacing** come from CSS variables in `globals.css`
  (`var(--color-*)`) so the dark/light themes switch globally.
- **External integrations fail silently** (log + return null/no-op) so a
  third-party outage degrades a feature instead of breaking the page.
- **DB reads have demo fallbacks** in `lib/catalog.ts` (and gift-cards/loyalty),
  so the storefront renders even before the database is seeded.
- **Server vs client:** anything importing `@/lib/db` is server-only. Keep
  client-safe constants in `lib/constants.ts` (never import `db` there).
- **Quality gate:** `npm run build`, `npm run lint`, `npm run typecheck`, and
  `npm test` should all pass before committing.
