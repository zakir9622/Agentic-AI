# Nayabi Collection — Technology & Dependency Risk Report

_Last updated: 2026-06-16_

A plain-language assessment of the technologies and libraries this project
depends on, the risks each carries, and how those risks are mitigated. Risk
levels: 🟢 Low · 🟡 Medium · 🔴 High.

---

## 1. Core framework & runtime

| Technology | Version | Role | Risk | Notes / mitigation |
| --- | --- | --- | --- | --- |
| **Next.js** | 16.2.9 | App framework (App Router, SSR, API) | 🟡 | Very recent major. Next 16 renamed `middleware`→`proxy` and changes conventions release-to-release. Pinned exact version; upgrade deliberately and re-read release notes. |
| **React** | 19.2.4 | UI library | 🟢 | Stable, mainstream. |
| **TypeScript** | 5.x | Type safety | 🟢 | Catches errors at build; `npm run typecheck` in CI. |
| **Node.js** | 20+ | Runtime | 🟢 | LTS. Keep on an active-LTS line. |
| **Tailwind CSS** | v4 | Styling | 🟡 | v4 is a ground-up rewrite (`@theme`, new engine). Smaller community knowledge base than v3; design tokens are centralized in `globals.css` to limit blast radius. |

**Framework risk summary:** 🟡 — the stack is modern and well-supported, but
**Next.js 16 + Tailwind v4 are both new majors**. The main risk is churn on
upgrades, not stability. Mitigation: exact-pinned versions, a green
build/lint/typecheck gate, and centralized design tokens.

---

## 2. Data layer

| Technology | Version | Role | Risk | Notes / mitigation |
| --- | --- | --- | --- | --- |
| **Prisma** | 7.8.0 | ORM / DB client | 🟡 | Prisma 7 uses the new `prisma-client` generator + driver adapters (`@prisma/adapter-pg`). Generated client is committed under `src/generated/prisma`. Schema changes require `prisma generate` + migration. |
| **PostgreSQL** | (managed) | Database | 🟢 | Industry standard. Use a managed provider with backups. |
| **pg / @prisma/adapter-pg** | 8.x / 7.8 | Postgres driver | 🟢 | Standard. |

**Mitigation:** every DB read in `src/lib/catalog.ts` is wrapped in try/catch
with **static demo-data fallbacks**, so the storefront still renders if the DB
is briefly unavailable or unseeded. This is a deliberate resilience choice.

---

## 3. Auth & security

| Library | Version | Role | Risk | Notes |
| --- | --- | --- | --- | --- |
| **next-auth** | 5.0.0-**beta**.31 | Authentication | 🔴 | **Beta**. v5 (Auth.js) APIs can still change. Highest single dependency risk. Mitigation: pin exact version; test auth flows before any bump. |
| **bcryptjs** | 3.x | Password hashing | 🟢 | Pure-JS bcrypt; widely used. |
| **@upstash/ratelimit** + **@upstash/redis** | 2.x / 1.x | Rate limiting | 🟢 | Optional; degrades gracefully if not configured. |
| **Cloudflare Turnstile** | (service) | Bot protection | 🟢 | Free, privacy-friendly CAPTCHA. |
| **zod** | 4.x | Input validation | 🟢 | v4 is current; used for server-action/form validation. |

**Security posture:** security headers set in `next.config.ts` (HSTS, X-Frame,
nosniff, Referrer-Policy, Permissions-Policy); admin routes gated by `proxy.ts`;
secrets kept in environment variables. **Action item:** change the default admin
password before launch.

> ⚠️ **Top risk to watch:** `next-auth@5 beta`. Treat auth as a pinned,
> change-controlled dependency until v5 reaches stable.

---

## 4. Payments, comms & shipping (third-party services)

| Library / Service | Role | Risk | Notes |
| --- | --- | --- | --- |
| **Razorpay** (REST) | Payments | 🟡 | External dependency on uptime + KYC approval. Webhook signature verified. ~2% fee is a business (not technical) risk. |
| **Resend** (`resend`, `@react-email/components`) | Email | 🟢 | Fails silently with a placeholder key in dev; verify sending domain in prod. |
| **MSG91** (REST) | SMS + WhatsApp | 🟡 | Indian SMS needs **DLT-approved templates**; WhatsApp needs an approved Business sender. All calls fail-silent so they never break checkout. |
| **Shiprocket** (REST) | Shipping aggregator | 🟡 | Token-based auth; fail-silent wrapper. Multi-carrier abstraction in `src/lib/shipping/` lets us add/replace couriers. |
| **Delhivery / Amazon Shipping** | Direct couriers | 🟡 | Require commercial contracts/approval + credentials. Implemented behind the same provider interface; Amazon adapter is a documented scaffold pending API approval. |
| **Sentry** (`@sentry/nextjs`) | Error monitoring | 🟢 | Optional; no DSN = disabled. |

**Mitigation pattern:** every external integration is **fail-silent** (logs and
returns null/no-op) so a third-party outage degrades a feature rather than
crashing checkout or the page render.

---

## 5. UI & state libraries

| Library | Version | Role | Risk | Notes |
| --- | --- | --- | --- | --- |
| **zustand** | 5.x | Client state (cart, recently-viewed) | 🟢 | Small, stable, persisted to localStorage. |
| **next-themes** | 0.4.x | Dark/light theme switching | 🟢 | Tiny, mature. |
| **lucide-react** | 1.18.0 | Icons | 🟡 | Icon set differs between versions (e.g. no `Instagram` export in 1.18 → custom SVG used). Auto tree-shaken by Next. Verify icon names exist before importing. |
| **react-hook-form** + **@hookform/resolvers** | 7.x / 5.x | Forms | 🟢 | Mainstream. |
| **clsx / tailwind-merge / class-variance-authority** | — | Class utilities | 🟢 | Trivial, stable. |
| **nanoid** | 5.x | ID generation | 🟢 | Stable. |

---

## 6. Build & tooling

| Tool | Risk | Notes |
| --- | --- | --- |
| **Turbopack** (Next build) | 🟡 | Now the default builder in Next 16; newer than webpack but maintained by Vercel. |
| **ESLint 9 / eslint-config-next** | 🟢 | Enforces React-hooks rules (caught real issues during build). |
| **Prettier 3 + prettier-plugin-tailwindcss** | 🟢 | Consistent formatting; runs via lint-staged. |
| **Husky + lint-staged** | 🟢 | Pre-commit quality gate. |
| **vercel** (CLI, devDep) | 🟢 | Deploy tooling. |

---

## 7. Overall risk register (prioritized)

| # | Risk | Level | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- | --- | --- |
| 1 | `next-auth@5` is **beta** — breaking changes on upgrade | 🔴 | Medium | High (login breaks) | Pin version; regression-test auth before any bump; watch Auth.js releases. |
| 2 | **Next.js 16 + Tailwind v4** are new majors — upgrade churn | 🟡 | Medium | Medium | Exact-pin; read release notes; centralized tokens; green CI gate. |
| 3 | **Payment/shipping/SMS providers** outage or policy change | 🟡 | Low–Med | Medium | Fail-silent wrappers; webhook signature checks; multi-carrier abstraction. |
| 4 | **DLT / WhatsApp template approval** delays in India | 🟡 | Medium | Medium (no SMS at launch) | Start approvals early; SMS/WhatsApp are non-blocking. |
| 5 | **Default admin password** shipped in code | 🔴 | — | High if unchanged | **Change before launch** (documented in DEPLOYMENT.md). |
| 6 | **Prisma 7** generated client + migrations drift | 🟡 | Low | Medium | Client committed; run `db:push`/`migrate deploy` on deploy; DB fallbacks. |
| 7 | **Secrets in env** leaking | 🟡 | Low | High | `.env` git-ignored; secrets only in host env; rotate on exposure. |
| 8 | **Single maintainer / bus factor** | 🟡 | — | Medium | This doc + README + structured code lower onboarding cost. |

---

## 8. Recommendations

1. **Before launch:** change the admin password, switch Razorpay to live keys,
   verify Resend domain, get DLT/WhatsApp templates approved.
2. **Treat `next-auth@5` as change-controlled** — do not auto-upgrade.
3. **Enable daily DB backups** at the managed Postgres provider.
4. **Turn on Sentry** in production for early error visibility.
5. **Re-run `npm run build && npm run lint && npm run typecheck`** on every
   dependency bump — this is the project's safety net.
6. **Review third-party fees quarterly** (Razorpay %, courier rates) — the main
   variable cost drivers.
