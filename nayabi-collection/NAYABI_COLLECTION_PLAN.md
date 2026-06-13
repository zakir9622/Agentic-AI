# NAYABI COLLECTION — Project Plan & Progress Tracker

> **"Nayabi" (نایاب) — Urdu for "rare"**
> Premium modest wear e-commerce platform for Muslim women across India.
> Hijabs · Abayas · Namaz Scarfs

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [What Has Been Built](#3-what-has-been-built)
4. [Design System](#4-design-system)
5. [Page Inventory](#5-page-inventory)
6. [Admin Panel](#6-admin-panel)
7. [Database & Seed Data](#7-database--seed-data)
8. [Local Setup (Windows)](#8-local-setup-windows)
9. [Deployment (Vercel + Neon)](#9-deployment-vercel--neon)
10. [Environment Variables](#10-environment-variables)
11. [Pending / TODO](#11-pending--todo)
12. [Commit History Summary](#12-commit-history-summary)

---

## 1. Project Overview

| Item | Value |
|---|---|
| **Brand** | Nayabi Collection |
| **Market** | India (INR, COD, UPI, Razorpay) |
| **Category** | Modest wear — Hijabs, Abayas, Namaz Scarfs |
| **Target users** | Muslim women across India |
| **Repo** | `zakir9622/Agentic-AI` → `/nayabi-collection` |
| **Branch (dev)** | `claude/nayabi-collection-ecommerce-ui9x3n` |
| **Branch (prod)** | `main` |
| **Local URL** | http://localhost:3000 |
| **Admin URL** | http://localhost:3000/admin/login |
| **Admin credentials** | admin@nayabicollection.com / Admin@1234! |

---

## 2. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| **Framework** | Next.js App Router | 16.2.9 |
| **Language** | TypeScript (strict) | 5.x |
| **Styling** | Tailwind CSS v4 (CSS-based config) | 4.x |
| **Database** | PostgreSQL (Neon cloud or local Docker) | 16 |
| **ORM** | Prisma 7 with `@prisma/adapter-pg` driver | 7.8.0 |
| **Auth** | NextAuth v5 (email/password + Google OAuth) | 5.0.0-beta |
| **Payments** | Razorpay (UPI, cards, wallets) + COD | — |
| **Email** | Resend (order confirmations, OTP, etc.) | 6.x |
| **Image hosting** | Cloudinary (admin uploads) | — |
| **Rate limiting** | Upstash Redis + @upstash/ratelimit | — |
| **Error tracking** | Sentry (@sentry/nextjs) | 10.x |
| **Forms** | React Hook Form + Zod validation | — |
| **State** | Zustand (cart, UI state) | 5.x |
| **Icons** | lucide-react | 1.18.0 |
| **Deployment** | Vercel | — |
| **CI/CD** | GitHub Actions + Husky pre-commit hooks | — |

### Key architectural decisions
- **Tailwind CSS v4** — config lives in `globals.css` via `@theme inline {}` (no `tailwind.config.js`)
- **Prisma 7** — uses driver adapter pattern: `new PrismaClient({ adapter: new PrismaPg({...}) })`
- **Generated client** at `src/generated/prisma/client` — gitignored, must run `prisma generate` after install
- **All prices stored in paise** — ₹999 = `99900` in DB; `formatPrice()` converts for display
- **`import "dotenv/config"`** — required first line in `prisma/seed.ts` because `tsx` doesn't auto-load `.env`

---

## 3. What Has Been Built

### Storefront (customer-facing)
- [x] Full glassmorphic design system (aurora background, glass cards, gold accent)
- [x] Auto-rotating hero carousel (3 slides, 5-second intervals, pause on hover)
- [x] Shop All / category pages with filters (color, size, fabric, sort)
- [x] Product detail page (gallery, variants, add-to-cart, reviews, related products)
- [x] Cart drawer (Zustand, persistent, real-time count badge)
- [x] Checkout — Razorpay (UPI/cards/wallets) + Cash on Delivery
- [x] Order confirmation page
- [x] Customer account portal (orders, returns, addresses, wishlist, security)
- [x] Printable order invoices
- [x] Return request flow (from My Orders)
- [x] Auth — register, login, Google OAuth, email OTP verification
- [x] Forgot/reset password flow
- [x] About, Contact (with form), FAQ (accordion), Privacy, Terms, Shipping & Returns, Size Guide
- [x] Announcement bar (DB-driven, scheduled start/end)
- [x] Cookie consent banner (GDPR-style, localStorage)
- [x] Sitemap.xml + robots.txt
- [x] Schema.org JSON-LD on product pages (Product, AggregateRating)
- [x] SEO metadata on all pages

### UI / Design System
- [x] `globals.css` — complete design token system (`@theme inline {}`)
- [x] Glass tiers: `.glass`, `.glass-elevated`, `.glass-hover`
- [x] Utilities: `gradient-text`, `nav-underline`, `social-icon`, `payment-badge`, `glass-inner`, `glass-interactive`, `ornament-divider`, `glass-reveal`, `btn-gold`, `glass-input`, `skeleton`, `stagger-enter`
- [x] Aurora mesh background (two-layer animated radial gradients)
- [x] CSS variable `--_hl` for hover highlight brightening on glass panels
- [x] Trust strip below hero (Free shipping · Returns · Payment · PAN India)
- [x] Components: `GlassButton`, `GlassCard`, `GlassBadge`, `GlassInput`, `GlassModal`, `GlassSkeleton`, `GlassToast`
- [x] `HeroCarousel` — client component, keyboard accessible, ARIA roles, progress bar
- [x] `ProductCard` — with "View Product" hover overlay
- [x] `Navbar` — sticky, scroll-shadow, active state detection, mobile menu
- [x] `Footer` — glassmorphic backdrop, social icons (Instagram/Facebook/YouTube/WhatsApp), newsletter subscribe, payment badges

### Admin Panel (`/admin`)
- [x] Login (separate cookie-based auth, `nc_admin` cookie, 8h TTL)
- [x] Dashboard with order metrics, revenue chart, recent orders
- [x] Products — list, create, edit (with image upload via Cloudinary)
- [x] Inventory management
- [x] Categories management
- [x] Orders — list, detail, status updates (processing → shipped → delivered)
- [x] Returns management
- [x] Customer list + profiles
- [x] Discount codes (percentage-based)
- [x] Reviews moderation
- [x] Announcement bar management (create, schedule, activate/deactivate)
- [x] Admin settings (multiple admin users)
- [x] GST report (analytics)
- [x] Audit log
- [x] Analytics page

### Backend / API
- [x] Cart API (`/api/cart/abandon`)
- [x] Checkout API (`/api/checkout`, `/api/checkout/verify`)
- [x] Discount code validation (`/api/discount/validate`)
- [x] Razorpay webhook (`/api/webhooks/razorpay`)
- [x] Shiprocket webhook (`/api/webhooks/shiprocket`)
- [x] Cron — abandoned cart recovery (`/api/cron/abandoned-carts`)
- [x] Cron — back-in-stock notifications (`/api/cron/back-in-stock`)

---

## 4. Design System

### Brand colours
| Token | Value | Usage |
|---|---|---|
| `--color-gold` | `#C9A96E` | Primary accent, CTAs, highlights |
| `--color-gold-light` | `#E2C48B` | Gradient start |
| `--color-gold-dark` | `#A8834A` | Gradient end |
| `--color-amethyst` | `#9B6EBB` | Aurora layer, secondary |
| `--color-bg-base` | `#07071C` | Deep indigo body background |
| `--color-bg-mid` | `#0C0C1E` | Card backgrounds |
| `--color-text-primary` | `#F4F2FF` | Main text |
| `--color-text-secondary` | `#C4BFE0` | Body text |
| `--color-text-muted` | `#8880A8` | Labels, captions |

### Glass tiers
| Class | Blur | Opacity | Use case |
|---|---|---|---|
| `.glass` | 32px | 0.08 | Product cards, panels |
| `.glass-elevated` | 48px | 0.12 | Navbar, drawers, modals |
| `.glass-hover` | — | — | Adds lift + gold glow on hover |

### Typography
- **Display / headings**: Playfair Display (serif, Google Fonts)
- **Body / UI**: Inter (sans-serif, Google Fonts)

---

## 5. Page Inventory

### Storefront pages

| Route | Type | Description |
|---|---|---|
| `/` | Dynamic | Homepage — carousel, categories, new arrivals, promise, bestsellers |
| `/shop` | Dynamic | All products with filter sidebar |
| `/shop?category=hijabs` | Dynamic | Category-filtered shop |
| `/products/[slug]` | Dynamic | Product detail page |
| `/checkout` | Static | Checkout (Razorpay + COD) |
| `/order-confirmation/[orderNumber]` | Dynamic | Post-purchase confirmation |
| `/login` | Static | Email + Google login |
| `/register` | Static | New account signup |
| `/forgot-password` | Static | Request password reset |
| `/reset-password` | Dynamic | Reset via email token |
| `/verify-email` | Dynamic | Email OTP verification |
| `/account` | Static | Account overview |
| `/account/orders` | Static | Order history |
| `/account/orders/[id]` | Dynamic | Order detail |
| `/account/orders/[id]/return` | Dynamic | Return request |
| `/account/orders/[id]/invoice` | Dynamic | Printable invoice |
| `/account/addresses` | Static | Saved addresses |
| `/account/wishlist` | Static | Saved products |
| `/account/security` | Static | Change password |
| `/about` | Static | Brand story + values |
| `/contact` | Static | Contact form |
| `/faq` | Static | FAQ accordion |
| `/shipping-returns` | Static | Policy page |
| `/size-guide` | Static | Sizing information |
| `/privacy` | Static | Privacy policy |
| `/terms` | Static | Terms of service |
| `/sitemap.xml` | Dynamic | SEO sitemap |
| `/robots.txt` | Static | Crawler config |

### Admin pages

| Route | Description |
|---|---|
| `/admin/login` | Admin authentication |
| `/admin` | Dashboard (metrics, charts, recent orders) |
| `/admin/products` | Product list |
| `/admin/products/new` | Create product |
| `/admin/products/[id]` | Edit product |
| `/admin/inventory` | Stock management |
| `/admin/categories` | Category management |
| `/admin/orders` | Order list |
| `/admin/orders/[id]` | Order detail + status update |
| `/admin/returns` | Returns list |
| `/admin/customers` | Customer list |
| `/admin/discounts` | Discount codes |
| `/admin/reviews` | Review moderation |
| `/admin/analytics` | Sales analytics + GST report |
| `/admin/audit` | Admin audit log |
| `/admin/settings` | General settings |
| `/admin/settings/announcement` | Announcement bar |
| `/admin/settings/admins` | Admin user management |

---

## 6. Admin Panel

**URL:** http://localhost:3000/admin/login

| Field | Value |
|---|---|
| Email | admin@nayabicollection.com |
| Password | Admin@1234! |

> Change this password immediately in production via `/admin/settings/admins`.

Auth uses a separate cookie (`nc_admin`) independent of NextAuth. Sessions last 8 hours.

---

## 7. Database & Seed Data

### Schema highlights
- `Product` → `ProductVariant` (color, size, fabric, stock, price per variant)
- `Category` (hijabs, abayas, namaz-scarfs, accessories)
- `Order` → `OrderItem` (with Razorpay + COD payment tracking)
- `User` → `Address`, `Wishlist`, `Review`
- `AdminUser` (separate from customer users)
- `DiscountCode` (percentage-based, usage tracking)
- `AnnouncementBar` (scheduled start/end, custom colours)
- `ReturnRequest` → `ReturnItem`
- `BackInStockRequest`
- `AbandonedCart`
- `AuditLog`

### Seeded sample data (`npm run db:seed`)
- **4 categories**: Hijabs, Abayas, Namaz Scarfs, Accessories
- **16 products** with Unsplash images, realistic INR prices, multiple variants:
  - 5 Hijabs (₹349–₹599) — georgette, chiffon, jersey, printed, luxury silk
  - 5 Abayas (₹999–₹3,499) — open, closed, butterfly, embroidered, premium
  - 3 Namaz Sets (₹599–₹1,299)
  - 3 Accessories (₹249–₹499)
- **1 admin user**: admin@nayabicollection.com / Admin@1234!
- **1 discount code**: `NAYABI10` (10% off)
- **1 announcement bar**: "Free shipping above ₹999 · Use NAYABI10"

---

## 8. Local Setup (Windows)

### Prerequisites
- Node.js **22.12+** (or 20.19+, or 24+) — check with `node -v`
- Git
- Docker Desktop (optional) OR a free Neon.tech database

### Option A — Neon cloud database (recommended, no Docker)

```powershell
# 1. Clone
git clone https://github.com/zakir9622/Agentic-AI.git
cd Agentic-AI\nayabi-collection

# 2. Install & generate
npm install
npx prisma generate

# 3. Create .env — paste your Neon connection string
# Get it from: https://neon.tech → New Project → Connection Details
# IMPORTANT: must end with ?sslmode=require
DATABASE_URL="postgresql://user:pass@ep-xxx.neon.tech/neondb?sslmode=require"
NEXTAUTH_URL="http://localhost:3000"
NEXTAUTH_SECRET="any-random-32-char-string"
ADMIN_COOKIE_SECRET="another-random-32-char-string"
NEXT_PUBLIC_APP_URL="http://localhost:3000"

# 4. Push schema and seed data
npm run db:push
npm run db:seed

# 5. Run
npm run dev
```

### Option B — Docker Desktop

```powershell
# Run the automated setup script (handles everything)
.\setup.ps1
npm run dev
```

> The `setup.ps1` script checks Node version, installs deps, runs `prisma generate`,
> creates `.env` with random secrets, starts a `nayabi-db` Docker container,
> and runs `db:push` + `db:seed` automatically.

### Useful commands

| Command | Action |
|---|---|
| `npm run dev` | Start dev server at http://localhost:3000 |
| `npm run build` | Production build |
| `npm run db:push` | Push Prisma schema to DB |
| `npm run db:seed` | Seed sample products + admin |
| `npx prisma studio` | Visual DB browser |
| `npx prisma generate` | Regenerate client (run after install) |

---

## 9. Deployment (Vercel + Neon)

### Step 1 — Neon database
1. Go to https://neon.tech → Create project → `nayabi-collection`, region: Asia Pacific (Mumbai)
2. Copy the connection string (includes `?sslmode=require`)
3. Run `npm run db:push && npm run db:seed` against Neon once

### Step 2 — Vercel deployment
1. Go to https://vercel.com → New Project → Import `zakir9622/Agentic-AI`
2. **Root directory**: `nayabi-collection`
3. **Framework**: Next.js (auto-detected)
4. Add all environment variables (see Section 10)
5. Deploy

### Step 3 — Update NEXTAUTH_URL
After first deploy, set:
```
NEXTAUTH_URL=https://your-app.vercel.app
NEXT_PUBLIC_APP_URL=https://your-app.vercel.app
```

### Hosting cost estimate

| Service | Cost |
|---|---|
| Vercel (Hobby) | Free |
| Neon (Free tier) | Free (0.5 GB, 10 hours compute/month) |
| Razorpay | 2% per transaction |
| Cloudinary (Free) | Free (25 GB storage) |
| Resend (Free) | Free (3,000 emails/month) |
| **Total to start** | **₹0 / month** |

---

## 10. Environment Variables

```env
# ── Required ─────────────────────────────────────────────────────────────
DATABASE_URL="postgresql://..."           # Neon or local PostgreSQL
NEXTAUTH_URL="http://localhost:3000"      # Your app URL
NEXTAUTH_SECRET="..."                     # 32+ random chars
ADMIN_COOKIE_SECRET="..."                 # 32+ random chars
NEXT_PUBLIC_APP_URL="http://localhost:3000"

# ── Payments (Razorpay) ───────────────────────────────────────────────────
RAZORPAY_KEY_ID=""
RAZORPAY_KEY_SECRET=""
NEXT_PUBLIC_RAZORPAY_KEY_ID=""

# ── Email (Resend) ────────────────────────────────────────────────────────
RESEND_API_KEY=""
RESEND_FROM_EMAIL="orders@nayabicollection.com"
RESEND_FROM_NAME="Nayabi Collection"

# ── Google OAuth (optional) ───────────────────────────────────────────────
GOOGLE_CLIENT_ID=""
GOOGLE_CLIENT_SECRET=""

# ── Image Uploads (Cloudinary) ────────────────────────────────────────────
CLOUDINARY_CLOUD_NAME=""
CLOUDINARY_API_KEY=""
CLOUDINARY_API_SECRET=""
```

> Generate secrets: `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`

---

## 11. Pending / TODO

### Must-do before going live
- [ ] **Razorpay keys** — sign up at razorpay.com, get live key ID + secret
- [ ] **Resend API key** — sign up at resend.com, verify domain for email sending
- [ ] **Change admin password** — update `Admin@1234!` via `/admin/settings/admins`
- [ ] **Real product photos** — replace Unsplash placeholders with actual product images (upload via admin panel with Cloudinary configured)
- [ ] **Real product data** — edit/replace seed products with your actual inventory
- [ ] **Cloudinary setup** — for admin image uploads to work
- [ ] **Custom domain** — point your domain to Vercel in domain settings

### Nice to have
- [ ] **Shiprocket integration** — automatic shipment creation + tracking
- [ ] **WhatsApp notifications** — order updates via WhatsApp Business API
- [ ] **Google Analytics** — add `NEXT_PUBLIC_GA_ID` and tracking script
- [ ] **Google OAuth** — fill in `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` for "Continue with Google" login
- [ ] **Social media accounts** — update footer social links from placeholder URLs to real handles
- [ ] **Newsletter backend** — wire the footer newsletter form to Resend or Mailchimp
- [ ] **Upstash Redis** — for rate limiting on auth endpoints (currently optional)
- [ ] **Sentry DSN** — for error tracking in production

### UI improvements considered
- [ ] Instagram feed section on homepage (embed real posts)
- [ ] "As seen on" or press mentions section
- [ ] Live chat widget (Tawk.to is free)
- [ ] Product size chart popup on PDP

---

## 12. Commit History Summary

| Commit | What was done |
|---|---|
| `f3f6149` | Initial project plan |
| `a8999b6` | M1: Scaffold — Next.js, Tailwind v4, Prisma schema, design tokens |
| `f1104dc` | M2–M4: Storefront, checkout (Razorpay+COD), full auth suite |
| `329151a` | M5: Customer account, order history, addresses, wishlist |
| `a78d5b5` | M6: Admin — dashboard, orders, returns, announcement bar, settings |
| `c28ecd8` | M7: Admin — products, inventory, categories, customers, discounts |
| `5487a46` | M8: Admin — analytics, GST report, audit log |
| `74df960` | M9–M11: Back-in-stock, SEO, security, Vercel config |
| `4e398b0` | Advanced glassmorphic UI overhaul + realistic seed data |
| `6a0d125` | Windows PowerShell setup script (`setup.ps1`) |
| `64ca672` | Fix setup.ps1 — ASCII-only (Windows encoding fix) |
| `24a133d` | Add Node.js version check to setup.ps1 |
| `59b1e6e` | Add `prisma generate` step to setup scripts |
| `61e60a1` | Fix: hydration mismatch in CookieConsent + homepage redesign |
| `927b9e0` | feat: production-quality CSS — gradient-text, nav-underline, social-icon, glassmorphic footer, "View Product" card overlay |
| `599defc` | feat: vivid aurora background, auto-rotating hero carousel, trust strip, no-DB fallback |
