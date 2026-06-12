# Nayabi Collection — Full-Stack Ecommerce Build Plan

> Modesty Wear Ecommerce Platform · Glassmorphic Next-Gen UI · Shopify-Grade Admin
> Status: AWAITING APPROVAL

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Framework | Next.js 14 (App Router) | SSR/SSG, file-based routing, API routes |
| Language | TypeScript | Type safety across frontend + backend |
| Styling | Tailwind CSS v4 + Custom CSS (glassmorphism) | Utility-first + bespoke glass effects |
| Database | PostgreSQL via Prisma ORM | Relational, handles orders/products/users |
| Auth | NextAuth.js v5 | Customer auth + Admin auth (role-based) |
| Payments | Stripe | Cards, UPI, Apple Pay, Google Pay |
| File Storage | Cloudinary | Product image uploads, CDN delivery |
| State Mgmt | Zustand | Cart, wishlist, UI state |
| Email | Resend + React Email | Order confirmations, shipping updates |
| Deployment | Vercel (frontend) + Supabase (DB) | Edge-optimized, zero-config |
| Search | Algolia (or built-in Postgres full-text) | Fast product search |

---

## Project Structure

```
nayabi-collection/
├── app/
│   ├── (storefront)/        ← Customer-facing pages
│   ├── (admin)/             ← Admin dashboard (protected)
│   ├── api/                 ← API routes
│   └── layout.tsx
├── components/
│   ├── ui/                  ← Reusable glass UI components
│   ├── storefront/          ← Shop-specific components
│   └── admin/               ← Admin-specific components
├── lib/                     ← Prisma, Stripe, auth, helpers
├── prisma/                  ← Schema & migrations
├── public/                  ← Static assets
└── styles/                  ← Global CSS, glass variables
```

---

## Phase 1 — Foundation & Design System

### Step 1.1 — Project Scaffolding
- [ ] Initialize Next.js 14 with TypeScript + App Router
- [ ] Configure Tailwind CSS v4
- [ ] Set up ESLint, Prettier, Husky pre-commit hooks
- [ ] Configure absolute imports (`@/` aliases)
- [ ] Set up environment variables template (`.env.example`)

### Step 1.2 — Glassmorphic Design System
- [ ] Define CSS custom properties (design tokens)
  - Glass blur levels: `--glass-sm`, `--glass-md`, `--glass-lg`
  - Color palette: Pearl white, Rose gold, Deep navy, Midnight purple
  - Gradient mesh backgrounds
  - Border radius scale
  - Shadow layers (soft glow, ambient)
- [ ] Build core glass component library:
  - `<GlassCard />` — frosted-glass panels
  - `<GlassButton />` — shimmer hover effects
  - `<GlassInput />` — translucent form fields
  - `<GlassModal />` — backdrop-blur overlays
  - `<GlassNavbar />` — sticky blur header
  - `<GlassBadge />` — pill tags
  - `<GlassToast />` — notification toasts
- [ ] Animated gradient mesh backgrounds (CSS-only, GPU-accelerated)
- [ ] CSS `@layer` architecture for zero-specificity conflicts
- [ ] `prefers-reduced-motion` media query support
- [ ] Dark/light mode via CSS variables + `next-themes`

### Step 1.3 — Database Schema (Prisma)
- [ ] `User` — id, name, email, hashedPassword, role, emailVerified, createdAt, updatedAt
- [ ] `VerificationToken` — token, userId, expiresAt, usedAt (email verification)
- [ ] `PasswordResetToken` — token, userId, expiresAt, usedAt (single-use, 1hr TTL)
- [ ] `Session` — sessionToken, userId, expiresAt (NextAuth managed)
- [ ] `AdminUser` — admin accounts with role permissions (`SUPER_ADMIN` / `MANAGER` / `FULFILLMENT`), loginAttempts, lockedUntil
- [ ] `Product` — id, name, slug, description, category, price, comparePrice, images, stock, tags, isActive
- [ ] `ProductVariant` — size, color, SKU, stock, price override
- [ ] `Category` — Hijabs, Abayas, Namaz Scarfs, Accessories
- [ ] `Order` — customer, items, status, shipping, payment, timestamps
- [ ] `OrderItem` — product snapshot, quantity, price at purchase
- [ ] `Cart` — persistent cart (guest + logged-in)
- [ ] `Wishlist` — saved products per user
- [ ] `Address` — customer shipping/billing addresses
- [ ] `DiscountCode` — code, type (%, fixed, free shipping), conditions, usage limits
- [ ] `Review` — product reviews with star rating, verified purchase flag
- [ ] `ShippingZone` — regions, rates, carrier options
- [ ] `Notification` — admin alerts (new order, low stock)
- [ ] `BlogPost` — optional content marketing (Islamic fashion guides)
- [ ] Run initial migration + seed with demo products

---

## Phase 2 — Storefront (Customer-Facing)

### Step 2.1 — Homepage
- [ ] Animated hero section — full-bleed gradient mesh + glass overlay text
- [ ] "New Arrivals" horizontal scroll carousel (GPU-accelerated)
- [ ] Category grid — Hijabs / Abayas / Namaz Scarfs / Accessories (glass tiles)
- [ ] Featured collection banner
- [ ] Bestsellers section with product cards
- [ ] Trust badges (Free shipping, Authentic, Easy returns)
- [ ] Newsletter signup with glass input
- [ ] Footer — links, social icons, payment method logos

### Step 2.2 — Product Listing Page (PLP)
- [ ] Filterable product grid:
  - Category filter
  - Color swatch filter
  - Size filter (S/M/L/XL or cm-based)
  - Price range slider
  - Sort: Newest, Price Low→High, Price High→Low, Bestselling
- [ ] Infinite scroll / pagination
- [ ] Product card: image hover zoom, quick-add to cart, wishlist toggle
- [ ] Active filter chips with clear-all
- [ ] Results count display
- [ ] Mobile: filter drawer (glass slide-in panel)

### Step 2.3 — Product Detail Page (PDP)
- [ ] Image gallery with zoom + swipe (mobile)
- [ ] Variant selector (color swatches, size buttons)
- [ ] Stock status indicator (In stock / Low stock / Out of stock)
- [ ] Add to Cart with quantity selector
- [ ] Add to Wishlist
- [ ] Size guide modal
- [ ] Product description (tabs: Description / Care Instructions / Shipping)
- [ ] Customer reviews section (star rating, pagination)
- [ ] Related products carousel
- [ ] Share to social media

### Step 2.4 — Shopping Cart
- [ ] Slide-out cart drawer (glass sidebar)
- [ ] Line items with image, name, variant, quantity stepper, remove
- [ ] Real-time subtotal calculation
- [ ] Discount code input field
- [ ] Estimated shipping display
- [ ] Order summary with tax calculation
- [ ] "Continue Shopping" + "Proceed to Checkout" buttons
- [ ] Upsell / cross-sell suggestions in cart
- [ ] Cart persistence (localStorage + DB sync for logged-in users)

### Step 2.5 — Checkout Flow (Multi-Step)
- [ ] **Step 1: Contact** — email, phone (guest or login prompt)
- [ ] **Step 2: Shipping** — address form, address book for returning users
- [ ] **Step 3: Delivery** — shipping method selection with rates
- [ ] **Step 4: Payment** — Stripe Elements (card, UPI, wallets), order summary
- [ ] **Step 5: Confirmation** — order number, summary, estimated delivery
- [ ] Address validation
- [ ] Discount code application at checkout
- [ ] Order notes field
- [ ] Progress indicator bar (glassmorphic stepper)

### Step 2.6 — Authentication Pages (Full Suite)

> All auth pages share a **split-layout**: animated gradient mesh background on the left (brand visual / quote), glass form card centered on the right. Mobile: full-screen glass card.

#### 2.6.1 — Register / Sign Up Page (`/register`)
- [ ] Glass card with soft inner glow, backdrop-blur backdrop
- [ ] Fields: Full Name, Email, Password, Confirm Password
- [ ] Password strength meter (visual bar: weak / fair / strong / very strong)
  - Color-coded: red → amber → green
  - Minimum contrast ratio 4.5:1 against glass card background (WCAG AA)
- [ ] Show/hide password toggle (eye icon, keyboard accessible)
- [ ] Real-time inline validation with readable error messages
  - Error text: `#FF6B6B` on dark glass — verified ≥ 4.5:1 contrast
  - Success tick: `#4ADE80` with sufficient contrast
- [ ] "Already have an account? Sign in" link
- [ ] Google OAuth "Continue with Google" button (glass variant)
- [ ] Terms & Privacy checkbox with links
- [ ] Submit button: gold shimmer CTA (`<GlassButton />`)
- [ ] On success: redirect to `/verify-email` with toast notification
- [ ] ARIA: `role="form"`, `aria-label="Create account"`, `aria-describedby` on all error messages
- [ ] Autofocus on Full Name field, logical tab order

#### 2.6.2 — Login / Sign In Page (`/login`)
- [ ] Glass card centered, brand logo above
- [ ] Fields: Email, Password
- [ ] Show/hide password toggle
- [ ] "Remember me" checkbox
- [ ] "Forgot your password?" link (right-aligned, gold color, underline on focus)
- [ ] Google OAuth "Continue with Google" button
- [ ] Divider: `── or ──` between OAuth and form
- [ ] Submit button with loading spinner state (no layout shift)
- [ ] Error state: glass card border turns amber, error banner inside card
  - Error banner bg: `rgba(239,68,68,0.15)` with `#FCA5A5` text — ≥ 4.5:1 against banner bg
- [ ] "Don't have an account? Register" link
- [ ] Rate limiting feedback: after 5 failed attempts, show lockout timer
- [ ] ARIA: `aria-live="polite"` on error region, `autocomplete="email"` + `autocomplete="current-password"`

#### 2.6.3 — Email Verification Page (`/verify-email`)
- [ ] Sent-confirmation state: glass card with animated mail icon, instructions text
- [ ] "Resend verification email" button (disabled with countdown timer after click)
- [ ] Deep-link handler: `/verify-email?token=xxx` → auto-verifies → redirect to account
- [ ] Success state: animated checkmark, "Email verified! Redirecting..." message
- [ ] Expired token state: clear error message + resend option

#### 2.6.4 — Forgot Password Page (`/forgot-password`)
- [ ] Single email field on glass card
- [ ] Submit sends reset link via Resend email service
- [ ] Success state (same page, no redirect): glass card transforms to confirmation view
  - "Check your inbox" message with email address displayed
  - Resend link with 60-second cooldown timer
- [ ] Input error: "No account found with this email" — neutral wording to prevent user enumeration
- [ ] Back to login link
- [ ] ARIA: `aria-live="polite"` announces state change to screen readers

#### 2.6.5 — Reset Password Page (`/reset-password?token=xxx`)
- [ ] Token validated on page load; if invalid/expired → error state with "Request new link" CTA
- [ ] Fields: New Password, Confirm New Password
- [ ] Password strength meter (same as register page)
- [ ] Show/hide toggles on both fields
- [ ] Validation: passwords match, meets strength requirements
- [ ] On success: auto-login + redirect to `/account` with toast "Password updated successfully"
- [ ] If token already used: "This link has already been used" error

#### 2.6.6 — Change Password Page (`/account/security`) — Authenticated
- [ ] Located within Customer Account settings under "Security" tab
- [ ] Fields: Current Password, New Password, Confirm New Password
- [ ] Current password verification before accepting new password
- [ ] Password strength meter on new password field
- [ ] Show/hide toggles on all three fields
- [ ] Success: inline success message (no page reload), session remains active
- [ ] Wrong current password: inline error with attempt counter
- [ ] Option: "Sign out all other devices" checkbox alongside save

#### 2.6.7 — Admin Login Page (`/admin/login`) — Separate
- [ ] Darker glass variant (`rgba(255,255,255,0.04)`) to visually distinguish from storefront
- [ ] "Admin Portal — Nayabi Collection" heading
- [ ] Email + Password fields only (no OAuth for admin)
- [ ] IP-based rate limiting feedback (5 attempts → 15-min lockout)
- [ ] 2FA prompt page (TOTP code input) after credentials validated — future-ready stub
- [ ] No "register" link — admin accounts are invitation-only
- [ ] Redirect to `/admin/dashboard` on success

---

#### Auth — Glassmorphic Visual Spec (All Pages)
| Element | Style |
|---|---|
| Page background | Animated gradient mesh: deep navy → amethyst → midnight purple |
| Form card | `backdrop-filter: blur(24px)`, `background: rgba(255,255,255,0.07)`, `border: 1px solid rgba(255,255,255,0.14)`, `border-radius: 24px`, inner box-shadow glow |
| Card max-width | `440px` (desktop), full-width `16px` margin (mobile) |
| Input fields | `background: rgba(255,255,255,0.05)`, white border on focus, `color: #F0EEF8` (≥ 7:1 contrast on glass bg) |
| Input placeholder | `rgba(240,238,248,0.45)` — passes 3:1 (decorative threshold) |
| Label text | `#E8E4F4` — ≥ 7:1 contrast on glass background |
| Primary button | Gold gradient with shimmer sweep animation, `color: #0A0A14` (dark text on gold — ≥ 7:1) |
| Link color | `#C9A96E` gold — ≥ 4.5:1 on dark glass background |
| Error text | `#FCA5A5` — ≥ 4.5:1 on glass background |
| Success text | `#86EFAC` — ≥ 4.5:1 on glass background |
| Focus ring | `outline: 2px solid #C9A96E`, `outline-offset: 3px` — visible on all backgrounds |

#### Auth — Accessibility Checklist (All Pages)
- [ ] All color combinations verified ≥ 4.5:1 contrast ratio (WCAG AA normal text)
- [ ] Interactive elements ≥ 3:1 contrast (WCAG AA UI components)
- [ ] Focus indicators visible on glass + dark backgrounds
- [ ] No information conveyed by color alone (icons + text accompany color states)
- [ ] Form errors linked via `aria-describedby`, not just color
- [ ] `autocomplete` attributes on all credential fields
- [ ] Logical heading hierarchy (`h1` per page, no skipped levels)
- [ ] Mobile touch targets ≥ 44×44px
- [ ] `prefers-reduced-motion`: disable animated background mesh, keep static gradient
- [ ] Screen reader tested: VoiceOver (macOS) + NVDA (Windows)

#### Auth — Email Templates (Resend + React Email)
- [ ] **Verification email** — branded glass-style HTML, Nayabi logo, verify button CTA
- [ ] **Password reset email** — "You requested a password reset" + reset button + 1-hour expiry notice
- [ ] **Password changed confirmation** — "Your password was changed" + "Not you? Contact us" link
- [ ] **Welcome email** — post-registration, brief onboarding CTA to browse collections
- [ ] All emails: plain-text fallback, unsubscribe footer, accessible HTML structure

### Step 2.7 — Customer Account
- [ ] Dashboard — recent orders, saved addresses, loyalty points
- [ ] Order history with status tracking timeline
- [ ] Order detail page — reorder button, track shipment link
- [ ] Address book management (add/edit/delete/default)
- [ ] Profile settings (name, email, password change)
- [ ] Wishlist page
- [ ] Return/refund request form

### Step 2.8 — Search
- [ ] Header search bar with instant suggestions (debounced)
- [ ] Full search results page with filters
- [ ] Search history (localStorage)
- [ ] No-results state with alternative suggestions

### Step 2.9 — Static / Content Pages
- [ ] About Us — brand story, modest fashion values
- [ ] Contact Us — form + map embed
- [ ] Shipping & Returns Policy
- [ ] Privacy Policy & Terms of Service
- [ ] FAQ accordion page
- [ ] Size Guide page

---

## Phase 3 — Admin Dashboard

> Accessible at `/admin` — role-based protection, separate auth session

### Step 3.1 — Admin Layout & Navigation
- [ ] Collapsible sidebar navigation (glass, icon + label)
- [ ] Top bar: search, notifications bell, admin profile menu
- [ ] Responsive: sidebar collapses to bottom nav on mobile
- [ ] Role permissions: `SUPER_ADMIN`, `MANAGER`, `FULFILLMENT`

### Step 3.2 — Dashboard Overview
- [ ] KPI cards: Total Revenue, Orders Today, New Customers, Avg. Order Value
- [ ] Revenue chart (line graph — 7d / 30d / 90d / custom range)
- [ ] Orders by status doughnut chart
- [ ] Recent orders live table
- [ ] Top selling products table
- [ ] Low stock alerts panel
- [ ] Real-time notifications (new orders, payment failures)

### Step 3.3 — Order Management
- [ ] Orders list with filters: status, date range, fulfillment, payment
- [ ] Status filters: Pending → Processing → Shipped → Delivered → Cancelled → Refunded
- [ ] Bulk actions: mark shipped, export CSV, print packing slips
- [ ] Order detail page:
  - Customer info, items, variants, quantities
  - Payment status + Stripe payment link
  - Timeline of status changes
  - Edit shipping address
  - Add internal notes
  - Manual refund initiation
  - Print invoice PDF
- [ ] Fulfillment workflow: assign tracking number, carrier, notify customer

### Step 3.4 — Product Management
- [ ] Product list with search, filter by category/status, sort
- [ ] Add/Edit product form:
  - Rich text description editor
  - Multiple image upload (drag-and-drop to Cloudinary)
  - Category assignment
  - Variant builder (color + size matrix)
  - Pricing: base price, compare-at price, cost price
  - SEO fields (meta title, description, URL slug)
  - Tags, collections
  - Inventory per variant
  - Weight/dimensions for shipping calc
  - Toggle: Active / Draft / Archived
- [ ] Duplicate product
- [ ] Bulk import via CSV
- [ ] Bulk edit: price, stock, status

### Step 3.5 — Inventory Management
- [ ] Stock levels per variant
- [ ] Low-stock threshold alerts
- [ ] Stock adjustment logs (reason: received, damaged, returned)
- [ ] Export inventory report

### Step 3.6 — Category & Collection Management
- [ ] Add/edit/delete categories (Hijabs, Abayas, Namaz Scarfs, Accessories)
- [ ] Custom collections (e.g., "Eid Collection 2025", "Summer Pastels")
- [ ] Drag-and-drop category sort order
- [ ] Category images + banner images

### Step 3.7 — Customer Management
- [ ] Customer list: name, email, orders count, total spent, join date
- [ ] Customer detail: profile, order history, saved addresses, notes
- [ ] Add/edit/delete customers
- [ ] Export customer list CSV

### Step 3.8 — Discount & Promotions
- [ ] Discount codes:
  - Percentage off (e.g., SAVE10 = 10% off)
  - Fixed amount off (e.g., EID50 = PKR 50 off)
  - Free shipping
  - Buy X Get Y (BOGO)
- [ ] Conditions: min order value, specific products/categories, first-time customers
- [ ] Usage limits: total uses, one-per-customer
- [ ] Validity: start/end date & time
- [ ] Auto-generate bulk discount codes (e.g., for influencer campaigns)
- [ ] Usage analytics per code
- [ ] Flash sale / limited-time banner management (homepage banner)

### Step 3.9 — Shipping Management
- [ ] Shipping zones (local city, national, international)
- [ ] Shipping rates per zone (flat rate, weight-based, price-based)
- [ ] Free shipping threshold (e.g., free over PKR 2000)
- [ ] Carrier integration options (TCS, Leopards, DHL stubs)
- [ ] Manual order fulfillment with custom tracking number input
- [ ] Shipping label generation (PDF)

### Step 3.10 — Analytics & Reports
- [ ] Sales report: revenue by day/week/month/year
- [ ] Product performance: views, add-to-cart rate, conversion
- [ ] Customer acquisition report
- [ ] Discount code performance
- [ ] Inventory value report
- [ ] Export all reports as CSV/Excel
- [ ] Date range picker for all reports

### Step 3.11 — Store Settings
- [ ] Store info: name, logo, address, contact, currency (PKR / USD toggle)
- [ ] Email notification templates (order confirmed, shipped, delivered)
- [ ] Payment gateway settings (Stripe keys)
- [ ] Tax settings (GST/sales tax rates by region)
- [ ] Store hours / maintenance mode toggle
- [ ] Admin user management (invite admins, assign roles)
- [ ] Storefront theme accent color picker

---

## Phase 4 — Performance, SEO & Polish

### Step 4.1 — Performance
- [ ] Next.js Image component with WebP/AVIF auto-conversion
- [ ] Lazy loading for below-fold content
- [ ] React Suspense + streaming for product pages
- [ ] Edge caching for product listings (ISR — Incremental Static Regeneration)
- [ ] CSS containment (`contain: layout style`) on glass cards
- [ ] `will-change: transform` on animated elements (careful usage)
- [ ] Bundle analyzer — eliminate dead code
- [ ] Core Web Vitals target: LCP < 2.5s, CLS < 0.1, INP < 200ms

### Step 4.2 — SEO
- [ ] Dynamic `<meta>` tags per page (Next.js Metadata API)
- [ ] Open Graph + Twitter Card images
- [ ] JSON-LD structured data (Product, BreadcrumbList, Organization)
- [ ] XML sitemap auto-generation
- [ ] robots.txt
- [ ] Canonical URLs
- [ ] Arabic/Urdu language meta support (optional)

### Step 4.3 — Accessibility (Site-Wide)

> Glassmorphic UI introduces readability risks — low-opacity backgrounds, subtle borders, blurred layers. Every glass surface must pass WCAG AA contrast before shipping.

#### Contrast Compliance Rules (all pages)
- [ ] Body text on glass panels: ≥ **7:1** (WCAG AAA target where possible)
- [ ] UI component text (buttons, labels, badges): ≥ **4.5:1** (WCAG AA)
- [ ] Interactive UI elements (borders, icons, controls): ≥ **3:1**
- [ ] Placeholder text: documented as decorative (< 4.5:1 acceptable per spec), but kept ≥ 3:1
- [ ] Never rely on color alone to convey meaning — always pair with icon or text label
- [ ] Verified with: Colour Contrast Analyser + automated `axe-core` in CI

#### Interactive Element Accessibility
- [ ] Full keyboard navigation: Tab, Shift+Tab, Enter, Space, Arrow keys
- [ ] `focus-visible` rings: `2px solid #C9A96E`, `outline-offset: 3px` — renders on all bg types
- [ ] Skip-to-content link at top of every page
- [ ] No focus traps except intentional modal dialogs (escapable via Escape key)
- [ ] All icon-only buttons have `aria-label`
- [ ] All images have meaningful `alt` text; decorative images use `alt=""`

#### Semantic Structure
- [ ] Logical `<h1>`→`<h2>`→`<h3>` hierarchy on every page (no skipped levels)
- [ ] Landmark regions: `<header>`, `<nav>`, `<main>`, `<footer>`, `<aside>`
- [ ] Product lists use `<ul>`/`<li>` for screen reader list count announcements
- [ ] Data tables (order history, admin tables) use `<th scope>` and `<caption>`
- [ ] Form fields: every `<input>` has associated `<label>` (not just placeholder)

#### Dynamic Content
- [ ] Cart drawer open/close: `aria-expanded`, `aria-controls`, focus management
- [ ] Toast notifications: `role="status"` or `role="alert"` based on urgency
- [ ] Filter changes: `aria-live="polite"` announces updated product count
- [ ] Page transitions: no full-page flash; `aria-busy="true"` during loading
- [ ] Modal dialogs: `role="dialog"`, `aria-modal="true"`, focus trapped inside

#### Glass-Specific Accessibility
- [ ] Every `backdrop-filter` element has an opaque fallback background (for browsers without support)
- [ ] Text on glass always sits above a minimum-opacity backing layer — no raw text on pure glass
- [ ] `@supports (backdrop-filter: blur())` progressive enhancement — solid card fallback
- [ ] Dark mode: all contrast ratios re-verified for dark mode variant
- [ ] `prefers-contrast: more` media query: increase glass opacity + border weight
- [ ] `prefers-reduced-motion`: animated mesh → static gradient, no hover transform animations

### Step 4.4 — Mobile Experience
- [ ] Touch-optimized swipe gestures (product gallery, cart drawer)
- [ ] Bottom navigation bar on mobile storefront
- [ ] PWA manifest + service worker (offline product browsing)
- [ ] Add to Home Screen support

---

## Phase 5 — Testing & Deployment

### Step 5.1 — Testing
- [ ] Unit tests: utility functions, price calculations, discount logic
- [ ] Integration tests: checkout flow, cart operations
- [ ] E2E tests with Playwright: homepage → PDP → cart → checkout
- [ ] Admin tests: product CRUD, order status updates
- [ ] Stripe webhook testing (test mode)

### Step 5.2 — Deployment
- [ ] Production environment variables setup
- [ ] Vercel project configuration
- [ ] Supabase production DB + connection pooling (PgBouncer)
- [ ] Cloudinary production account
- [ ] Custom domain setup (nayabicollection.com)
- [ ] SSL certificate (auto via Vercel)
- [ ] Stripe live mode activation
- [ ] Error monitoring: Sentry integration
- [ ] Uptime monitoring

---

## Glassmorphic UI Specification

### Color Palette
| Token | Value | Usage |
|---|---|---|
| `--color-primary` | `#C9A96E` (warm gold) | CTAs, highlights |
| `--color-secondary` | `#8B5E9B` (amethyst) | Accents |
| `--color-bg-dark` | `#0A0A14` (deep navy) | Page background |
| `--color-bg-mid` | `#12122A` | Section backgrounds |
| `--glass-bg` | `rgba(255,255,255,0.06)` | Card backgrounds |
| `--glass-border` | `rgba(255,255,255,0.12)` | Card borders |
| `--glass-blur` | `blur(20px)` | Backdrop filter |
| `--glow-gold` | `0 0 40px rgba(201,169,110,0.3)` | Hover glows |
| `--glow-purple` | `0 0 40px rgba(139,94,155,0.3)` | Accent glows |

### Typography
- Display: **Playfair Display** (elegant serif for headings)
- Body: **Inter** (clean, highly legible)
- Arabic/Urdu support: **Noto Naskh Arabic** (for future localization)

### Animation Principles
- All transitions: `cubic-bezier(0.4, 0, 0.2, 1)` (ease-in-out)
- Duration scale: `150ms` (micro) / `300ms` (standard) / `500ms` (page)
- GPU-only: `transform` + `opacity` only (no `width/height` animations)
- Stagger delays on list entrances (50ms per item)

---

## Delivery Milestones

| Milestone | Deliverable | Est. Scope |
|---|---|---|
| **M1** | Design system + DB schema + project setup | Phase 1 |
| **M2** | Homepage + PLP + PDP | Phase 2 (2.1–2.3) |
| **M3** | Cart + Checkout + Full Auth Suite (Register, Login, Email Verify, Forgot/Reset/Change Password, Admin Login) + Email Templates | Phase 2 (2.4–2.6) |
| **M4** | Customer account + Search + Static pages | Phase 2 (2.7–2.9) |
| **M5** | Admin: Dashboard + Orders + Products | Phase 3 (3.1–3.5) |
| **M6** | Admin: Customers + Discounts + Shipping + Analytics | Phase 3 (3.6–3.11) |
| **M7** | Performance + SEO + PWA + Testing | Phase 4–5 |
| **M8** | Deployment + Domain + Monitoring | Phase 5.2 |

---

## What's NOT Included (Can Be Added Later)
- Multi-vendor / marketplace features
- Loyalty points / referral program
- Live chat integration
- Subscription / recurring orders
- Arabic RTL full layout (Urdu/Arabic storefront)
- Native mobile apps (React Native)
- ERP / accounting integrations

---

*Awaiting your approval to begin development starting with Milestone M1.*
