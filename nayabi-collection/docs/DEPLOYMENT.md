# Nayabi Collection — Go-Live & Hosting Guide

This document explains how to take the store from this repository to a live,
production website, which third-party services you must purchase, and what it
will roughly cost per month (₹, India). All prices are indicative (Jun 2026)
and should be re-checked on each provider's site.

---

## 1. Recommended hosting

**Vercel** is the recommended host — this is a Next.js 16 app and Vercel is built
by the Next.js team, so SSR, image optimization, cron jobs, and edge caching all
work with zero config. The repo already contains `vercel.json`.

| Option | Best for | Cost |
| --- | --- | --- |
| **Vercel Hobby** | Testing / very low traffic (personal, non-commercial) | Free |
| **Vercel Pro** ✅ recommended for a real store | Commercial use, custom domain, analytics, more bandwidth | ~$20/mo (~₹1,700/mo) |
| Self-host (VPS — Hetzner/DigitalOcean + Docker) | Full control, lower cost at scale | ₹400–₹1,200/mo + your time |
| Netlify / Cloudflare Pages | Alternatives, similar to Vercel | Free–$19/mo |

> A commercial store should be on **Vercel Pro** (the Hobby plan's license
> excludes commercial use). Self-hosting on a VPS is cheaper but you become
> responsible for scaling, TLS, and uptime.

---

## 2. Services you must purchase / sign up for

The app integrates these services. Items marked **Required** are needed for a
functioning store; **Optional** can be added later.

| Service | Purpose | Required? | Typical cost (₹/mo) |
| --- | --- | --- | --- |
| **Domain name** (e.g. nayabicollection.in) | Your web address | Required | ₹80–₹1,000/yr (.in cheaper than .com) |
| **Hosting** (Vercel Pro) | Runs the site | Required | ~₹1,700 |
| **PostgreSQL DB** (Neon / Supabase / Railway) | All store data | Required | Free tier → ₹0; paid ₹500–₹2,000 |
| **Razorpay** | Online payments (UPI/cards/netbanking) | Required for prepaid | No monthly fee; **~2% per transaction** + GST |
| **Resend** | Transactional email (order/shipping/OTP) | Required | Free 3k emails/mo; paid from ~₹1,700 |
| **MSG91** | SMS + WhatsApp order updates | Required (for SMS/WhatsApp) | Pay-as-you-go: SMS ~₹0.18–0.25 each; WhatsApp ~₹0.35–0.80/conversation |
| **Cloudinary** | Product image hosting/CDN/optimization | Required (or use any image CDN) | Free tier generous; paid from ~₹3,500 |
| **Shiprocket** | Shipping / courier aggregation + tracking | Required for fulfillment | Free plan available; pay per shipment (₹20–₹60+/shipment by weight/zone) |
| **Delhivery** | Direct courier (optional, in addition to Shiprocket) | Optional | Per-shipment, B2B contract |
| **Amazon Shipping (India)** | Direct courier (optional) | Optional | Per-shipment; requires approval |
| **Upstash Redis** | Rate limiting + abandoned-cart timing | Recommended | Free tier → ₹0; paid usage-based |
| **Cloudflare Turnstile** | Bot/CAPTCHA protection on auth | Recommended | Free |
| **Sentry** | Error monitoring | Optional | Free tier → ₹0; paid from ~₹2,200 |
| **Google OAuth** | "Sign in with Google" | Optional | Free |

### Indicative monthly total

| Scenario | Approx. monthly cost (excl. payment %) |
| --- | --- |
| **Lean launch** (Vercel Pro + free DB/Redis tiers + Resend free + Cloudinary free + pay-per-use SMS/shipping) | **₹1,700–₹3,000/mo** + ~2% on payments + per-shipment courier + per-SMS |
| **Comfortable** (paid DB, paid email, Sentry) | **₹5,000–₹9,000/mo** + variable |

> The two biggest **variable** costs are **payment fees (~2% of revenue, Razorpay)**
> and **shipping (per parcel)**. Budget these as a % of sales, not a flat fee.
> Domain is billed yearly, everything else monthly/usage-based.

---

## 3. One-time setup before launch

1. **Buy a domain** (GoDaddy / Namecheap / Cloudflare Registrar). `.in` domains
   are cheapest for an India-focused brand.
2. **Create a PostgreSQL database** (Neon is the easiest free option). Copy its
   connection string.
3. **Create accounts** for Razorpay, Resend, MSG91, Cloudinary, Shiprocket,
   Upstash, Cloudflare Turnstile (see table). Collect the API keys.
4. **Razorpay KYC**: complete business verification (PAN, GST, bank account) —
   this can take 1–3 business days, do it early.
5. **MSG91 WhatsApp**: register a WhatsApp Business sender and get DLT-approved
   SMS templates (Indian SMS requires DLT registration — also start early).
6. **Verify your email domain** in Resend (add the DNS records they give you).

---

## 4. Configure environment variables

Copy `.env.example` to your host's environment settings and fill every value.
See the file for the full list. The critical ones:

- `DATABASE_URL` — your Postgres connection string
- `NEXTAUTH_SECRET` — generate with `openssl rand -base64 32`
- `NEXTAUTH_URL` / `NEXT_PUBLIC_APP_URL` — your live domain (https://…)
- Razorpay, Resend, MSG91, Cloudinary, Shiprocket keys
- `CRON_SECRET` — a strong random string (protects the cron endpoints)

> Never commit real secrets. On Vercel, add them under
> **Project → Settings → Environment Variables** (Production scope).

---

## 5. Deploy

### Option A — Vercel (recommended)
1. Push this repo to GitHub (already done).
2. On vercel.com → **New Project** → import the repo.
3. Set the **Root Directory** to `nayabi-collection`.
4. Add all environment variables (Production).
5. Deploy. Vercel runs `next build` automatically.
6. **Initialize the database** once: run `npm run db:push` (or
   `prisma migrate deploy`) against the production `DATABASE_URL`, then
   `npm run db:seed` to load starter categories/products.
7. Add your custom domain under **Settings → Domains** and point DNS as shown.

### Option B — Self-host (VPS + Docker)
1. Provision an Ubuntu VPS, install Node 20+.
2. `npm ci && npm run build && npm run start` behind Nginx with a TLS cert
   (Let's Encrypt). Use `pm2` or a systemd unit to keep it running.
3. Run the DB migration + seed as above.
4. Configure the cron endpoints (`/api/cron/*`) via a system cron + `CRON_SECRET`.

---

## 6. Post-launch checklist

- [ ] Razorpay switched from **test** keys to **live** keys
- [ ] Razorpay webhook URL set to `https://yourdomain/api/webhooks/razorpay`
- [ ] Shiprocket webhook set to `https://yourdomain/api/webhooks/shiprocket`
- [ ] Change the default admin password (`Admin@1234!`)
- [ ] DLT-approved SMS templates + WhatsApp templates live in MSG91
- [ ] Resend sending domain verified
- [ ] Test a full order end-to-end (prepaid + COD)
- [ ] `robots.txt` / `sitemap.xml` reachable; submit to Google Search Console
- [ ] Set up daily DB backups (most managed Postgres providers include this)

---

## 7. Ongoing costs summary

- **Fixed monthly:** hosting (₹1,700 Vercel Pro), optionally paid DB/email/Sentry.
- **Per year:** domain renewal.
- **Variable (scales with sales):** Razorpay ~2% + GST per payment, courier per
  parcel, SMS/WhatsApp per message, Cloudinary/bandwidth if you exceed free tiers.

For a small store doing a few hundred orders a month, expect roughly
**₹3,000–₹8,000/month all-in**, dominated by payment and shipping fees.
