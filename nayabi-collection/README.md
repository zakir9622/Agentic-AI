# Nayabi Collection

> **Nayabi** *(Urdu: نایاب — rare, precious)* · Full-stack ecommerce platform for modest wear — hijabs, abayas, and namaz scarfs — built for the India market.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Next.js 16.2.9 (App Router, RSC) |
| Language | TypeScript (strict mode) |
| Styling | Tailwind CSS v4 · Glassmorphic design system |
| Database | PostgreSQL · Prisma 7.8.0 (adapter-based) |
| Auth | NextAuth.js v5 (JWT + Google OAuth) · Cookie-based admin auth |
| Payments | Razorpay (card / UPI / netbanking / EMI) · Cash on Delivery |
| SMS & WhatsApp | MSG91 |
| Email | Resend |
| Image Storage | Cloudinary |
| Rate Limiting | Upstash Redis (graceful no-op when not configured) |
| Deployment | Vercel (cron jobs + function timeouts in `vercel.json`) |

---

## Features

### Storefront
- **Catalog** — category + multi-facet search (color, fabric, size, price range), sort by price / newest / popularity
- **Product detail** — variant picker (color · size · fabric), stock badge, size guide modal, back-in-stock alert subscription, customer reviews with verified-purchase badge
- **Cart** — Zustand-powered persistent cart, item quantity controls, coupon code input with real-time validation
- **Checkout** — Indian address form (29-state dropdown, pincode), Razorpay (full payment methods) + COD toggle, GST 5% auto-applied, order summary with discount line
- **Order tracking** — 5-step visual timeline (Placed → Confirmed → Processing → Shipped → Delivered), live shipment tracking link

### Customer Account
- Registration / Login (email+password · Google OAuth)
- Password reset via email OTP
- Order history with status badges and item previews
- Order detail — full tracking timeline, shipment carrier + tracking number, return CTA
- **Wishlist** — toggle from product detail page
- **Saved addresses** — add / edit / set default / delete
- **Returns & exchanges** — 7-day window, item selector with qty steppers, refund method (bank transfer / store credit)
- Security — change password

### Admin Dashboard (`/admin`)
- **Separate auth** — bcrypt cookie-based session (8h TTL), 5-attempt lockout
- **KPI dashboard** — revenue MoM growth, orders, customers, pending returns, low-stock count
- **Orders** — list, detail, status transitions, ship form (carrier / tracking number / URL)
- **Returns** — queue with status filter, approve/reject with admin note
- **Products** — create/edit with slug, description, tags, image URLs; variant builder (color, size, fabric, SKU, stock, price override)
- **Categories** — create/rename with auto-slug
- **Inventory** — inline stock editor across all variants with low-stock highlighting
- **Customers** — paginated list with total spend, order count, join date
- **Discounts** — percent / fixed-amount / free-shipping codes with usage limits and expiry
- **Analytics** — 6-month revenue bar chart, YTD KPIs, top 10 products by revenue, GST summary (taxable value + GST collected)
- **Audit log** — paginated record of all admin actions with actor, action, timestamp
- **Settings** — announcement bar, shipping zones, tax/GST settings

### Notifications
| Trigger | Channel |
|---|---|
| Order placed | SMS + WhatsApp (MSG91) |
| Order shipped | SMS + WhatsApp (MSG91) |
| Order delivered | SMS + WhatsApp (MSG91) |
| Password reset OTP | Email (Resend) |
| Abandoned cart (30 min) | SMS (cron, every hour) |
| Back-in-stock alert | SMS + WhatsApp (cron, every 30 min) |

### India-Specific
- All prices stored in **paise** (₹999 = 99900), displayed via `formatPrice()`
- **GST 5%** calculated at checkout and surfaced in admin GST report
- Indian address format — 29-state dropdown, 6-digit pincode, +91 phone
- **COD** with configurable extra charge (default ₹50)
- Free shipping above configurable threshold (default ₹999)
- Shiprocket-compatible shipment data model

---

## Design System

Dark glassmorphic theme driven by CSS custom properties (Tailwind v4 `@theme inline`):

```css
--color-bg-base:        #0A0A14   /* deep navy-black */
--color-gold:           #C9A96E   /* primary accent */
--color-amethyst:       #8B5E9B   /* secondary accent */
--color-glass-bg:       rgba(255,255,255,0.04)
--color-glass-border:   rgba(255,255,255,0.10)
--color-text-primary:   #F0EBE3
--color-text-secondary: #A89D8F
--color-text-muted:     #6B6057
```

**Reusable primitives** in `src/components/ui/`:

| Component | Props |
|---|---|
| `GlassCard` | `padding` (sm/md/lg), `hoverable` |
| `GlassButton` | `variant` (primary/secondary/ghost), `size` (sm/md/lg) |
| `GlassBadge` | `variant` (success/error/warning/info/gold/neutral) |
| `GlassInput` | standard input with glass styling |
| `GlassSelect` | dropdown with glass styling |
| `GlassTextarea` | textarea with glass styling |

---

## Project Structure

```
nayabi-collection/
├── prisma/
│   ├── schema.prisma          # 20+ model DB schema
│   └── seed.ts                # Admin user + 6 sample products
├── src/
│   ├── app/
│   │   ├── (storefront)/      # Customer-facing routes
│   │   │   ├── page.tsx           # Home
│   │   │   ├── products/          # Catalog ([slug]/page.tsx = PDP)
│   │   │   ├── cart/
│   │   │   ├── checkout/
│   │   │   ├── account/           # Orders, wishlist, addresses, returns
│   │   │   ├── about/ contact/ faq/ size-guide/ privacy/ terms/
│   │   │   └── shipping-returns/
│   │   ├── (admin)/admin/     # Admin dashboard
│   │   │   ├── page.tsx           # KPI dashboard
│   │   │   ├── orders/ returns/ products/ categories/
│   │   │   ├── inventory/ customers/ discounts/
│   │   │   ├── analytics/ audit/ settings/
│   │   │   └── login/
│   │   ├── api/
│   │   │   ├── auth/              # NextAuth.js endpoints
│   │   │   ├── checkout/          # Razorpay order creation
│   │   │   ├── discount/validate/ # Coupon code validation
│   │   │   ├── back-in-stock/     # Subscribe to alert
│   │   │   ├── webhook/razorpay/  # Payment webhook (HMAC verified)
│   │   │   ├── webhook/shiprocket/
│   │   │   ├── admin/             # Admin REST API
│   │   │   └── cron/              # Abandoned cart + back-in-stock
│   │   └── actions/           # All "use server" server actions
│   ├── components/
│   │   ├── ui/                # Glass design primitives + CookieConsent
│   │   ├── admin/             # AdminLayout sidebar + nav
│   │   └── storefront/        # Header, footer, ProductCard, cart drawer…
│   ├── lib/
│   │   ├── auth.ts            # NextAuth v5 config
│   │   ├── admin-auth.ts      # Cookie-based admin session
│   │   ├── db.ts              # Prisma singleton
│   │   ├── constants.ts       # RETURN_WINDOW_DAYS, INDIAN_STATES, …
│   │   ├── utils.ts           # formatPrice, cn, slugify
│   │   ├── ratelimit.ts       # Upstash rate limiting (graceful no-op)
│   │   ├── notifications.ts   # MSG91 SMS/WhatsApp helpers
│   │   └── mail.ts            # Resend email helpers
│   └── generated/prisma/      # Prisma generated client output
├── vercel.json                # Cron + function max-duration config
└── .env.example               # Full annotated environment variable template
```

---

## Getting Started

### Prerequisites
- Node.js 20+
- PostgreSQL 14+ (or [Neon](https://neon.tech) / [Supabase](https://supabase.com) free tier)
- Accounts: Razorpay (test keys), MSG91, Resend, Cloudinary

### 1 — Clone & install

```bash
git clone https://github.com/zakir9622/Agentic-AI.git
cd Agentic-AI/nayabi-collection
npm install
```

### 2 — Configure environment

```bash
cp .env.example .env
# Fill in your credentials — see Environment Variables below
```

### 3 — Database setup

```bash
npx prisma db push      # Apply schema to your PostgreSQL DB
npx prisma db seed      # Seed admin user + sample products
```

Seed creates:
- Admin: `admin@nayabicollection.com` / `Admin@1234!` — **change this immediately**
- 3 categories (Hijabs, Abayas, Namaz Scarfs)
- 6 products with multiple variants
- Sample discount codes

### 4 — Run locally

```bash
npm run dev
```

| URL | What you see |
|---|---|
| `http://localhost:3000` | Storefront — home, catalog, checkout |
| `http://localhost:3000/admin` | Admin dashboard (login first) |
| `http://localhost:3000/admin/login` | Admin login |

---

## Environment Variables

Copy `.env.example` and fill in the values:

| Variable | Required | Description |
|---|---|---|
| `DATABASE_URL` | ✅ | PostgreSQL connection string |
| `NEXTAUTH_SECRET` | ✅ | 32+ char random (`openssl rand -base64 32`) |
| `NEXTAUTH_URL` | ✅ | App base URL (`http://localhost:3000` locally) |
| `NEXT_PUBLIC_APP_URL` | ✅ | Same as above (public env) |
| `GOOGLE_CLIENT_ID` | ✅ | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | ✅ | Google OAuth client secret |
| `RAZORPAY_KEY_ID` | ✅ | Razorpay key ID (`rzp_test_…` for test) |
| `RAZORPAY_KEY_SECRET` | ✅ | Razorpay key secret |
| `RAZORPAY_WEBHOOK_SECRET` | ✅ | From Razorpay dashboard → Webhooks |
| `NEXT_PUBLIC_RAZORPAY_KEY_ID` | ✅ | Same as `RAZORPAY_KEY_ID` (client-side) |
| `CLOUDINARY_CLOUD_NAME` | ✅ | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | ✅ | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | ✅ | Cloudinary API secret |
| `RESEND_API_KEY` | ✅ | Resend API key (`re_…`) |
| `RESEND_FROM_EMAIL` | ✅ | Verified sender address |
| `RESEND_FROM_NAME` | ⚠️ | Sender display name (default: Nayabi Collection) |
| `MSG91_AUTH_KEY` | ✅ | MSG91 auth key |
| `MSG91_SENDER_ID` | ✅ | 6-char DLT-registered sender ID |
| `MSG91_WHATSAPP_NUMBER` | ✅ | WhatsApp Business number |
| `MSG91_SMS_TEMPLATE_ORDER_PLACED` | ✅ | DLT-approved template ID |
| `MSG91_SMS_TEMPLATE_SHIPPED` | ✅ | DLT-approved template ID |
| `MSG91_SMS_TEMPLATE_OTP` | ✅ | DLT-approved OTP template ID |
| `MSG91_SMS_TEMPLATE_BACK_IN_STOCK` | ✅ | DLT-approved template ID |
| `CRON_SECRET` | ✅ | Shared secret for cron endpoint auth |
| `COD_EXTRA_CHARGE` | ⚠️ | COD fee in paise (default: `5000` = ₹50) |
| `FREE_SHIPPING_THRESHOLD` | ⚠️ | Free shipping threshold in paise (default: `99900` = ₹999) |
| `RETURN_WINDOW_DAYS` | ⚠️ | Return window in days (default: `7`) |
| `LOW_STOCK_THRESHOLD` | ⚠️ | Low stock alert count (default: `5`) |
| `ABANDONED_CART_DELAY_MINUTES` | ⚠️ | Minutes before cart is "abandoned" (default: `30`) |
| `SHIPROCKET_EMAIL` | ⚠️ | Shiprocket account email |
| `SHIPROCKET_PASSWORD` | ⚠️ | Shiprocket account password |
| `UPSTASH_REDIS_REST_URL` | ⚠️ | Upstash Redis URL (rate limiting) |
| `UPSTASH_REDIS_REST_TOKEN` | ⚠️ | Upstash Redis token |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | ⚠️ | Cloudflare Turnstile site key |
| `TURNSTILE_SECRET_KEY` | ⚠️ | Cloudflare Turnstile secret |
| `SENTRY_DSN` | ⚠️ | Sentry DSN for error monitoring |

✅ = required for core functionality · ⚠️ = optional / feature-specific

---

## Deployment (Vercel)

1. Push to GitHub, import in [Vercel](https://vercel.com)
2. Add all environment variables in the Vercel dashboard
3. Set `NEXTAUTH_URL` and `NEXT_PUBLIC_APP_URL` to your production domain
4. Cron jobs are auto-configured via `vercel.json`:
   - `GET /api/cron/abandoned-carts` — every hour
   - `GET /api/cron/back-in-stock` — every 30 minutes
5. Register webhooks in Razorpay dashboard:
   - `https://yourdomain.com/api/webhook/razorpay`
6. Register webhook in Shiprocket (if used):
   - `https://yourdomain.com/api/webhook/shiprocket`

---

## Admin Setup

The admin section uses a **separate auth system** — a signed, base64-encoded httpOnly cookie (`nc_admin`) with 8-hour TTL, independent of NextAuth.

Admin accounts live in the `AdminUser` table. To create the first admin, run the seed:

```bash
npx prisma db seed
# Creates: admin@nayabicollection.com / Admin@1234!
```

Or add directly via Prisma Studio:

```bash
npx prisma studio
# Add a row to AdminUser with a bcrypt-hashed password (cost factor 12)
```

**Change the default seed password immediately after first login.**

---

## Pending Items

| # | Item | Priority | Notes |
|---|---|---|---|
| 1 | **Shiprocket API calls** | High | DB model is ready; `createShipment`, fetch tracking not yet wired to Shiprocket REST API |
| 2 | **Product image upload** | Medium | Admin form accepts URLs only; Cloudinary upload widget not integrated |
| 3 | **Sentry error monitoring** | Medium | `instrumentation.ts` stub commented out; install `@sentry/nextjs` and uncomment |
| 4 | **Cloudflare Turnstile CAPTCHA** | Medium | Env vars documented; verification middleware not yet added to auth routes |
| 5 | **Customer review submission** | Low | Reviews are displayed on PDP (with verified-purchase badge); customers cannot submit reviews from the storefront yet |
| 6 | **Multi-image gallery on PDP** | Low | Single image per variant; carousel for multiple images not implemented |
| 7 | **Next/Image for product images** | Low | Currently `<img>` tags; swap to `next/image` for automatic LCP + CDN optimization |
| 8 | **Printable / PDF invoice** | Low | Order detail page doesn't have a downloadable invoice |
| 9 | **Admin user management UI** | Low | Admin accounts created via seed/Prisma Studio only; no in-dashboard UI |

---

## Key Architectural Notes

| Decision | Why |
|---|---|
| All prices in **paise** (integers) | Avoids floating-point errors; matches Razorpay's native unit |
| `"use server"` in dedicated files only | Next.js 16 prohibits inline `"use server"` inside client component files |
| Admin auth separate from NextAuth | Admins bypass OAuth; short-lived cookie + lockout is simpler and more secure for a closed admin |
| Upstash as graceful no-op | Rate limiting is optional — `ratelimitCheck()` returns `true` (allow) when Redis isn't configured |
| `export const dynamic = "force-dynamic"` on `sitemap.ts` | Prevents Prisma connecting at Vercel build time |

---

## Scripts

```bash
npm run dev          # Start dev server (http://localhost:3000)
npm run build        # Production build
npm run lint         # ESLint
npm run typecheck    # TypeScript type-check (no emit)
npm run format       # Prettier write
npx prisma studio    # Open Prisma DB browser
npx prisma db seed   # Seed admin + sample data
```

---

## License

Private — all rights reserved. © Nayabi Collection.
