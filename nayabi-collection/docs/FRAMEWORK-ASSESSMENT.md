# Framework Assessment — Is Next.js the Problem?

> **Bottom line:** No. The slowness you feel is **`next dev` (Turbopack)
> first-time route compilation** — a development-only cost. **Production renders
> in 1.5–50 ms per route with no database.** A rewrite to another framework would
> cost weeks, carry high risk, and would **not** remove on-demand dev compilation
> (every modern framework does it). Recommendation: **stay on Next.js** and apply
> the dev-speed mitigations below.

---

## 1. What we actually measured (June 2026)

Both servers were run on this machine **with the database intentionally OFF**
(Postgres refuses the connection instantly with `ECONNREFUSED`, so the demo-data
fallbacks engage with no hang).

### Production (`npm run build` + `npm run start`) — time to first byte
| Route | TTFB |
|---|---|
| `/` (home) | **4.9 ms** |
| `/shop` | **50 ms** |
| `/products/[slug]` (PDP) | **21 ms** |
| `/gift-cards` | **1.7 ms** |
| `/track` | **1.6 ms** |
| `/about` | **1.9 ms** |

### Development (`npm run dev`, Turbopack) — total request time
| Route | Cold (first hit) | Warm (second hit) |
|---|---|---|
| `/` | 0.43 s | 0.35 s |
| `/shop` | 0.71 s | 0.29 s |
| `/products/[slug]` | **3.69 s** | 0.34 s |

### Reading the numbers
- **Production is not slow.** Sub-50 ms server render, no DB required.
- **The DB being down adds ~0 latency** — `ECONNREFUSED` is immediate; the
  `try/catch → demo data` fallbacks return at once. (A *misconfigured* DB that
  black-holes connections *would* hang — see mitigation #5.)
- **The 3.7 s you see is Turbopack compiling the PDP route on first visit in dev.**
  It is paid once per route per dev session, then drops to ~0.3 s. The PDP is the
  heaviest route (gallery + actions + reviews + related + recently-viewed), so it
  compiles slowest.

**Conclusion:** the symptom is *dev-server on-demand compilation*, not *rendering*,
not *Next.js runtime*, and not *the database*. Your end users never pay this cost.

---

## 2. Why a framework swap would not fix it

On-demand route compilation in dev is **universal** to the modern bundler model:

| Framework | Dev engine | First-hit compile in dev? |
|---|---|---|
| Next.js 16 | Turbopack | Yes |
| Remix / React Router 7 | Vite | Yes (Vite lazy-transforms on request) |
| TanStack Start | Vite | Yes |
| SvelteKit | Vite | Yes |
| Astro | Vite | Yes |
| Plain Vite SPA | Vite | Yes |

Vite-based stacks often *feel* snappier on a cold first hit because they ship
unbundled ESM in dev, but they pay it back as **many small module requests** and
still recompile on first route visit. The 3.7 s figure is also inflated by this
route's component count and by first-run TS/SWC warmup — both of which exist in
any toolchain. **You would spend weeks migrating to trade one dev-compile model
for another, with no guaranteed win and a large correctness risk.**

---

## 3. Make dev fast now (recommended, ~1 hour, low risk)

1. **Warm the routes you work on.** Turbopack compiles lazily; after the first
   hit a route is cached for the session. Hit the route once, then iterate.
2. **Keep the dev server running.** Don't restart between edits — HMR keeps
   compiled routes warm. Cold restarts re-pay first-hit compiles.
3. **Use production for "is it fast?" judgements.** `npm run build && npm run
   start` reflects real user performance (the table above). Never benchmark UX on
   `next dev`.
4. **Split the heaviest route.** The PDP pulls many client components. Lazy-load
   the below-the-fold, interactivity-only pieces (reviews list, related grid,
   recently-viewed) with `next/dynamic` so the first compile/parse is smaller.
   This also trims the production JS bundle.
5. **Guarantee instant DB failure in dev.** If you point `DATABASE_URL` at a host
   that *drops* packets (not one that refuses), every render waits for the Prisma
   connect timeout. Set a short connect timeout
   (`?connect_timeout=2`) or leave `DATABASE_URL` unset locally so the demo
   fallbacks fire immediately.
6. **Raise Node's memory if compiles thrash** on large edits:
   `NODE_OPTIONS=--max-old-space-size=4096 npm run dev`.

None of these require leaving Next.js or touching the production path.

---

## 4. Risk register — if you still choose to migrate

| # | Risk | Likelihood | Impact | Notes / mitigation |
|---|---|---|---|---|
| R1 | **Loss of App-Router features** (RSC, Server Actions, route-level caching, `proxy`/middleware, `generateMetadata`, ISR) | High | High | The app leans on Server Actions (`src/app/actions/*`), RSC data loading (`lib/catalog`), ISR (`revalidate`), and the admin `proxy.ts`. Remix/React Router have analogues (loaders/actions) but **not** RSC; Astro needs an islands rewrite of every interactive page. Expect to re-architect data flow. |
| R2 | **Rewrite scope** — 30+ routes, 11 server-action modules, 20+ API routes, Prisma, Razorpay, NextAuth, MSG91, Cloudinary, shipping adapters | High | High | This is a multi-week rewrite, not a port. Every integration is wired to Next conventions (route handlers, `cookies()`, `next/headers`). |
| R3 | **Auth migration** — NextAuth v5 is Next-specific | Medium | High | A new framework needs a different auth library (e.g. Auth.js core, Lucia) → re-test every protected route + the admin cookie flow. |
| R4 | **Image optimization** — `next/image` (sizing, lazy, CDN) | High | Medium | Replace with framework-native (`@unpic`, Astro `<Image>`) or a service; re-verify all art direction + `sizes`. |
| R5 | **SEO / metadata / sitemap / robots** regressions | Medium | High | `generateMetadata`, `sitemap.ts`, `robots.ts`, JSON-LD all reimplement differently. Risk of silent SEO loss. |
| R6 | **Test suite churn** | High | Medium | 32 Playwright e2e + 230 unit tests assume current routes/markup. e2e mostly survives (selectors are route/role-based); unit tests are framework-agnostic. Still, full re-run + fixes needed. |
| R7 | **Hosting / deploy change** | Medium | Medium | `vercel.json` cron jobs + function durations are Vercel-specific. A new stack may need a different host/cron mechanism. |
| R8 | **Opportunity cost** | High | High | Weeks spent migrating buy *no user-visible improvement* (prod is already fast) and *no dev-compile elimination* (R in §2). |

### What is **portable** (low risk, reassuring)
- The entire **design system is framework-agnostic CSS** — `src/app/globals.css`
  (tokens, glass system, themes) moves verbatim.
- **Domain logic** is plain TS: `lib/pricing`, `lib/loyalty`, `lib/gift-cards`,
  `lib/shipping/*`, `lib/utils`, `lib/razorpay` (crypto) — all portable.
- **Prisma schema + client** are framework-independent.
- **Unit/regression tests** (`tests/*.test.ts`) run on `node:test` — portable.

---

## 5. If migration is still desired — phased, de-risked plan

Only pursue this for a *strategic* reason (team standardization, leaving Vercel,
etc.) — **not** for the dev-compile symptom, which §3 fixes.

- **Phase 0 — Decide the target.** Most defensible options:
  - **Stay on Next.js** (recommended) — apply §3.
  - **React Router 7 (framework mode) on Vite** — closest App-Router analogue with
    loaders/actions; no RSC. Best "feels like Next, runs on Vite" option.
  - **Astro + React islands** — only if the catalog/content pages dominate and
    interactivity is localized; biggest rewrite of the interactive flows.
- **Phase 1 — Spike (1–2 days).** Port *one* read route (`/shop`) and *one*
  mutation (cart add or `track` action) to the target. Measure dev + prod timings
  and developer ergonomics against the tables above. Kill the project here if the
  spike doesn't beat tuned Next.js.
- **Phase 2 — Shared core.** Extract `lib/*`, `globals.css`, Prisma, and unit
  tests into a framework-neutral package (they already are — just relocate).
- **Phase 3 — Storefront routes**, behind a feature flag / parallel deploy.
- **Phase 4 — Auth + admin + server actions** (highest risk; do last).
- **Phase 5 — Integrations** (Razorpay, webhooks, cron, mail/SMS), then cut over
  hosting and DNS.
- **Gate every phase** on the full e2e + unit suite staying green.

---

## 6. Recommendation

1. **Keep Next.js.** Production performance is already excellent (§1).
2. **Apply §3** (warm routes, lazy-load the PDP's heavy client components, ensure
   instant DB failure locally). Expect the cold PDP compile to drop materially and
   the production bundle to shrink.
3. **Re-benchmark on `next start`, not `next dev`** — that is what your customers
   experience.
4. Revisit a framework change only if a **business/architecture** driver appears;
   if so, follow §5 and let the Phase-1 spike make the decision on evidence.
