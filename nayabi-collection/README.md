# Nayabi Collection

> **Nayabi** *(Urdu: نایاب — rare, precious)* · Full-stack ecommerce platform for modest wear — hijabs, abayas, and namaz scarfs — built for the India market.

Live demo branch: `claude/nayabi-collection-ecommerce-ui9x3n`

> **▶ Want to run it on your machine right now?** See **[LOCAL_SETUP.md](./LOCAL_SETUP.md)** — one command (`bash setup.sh`) gets the whole site running locally with sample data.

---

## What You Need From Your Side

Before you can launch, you need accounts/credentials for these services. Items marked **Required** block core functionality; **Optional** items degrade gracefully if missing.

| Service | Required? | What to Do | Cost |
|---|---|---|---|
| **PostgreSQL database** | Required | Create a free DB on [Neon](https://neon.tech) or [Supabase](https://supabase.com) | Free tier available |
| **Razorpay** | Required | [Sign up](https://razorpay.com) → Dashboard → API Keys | Free to sign up; 2% per transaction |
| **NextAuth secret** | Required | Run `openssl rand -hex 32` in terminal | Free |
| **Resend** (email) | Required | [Sign up](https://resend.com) → API Keys | Free up to 100 emails/day |
| **Google OAuth** | Recommended | [Google Cloud Console](https://console.cloud.google.com) → Credentials → OAuth 2.0 | Free |
| **Cloudinary** (images) | Recommended | [Sign up](https://cloudinary.com) → Dashboard → Copy Cloud Name, Key, Secret | Free up to 25 GB |
| **MSG91** (SMS/WhatsApp) | Optional | [Sign up](https://msg91.com) → Sender IDs → Templates | Pay-per-message |
| **Shiprocket** (shipping) | Optional | [Sign up](https://shiprocket.in) → API Credentials | Pay-per-shipment |
| **Upstash Redis** (rate limit) | Optional | [Sign up](https://upstash.com) → Create database | Free tier: 10k req/day |
| **Cloudflare Turnstile** (CAPTCHA) | Optional | [Cloudflare Dashboard](https://dash.cloudflare.com) → Turnstile → Add site | Free |
| **Sentry** (error monitoring) | Optional | [Sign up](https://sentry.io) → New project → Next.js | Free up to 5k errors/month |
| **Vercel** (hosting) | Recommended | [Sign up](https://vercel.com) → Import repository | Free hobby tier |

---

## Hosting & Running Costs

### What You'll Actually Pay (India market, realistic estimates)

| Service | Free Tier | Paid Plan | When You Need Paid |
|---|---|---|---|
| **Vercel** (hosting) | 100 GB bandwidth, 100k function invocations | Pro ₹1,700/mo | >100 concurrent users, custom domain needed |
| **Neon PostgreSQL** | 0.5 GB storage, 1 project | Scale ₹640/mo | >1000 daily orders |
| **Razorpay** | No monthly fee | **2% per transaction** | Always pay-per-use |
| **Cloudinary** (images) | 25 GB storage, 25 GB bandwidth | Plus $89/mo | >500 product images |
| **Resend** (email) | 100 emails/day | $20/mo for 50k | >100 orders/day |
| **MSG91** (SMS) | — | ~₹0.20–₹0.35 per SMS | Per SMS sent |
| **Shiprocket** | No fixed fee | 2–5% of shipping charge | Per shipment |
| **Upstash Redis** | 10k req/day | $0.20 per 100k beyond | High traffic only |

**Bootstrap cost to launch: ₹0/month** — all free tiers cover a starting store.
**At 100 orders/day:** ~₹5,000–₹8,000/month (mostly Razorpay fees + Vercel Pro).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Next.js 16.2.9 (App Router, RSC, Server Actions) |
| Language | TypeScript (strict mode) |
| Styling | Tailwind CSS v4 · Advanced glassmorphic design system |
| Database | PostgreSQL · Prisma 7.8.0 (driver adapter) |
| Auth | NextAuth.js v5 (JWT + Google OAuth) · Cookie-based admin auth |
| Payments | Razorpay (card / UPI / netbanking / EMI / COD) |
| SMS & WhatsApp | MSG91 |
| Email | Resend |
| Image Storage | Cloudinary (server-side signed upload) |
| Rate Limiting | Upstash Redis (graceful no-op when not configured) |
| CAPTCHA | Cloudflare Turnstile (graceful no-op when not configured) |
| Error Tracking | Sentry |
| Shipping | Shiprocket |
| Deployment | Vercel |

---

## Features

### Storefront
- **Home page** — aurora glassmorphic hero, floating product preview cards, category grid, new arrivals, bestsellers, promise banner
- **Catalog** — category + multi-facet search (color, fabric, size, price range), sort by price / newest / popularity
- **Product detail** — variant picker (color · size · fabric), stock badge, size guide modal, back-in-stock alert, customer reviews with verified-purchase badge
- **Cart** — Zustand-powered persistent cart, quantity controls, coupon code validation
- **Checkout** — Indian address form (29-state dropdown, pincode), Razorpay full payment + COD, GST 5%, order summary with discount
- **Order tracking** — 5-step visual timeline, live tracking link, invoice PDF

### Customer Account
- Email+password + Google OAuth
- Password reset via email OTP
- Order history, order detail with tracking timeline
- Wishlist, saved addresses, returns & exchanges
- Security (change password)

### Admin Dashboard (`/admin`)
- Separate bcrypt cookie auth (8h TTL, 5-attempt lockout)
- KPI dashboard — revenue, orders, customers, returns, low stock
- Orders — list, detail, status transitions, ship form
- Returns — queue, approve/reject
- Products — full CRUD with image upload (Cloudinary), variants
- Categories, reviews moderation, user management

---

## Project Structure

```
nayabi-collection/
├── prisma/
│   ├── schema.prisma          # Full data model
│   └── seed.ts                # 16 sample products + admin user
├── src/
│   ├── app/
│   │   ├── (admin)/admin/     # Admin pages
│   │   ├── (storefront)/      # Customer-facing pages
│   │   ├── actions/           # Server actions (auth, orders, admin)
│   │   ├── api/               # API routes (webhooks, upload, reviews)
│   │   └── globals.css        # Design tokens + glassmorphic system
│   ├── components/
│   │   ├── admin/             # Admin layout + components
│   │   ├── auth/              # Login/register forms
│   │   ├── storefront/        # Navbar, footer, product card, cart
│   │   └── ui/                # Glass design system (GlassCard, GlassButton, GlassInput)
│   └── lib/
│       ├── catalog.ts         # Typed product queries
│       ├── cart.ts            # Zustand cart store
│       ├── shiprocket.ts      # Shiprocket API (token-cached)
│       ├── turnstile.ts       # Cloudflare CAPTCHA
│       └── utils.ts           # formatPrice, cn helpers
```

---

## Getting Started

### 1. Clone and install

```bash
git clone https://github.com/zakir9622/Agentic-AI
cd Agentic-AI/nayabi-collection
npm install
```

### 2. Create your database

Sign up at [neon.tech](https://neon.tech) (free), create a project, copy the **Connection String** (starts with `postgresql://`).

### 3. Set environment variables

Create `.env.local` in the `nayabi-collection/` folder:

```env
# ── DATABASE (Required) ─────────────────────────────────────────────────────
DATABASE_URL="postgresql://user:password@host/dbname?sslmode=require"

# ── NEXTAUTH (Required) ────────────────────────────────────────────────────
NEXTAUTH_URL="http://localhost:3000"
NEXTAUTH_SECRET="run: openssl rand -hex 32"

# ── ADMIN (Required) ──────────────────────────────────────────────────────
ADMIN_COOKIE_SECRET="run: openssl rand -hex 32"

# ── RAZORPAY (Required for payments) ─────────────────────────────────────
RAZORPAY_KEY_ID="rzp_test_..."
RAZORPAY_KEY_SECRET="..."
NEXT_PUBLIC_RAZORPAY_KEY_ID="rzp_test_..."

# ── EMAIL (Required) ─────────────────────────────────────────────────────
RESEND_API_KEY="re_..."
EMAIL_FROM="Nayabi Collection <orders@yourdomain.com>"

# ── GOOGLE OAUTH (Recommended) ───────────────────────────────────────────
GOOGLE_CLIENT_ID="....apps.googleusercontent.com"
GOOGLE_CLIENT_SECRET="GOCSPX-..."

# ── CLOUDINARY (Recommended for image upload) ────────────────────────────
CLOUDINARY_CLOUD_NAME="your-cloud-name"
CLOUDINARY_API_KEY="your-api-key"
CLOUDINARY_API_SECRET="your-api-secret"

# ── OPTIONAL ─────────────────────────────────────────────────────────────
MSG91_API_KEY=""
MSG91_SENDER_ID=""
MSG91_OTP_TEMPLATE_ID=""
SHIPROCKET_EMAIL=""
SHIPROCKET_PASSWORD=""
NEXT_PUBLIC_TURNSTILE_SITE_KEY=""
TURNSTILE_SECRET_KEY=""
UPSTASH_REDIS_REST_URL=""
UPSTASH_REDIS_REST_TOKEN=""
SENTRY_DSN=""
SHIPROCKET_WEBHOOK_TOKEN=""
```

### 4. Set up the database and seed

```bash
# Push schema to your database
npx prisma db push

# Seed sample products and admin user
npx prisma db seed
```

### 5. Run locally

```bash
npm run dev
# Open http://localhost:3000
```

### 6. Access admin panel

Go to `http://localhost:3000/admin/login`

Default credentials from seed (change immediately):
- Email: `admin@nayabicollection.com`
- Password: `Admin@1234!`

---

## Deploying to Vercel

1. Push your branch to GitHub
2. Go to [vercel.com](https://vercel.com) → **New Project** → Import your repo
3. Set **Root Directory** to `nayabi-collection`
4. Add all environment variables from `.env.local` in the Vercel dashboard
5. Add one more: `NEXTAUTH_URL="https://yourdomain.vercel.app"`
6. Deploy

After deploy, run the database seed once:
```bash
# From local machine, pointing at production DB
DATABASE_URL="your-production-db-url" npx prisma db seed
```

### Custom Domain
In Vercel → Settings → Domains → add your domain.
Update `NEXTAUTH_URL` to your custom domain.
In Google Cloud Console, add the domain to OAuth Authorized redirect URIs.

---

## Design System

The UI uses an advanced glassmorphic design system:

- **Aurora background** — two-layer animated gradient mesh (18s + 11s cycle)
- **Glass tiers** — `.glass` (content cards) / `.glass-elevated` (modals, nav, drawers)
- **Top-edge highlight** — `::before` pseudo-element gives each glass panel a premium light refraction
- **Noise texture** — subtle SVG fractal noise on glass surfaces for depth
- **Gold accent** — `#C9A96E` with glow shadows on hover
- **Amethyst accent** — `#9B6EBB` for secondary highlights
- **Stagger animations** — `fadeInUp` on grid items with 60ms delay increments

All design tokens are in `src/app/globals.css` under `@theme inline {}`.

---

## Scripts

```bash
npm run dev          # Start dev server
npm run build        # Production build (runs `prisma generate` first)
npm run lint         # ESLint
npm run typecheck    # Type check (tsc --noEmit)
npm test             # Unit + integration tests (node:test via tsx)
npm run db:push      # Sync schema to DB
npm run db:seed      # Seed sample data
npm run db:studio    # Visual DB browser (opens at localhost:5555)
```

## Documentation

Detailed guides live in [`docs/`](./docs):

- [`docs/DEPLOYMENT.md`](./docs/DEPLOYMENT.md) — go-live steps, hosting choice, required services, and indicative monthly costs.
- [`docs/RISK-REPORT.md`](./docs/RISK-REPORT.md) — technology & dependency risk assessment.
- [`docs/PROJECT-STRUCTURE.md`](./docs/PROJECT-STRUCTURE.md) — full codebase map and conventions.

---

## Architectural Notes

- **All prices in paise** — ₹999 is stored as `99900`. Use `formatPrice()` from `lib/utils.ts` for display.
- **Server Actions** — all mutations use Next.js Server Actions in `src/app/actions/`. Never call them directly in "use client" files without `useActionState`.
- **Prisma client** — uses driver adapter pattern: `new PrismaClient({ adapter: new PrismaPg({...}) })`. The generated client is at `src/generated/prisma`.
- **Admin session** — cookie `nc_admin` with `AdminSession` interface (uses `adminId`, not `id`).
- **Graceful degradation** — MSG91, Shiprocket, Upstash, Turnstile, and Sentry all fail silently if env vars are missing. The store works without them.
- **Image upload** — server-side Cloudinary REST API with SHA-1 signature (no SDK needed).
