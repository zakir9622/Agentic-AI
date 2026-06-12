# Nayabi Collection — Full-Stack Ecommerce Build Plan

> Modesty Wear · India Market · Glassmorphic Next-Gen UI · Shopify-Grade Admin
> Status: AWAITING FINAL APPROVAL

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Framework | Next.js 14 (App Router) | SSR/SSG, file-based routing, API routes |
| Language | TypeScript | Type safety across frontend + backend |
| Styling | Tailwind CSS v4 + Custom CSS (glassmorphism) | Utility-first + bespoke glass effects |
| Database | PostgreSQL via Prisma ORM | Relational, handles all ecommerce data |
| Cache / Sessions | Upstash Redis | Rate limiting, sessions, abandoned cart tracking |
| Auth | NextAuth.js v5 | Customer + Admin auth, role-based |
| Payments | Razorpay (UPI, cards, net banking, wallets) + COD | India's dominant gateway, native UPI support |
| File Storage | Cloudinary | Product images/videos, CDN delivery |
| State Mgmt | Zustand | Cart, wishlist, UI state |
| Email | Resend + React Email | Transactional emails, abandoned cart recovery |
| SMS / WhatsApp | MSG91 | OTP, order SMS, WhatsApp Business messages |
| Shipping | Shiprocket API | Multi-courier aggregator, COD, tracking |
| Deployment | Vercel (frontend) + Supabase (DB) | Edge-optimized, zero-config |
| Error Monitoring | Sentry | Production error tracking |
| Search | Postgres full-text search (pgvector) | Fast product search, no extra cost |

---

## Project Structure

```
nayabi-collection/
├── app/
│   ├── (storefront)/        ← Customer-facing pages
│   ├── (admin)/             ← Admin dashboard (protected)
│   ├── api/                 ← API routes (auth, payments, webhooks, etc.)
│   └── layout.tsx
├── components/
│   ├── ui/                  ← Reusable glass UI primitives
│   ├── storefront/          ← Shop-specific components
│   └── admin/               ← Admin-specific components
├── lib/                     ← Prisma, Razorpay, auth, MSG91, helpers
├── prisma/                  ← Schema & migrations
├── public/                  ← Static assets
└── styles/                  ← Global CSS, glass design tokens
```

---

## Phase 1 — Foundation & Design System

### Step 1.1 — Project Scaffolding
- [ ] Initialize Next.js 14 with TypeScript + App Router
- [ ] Configure Tailwind CSS v4
- [ ] Set up ESLint, Prettier, Husky pre-commit hooks
- [ ] Configure `@/` absolute imports
- [ ] `.env.example` with all required keys documented
- [ ] CI/CD: GitHub Actions — lint + typecheck on every push

### Step 1.2 — Glassmorphic Design System
- [ ] CSS custom properties (design tokens):
  - Glass levels: `--glass-sm` / `--glass-md` / `--glass-lg`
  - Color palette: warm gold, amethyst, deep navy, midnight purple
  - Gradient mesh backgrounds, border radius scale, glow shadows
- [ ] Core glass component library:
  - `<GlassCard />` — frosted-glass panels with inner glow
  - `<GlassButton />` — shimmer hover, loading state
  - `<GlassInput />` — translucent fields, focus glow
  - `<GlassModal />` — backdrop-blur overlay, focus trap
  - `<GlassNavbar />` — sticky blur header
  - `<GlassBadge />` — pill tags
  - `<GlassToast />` — notification toasts (`role="alert"`)
  - `<GlassSkeleton />` — shimmer loading placeholders
  - `<AnnouncementBar />` — admin-controlled sitewide banner
- [ ] Animated gradient mesh backgrounds (CSS-only, GPU-accelerated)
- [ ] `@supports (backdrop-filter: blur())` — opaque solid card fallback
- [ ] `prefers-reduced-motion` — disable animated mesh, keep static gradient
- [ ] `prefers-contrast: more` — increase glass opacity + border weight
- [ ] Dark/light mode via CSS variables + `next-themes`
- [ ] Custom 404 page — branded, with search + navigation
- [ ] Custom 500 error page — branded, with "Go home" CTA

### Step 1.3 — Database Schema (Prisma)

**Auth & Users**
- [ ] `User` — id, name, email, phone, hashedPassword, role, emailVerified, createdAt
- [ ] `VerificationToken` — token, userId, expiresAt, usedAt
- [ ] `PasswordResetToken` — token, userId, expiresAt, usedAt (1hr TTL, single-use)
- [ ] `Session` — sessionToken, userId, expiresAt (NextAuth managed)
- [ ] `AdminUser` — id, name, email, hashedPassword, role, loginAttempts, lockedUntil

**Catalogue**
- [ ] `Category` — id, name, slug, image, bannerImage, sortOrder, parentId (for sub-categories)
- [ ] `Collection` — id, name, slug, description, image (Eid Collection, Summer Pastels, etc.)
- [ ] `Product` — id, name, slug, description, categoryId, collectionId, images[], videoUrl, price, comparePrice, costPrice, weight, dimensions, tags[], isActive, seoTitle, seoDescription, createdAt
- [ ] `ProductVariant` — id, productId, color, size, fabric, SKU, stock, lowStockThreshold, price, images[]
- [ ] `Review` — id, productId, userId, rating, title, body, images[], isVerifiedPurchase, isApproved, createdAt

**Orders & Cart**
- [ ] `Cart` — id, userId (nullable), sessionId, items[], updatedAt
- [ ] `AbandonedCart` — id, cartId, email, phone, reminderSentAt[], convertedAt
- [ ] `Order` — id, userId (nullable), guestEmail, guestPhone, items[], subtotal, discount, shippingCost, tax, total, currency(`INR`), paymentMethod, paymentStatus, fulfillmentStatus, notes, createdAt
- [ ] `OrderItem` — id, orderId, productId, variantId, name (snapshot), variant (snapshot), qty, unitPrice, totalPrice
- [ ] `Address` — id, userId, name, phone, line1, line2, city, state, pincode, country(`IN`), isDefault

**Payments & COD**
- [ ] `Payment` — id, orderId, razorpayOrderId, razorpayPaymentId, method (`CARD`/`UPI`/`NETBANKING`/`WALLET`/`COD`), amount, status, paidAt, failureReason
- [ ] `CODRecord` — id, orderId, expectedCollection, collectedAt, collectedBy, shiprocketCodId

**Discounts**
- [ ] `DiscountCode` — id, code, type (`PERCENT`/`FIXED`/`FREE_SHIPPING`/`BOGO`), value, conditions{}, usageLimit, usedCount, perCustomerLimit, startsAt, expiresAt, isActive
- [ ] `DiscountUsage` — id, discountCodeId, orderId, userId, usedAt

**Shipping**
- [ ] `ShippingZone` — id, name, states[], method, rates{}, freeThreshold, carrierId
- [ ] `Shipment` — id, orderId, shiprocketShipmentId, carrier, trackingNumber, trackingUrl, status, shippedAt, deliveredAt
- [ ] `ShipmentEvent` — id, shipmentId, status, location, timestamp (courier tracking history)

**Returns & Exchanges**
- [ ] `ReturnRequest` — id, orderId, userId, items[], reason, type (`RETURN`/`EXCHANGE`), status (`PENDING`/`APPROVED`/`REJECTED`/`RECEIVED`/`REFUNDED`/`EXCHANGED`), adminNote, createdAt
- [ ] `ReturnItem` — id, returnRequestId, orderItemId, qty, reason, condition
- [ ] `ExchangeItem` — id, returnRequestId, newVariantId, qty (replacement items for exchange)
- [ ] `Refund` — id, returnRequestId, orderId, amount, method (`ORIGINAL`/`STORE_CREDIT`), razorpayRefundId, status, processedAt

**Marketing & Notifications**
- [ ] `Wishlist` — id, userId, variantId, addedAt
- [ ] `BackInStockAlert` — id, variantId, email, phone, notifiedAt
- [ ] `AbandonedCartEmail` — id, abandonedCartId, sentAt, sequence (1/2/3)
- [ ] `Notification` — id, adminUserId (nullable), type, message, isRead, createdAt
- [ ] `AnnouncementBar` — id, text, bgColor, textColor, linkUrl, isActive, startsAt, expiresAt
- [ ] `AuditLog` — id, adminUserId, action, entityType, entityId, before{}, after{}, ip, createdAt

---

## Phase 2 — Storefront (Customer-Facing)

### Step 2.1 — Homepage
- [ ] Animated hero: gradient mesh + glass overlay text + CTA button
- [ ] Sitewide `<AnnouncementBar />` (pulled from DB, admin-managed)
- [ ] "New Arrivals" horizontal scroll carousel (GPU-accelerated)
- [ ] Category grid — Hijabs / Abayas / Namaz Scarfs / Accessories
- [ ] Featured collection banner (Eid Collection etc.)
- [ ] Bestsellers with product cards
- [ ] Social proof strip ("500+ happy customers", "4.9★ rating")
- [ ] Recently viewed products section (localStorage, shows after first browse)
- [ ] Newsletter signup with glass input (Resend list)
- [ ] Footer — links, social icons, payment logos (Razorpay, UPI, COD badge)
- [ ] Back-to-top button (appears after scroll)

### Step 2.2 — Product Listing Page (PLP)
- [ ] Filterable product grid:
  - Category, Color swatch, Size, Fabric/Material (chiffon, cotton, jersey, silk, georgette)
  - Price range slider, Sort (Newest / Price / Bestselling / Rating)
- [ ] Infinite scroll with skeleton loading
- [ ] Product card: image hover zoom, quick-add to cart, wishlist toggle, badge (Sale / New / Low Stock)
- [ ] Active filter chips with clear-all
- [ ] Results count + "No results" designed empty state
- [ ] Mobile: filter drawer (glass slide-in panel)
- [ ] Breadcrumb navigation (Home > Category > Subcategory)

### Step 2.3 — Product Detail Page (PDP)
- [ ] Image gallery (multiple images + video embed) with zoom + mobile swipe
- [ ] Variant selector (color swatches, size buttons, fabric)
- [ ] Stock status: In Stock / Low Stock (X left) / Out of Stock
- [ ] "Notify me when back in stock" button (email + phone) for OOS variants
- [ ] Add to Cart + quantity selector
- [ ] Add to Wishlist
- [ ] Size guide modal
- [ ] Product description tabs: Description / Care Instructions / Shipping & Returns
- [ ] Social proof: "X people bought this in last 24h", "Y people viewing now"
- [ ] Customer reviews section (star rating, images, verified badge, pagination)
- [ ] Related products carousel
- [ ] Recently viewed products
- [ ] Product FAQ accordion (per product, admin-editable)
- [ ] Share buttons (WhatsApp, Instagram, copy link)
- [ ] Breadcrumb navigation

### Step 2.4 — Shopping Cart
- [ ] Slide-out cart drawer (glass sidebar)
- [ ] Line items with image, name, variant, quantity stepper, remove
- [ ] Real-time subtotal + GST calculation
- [ ] Discount code field with validation feedback
- [ ] Estimated shipping display
- [ ] Gift wrapping option toggle (+₹50)
- [ ] Gift message text area (shows if gift wrapping selected)
- [ ] "Continue Shopping" + "Proceed to Checkout" CTAs
- [ ] Upsell/cross-sell suggestions inside cart
- [ ] Cart persistence: localStorage for guests, DB sync for logged-in users
- [ ] Abandoned cart tracking: after 30min inactivity, record email/phone if entered

### Step 2.5 — Checkout Flow (Multi-Step)
- [ ] **Step 1: Contact** — email, phone (guest or login prompt), WhatsApp opt-in checkbox
- [ ] **Step 2: Shipping** — Indian address form (line1, line2, city, state, pincode), address book for returning users, pincode-based serviceability check
- [ ] **Step 3: Delivery** — shipping method selection with Shiprocket rates, estimated delivery date
- [ ] **Step 4: Payment** — Razorpay checkout (UPI, cards, net banking, wallets) + Cash on Delivery option, order summary, discount code
- [ ] **Step 5: Confirmation** — order ID, summary, estimated delivery, "Track order" link, "Create account" prompt for guests
- [ ] Order notes field ("Leave at door", "Call before delivery")
- [ ] Gift message display if added in cart
- [ ] GST breakdown in order summary
- [ ] Glass progress stepper (Step 1/2/3/4)

### Step 2.6 — Payment Methods
- [ ] **Razorpay**: UPI (Google Pay, PhonePe, Paytm, BHIM), Debit/Credit cards, Net banking, Wallets
- [ ] **Cash on Delivery (COD)**: available for pincodes supported by Shiprocket; COD charge configurable (e.g., ₹50 extra)
- [ ] **COD confirmation**: after placing COD order, send WhatsApp + SMS confirmation
- [ ] Razorpay webhook handler: `payment.captured`, `payment.failed`, `refund.processed`
- [ ] Payment failure page: retry payment link, change payment method option
- [ ] Razorpay signature verification on all webhook events

### Step 2.7 — Authentication Pages

> All auth pages: split-layout (animated gradient mesh left panel, glass form card right). Mobile: full-screen glass card.

#### 2.7.1 — Register (`/register`)
- [ ] Fields: Full Name, Email, Phone (optional, for SMS/WhatsApp updates), Password, Confirm Password
- [ ] Password strength meter (weak → fair → strong → very strong), color-coded ≥ 4.5:1 contrast
- [ ] Show/hide password toggle
- [ ] Google OAuth "Continue with Google"
- [ ] Terms & Privacy checkbox
- [ ] On success → `/verify-email` + welcome email
- [ ] ARIA: `role="form"`, `aria-describedby` on errors, logical tab order

#### 2.7.2 — Login (`/login`)
- [ ] Fields: Email, Password; Show/hide toggle; Remember me
- [ ] "Forgot password?" link
- [ ] Google OAuth button + `── or ──` divider
- [ ] Loading spinner on submit (no layout shift)
- [ ] Error banner: glass amber border + `#FCA5A5` text ≥ 4.5:1
- [ ] Rate limit: 5 failed attempts → lockout timer display
- [ ] `autocomplete="email"` + `autocomplete="current-password"`

#### 2.7.3 — Email Verification (`/verify-email`)
- [ ] Confirmation state + animated mail icon
- [ ] Resend button with 60s countdown
- [ ] Auto-verify on token link click → redirect to account

#### 2.7.4 — Forgot Password (`/forgot-password`)
- [ ] Single email field; success transitions card to "Check inbox" state
- [ ] 60s resend cooldown; neutral error wording (no user enumeration)
- [ ] `aria-live="polite"` on state change

#### 2.7.5 — Reset Password (`/reset-password?token=xxx`)
- [ ] Token validated on load; expired/used → error + "Request new link" CTA
- [ ] New Password + Confirm fields, strength meter, show/hide toggles
- [ ] On success: auto-login → `/account` + toast

#### 2.7.6 — Change Password (`/account/security`)
- [ ] Current Password + New + Confirm fields
- [ ] Strength meter; wrong current password → inline error
- [ ] "Sign out all other devices" checkbox

#### 2.7.7 — Admin Login (`/admin/login`)
- [ ] Darker glass variant; email + password only (no OAuth)
- [ ] 5 attempts → 15-min lockout; 2FA TOTP stub
- [ ] No register link; redirect to `/admin/dashboard`

**Auth — Glassmorphic Visual Spec**

| Element | Value |
|---|---|
| Form card | `backdrop-filter: blur(24px)`, `background: rgba(255,255,255,0.07)`, `border: 1px solid rgba(255,255,255,0.14)`, `border-radius: 24px` |
| Input text | `#F0EEF8` — ≥ 7:1 on glass bg |
| Label text | `#E8E4F4` — ≥ 7:1 |
| Primary button | Gold gradient, `color: #0A0A14` — ≥ 7:1 |
| Link color | `#C9A96E` — ≥ 4.5:1 |
| Error text | `#FCA5A5` — ≥ 4.5:1 |
| Success text | `#86EFAC` — ≥ 4.5:1 |
| Focus ring | `2px solid #C9A96E`, `outline-offset: 3px` |

**Auth — Email Templates (Resend + React Email)**
- [ ] Verification email, Password reset email, Password changed confirmation, Welcome email
- [ ] All: plain-text fallback, unsubscribe footer, accessible HTML

### Step 2.8 — Abandoned Cart Recovery
- [ ] After 30min of cart inactivity (with email/phone captured at checkout step 1):
  - **Email 1** (1hr later): "You left something behind" — show cart items, direct link back
  - **Email 2** (24hr later): "Your cart is waiting" — add urgency if low stock
  - **Email 3** (48hr later): optional 5% discount code to convert
- [ ] WhatsApp message (MSG91): same 3-step sequence for users who opted in
- [ ] Mark as converted when order is placed; stop sequence immediately
- [ ] Admin view: abandoned carts list, recovery rate metric on dashboard

### Step 2.9 — Notifications (SMS + WhatsApp via MSG91)
- [ ] **Order Placed** — SMS + WhatsApp: order ID, total, estimated delivery
- [ ] **Payment Confirmed** — SMS for Razorpay payments
- [ ] **COD Order Confirmed** — WhatsApp: "Pay ₹X on delivery"
- [ ] **Order Shipped** — SMS + WhatsApp: tracking number + link
- [ ] **Out for Delivery** — WhatsApp message
- [ ] **Order Delivered** — WhatsApp: delivery confirmation + review request link
- [ ] **Return Approved** — WhatsApp: pickup scheduled
- [ ] **Refund Processed** — SMS + WhatsApp: refund amount + timeline
- [ ] **Back-in-Stock Alert** — SMS + WhatsApp for subscribed users
- [ ] **OTP** — for phone-based login / checkout verification
- [ ] Customer opt-in/out in account settings

### Step 2.10 — Returns & Exchanges (Customer Side)
- [ ] Returns portal at `/account/orders/[id]/return`
- [ ] Select items to return/exchange, quantity, reason from dropdown (Wrong size, Damaged, Not as described, Changed mind)
- [ ] Upload photos of item (required for damaged/defective claims)
- [ ] Choose resolution: Full refund / Exchange for different size / Store credit
- [ ] Submit → "Your request is under review" confirmation state
- [ ] Track return status in order detail page (Requested → Approved → Pickup Scheduled → Item Received → Refund/Exchange Processed)
- [ ] Return window enforced: 7-day policy from delivery date
- [ ] Exchange flow: select new variant, confirm; only proceed if new variant is in stock

### Step 2.11 — Customer Account
- [ ] Dashboard: recent orders, saved addresses, wishlist preview, notification preferences
- [ ] Order history with status timeline
- [ ] Order detail: items, payment breakdown, tracking, return/exchange button
- [ ] Address book (add/edit/delete/set default), Indian address format
- [ ] Profile settings: name, email, phone
- [ ] Security tab: change password, active sessions, sign out all devices
- [ ] Wishlist page (share wishlist via link)
- [ ] Notification preferences: email / SMS / WhatsApp toggles per event type
- [ ] GDPR/data rights: "Export my data" + "Delete my account" options

### Step 2.12 — Search
- [ ] Header search bar: instant suggestions (debounced 300ms), recent searches
- [ ] Full search results page with filters
- [ ] No-results state: suggestions + popular products

### Step 2.13 — Static Pages
- [ ] About Us, Contact Us (form), Shipping & Returns Policy, Privacy Policy, Terms of Service, FAQ, Size Guide
- [ ] All pages: glass card layouts, breadcrumbs, consistent typography

---

## Phase 3 — Admin Dashboard

> Route: `/admin` — separate auth session, role-based access

### Step 3.1 — Layout & Navigation
- [ ] Collapsible sidebar: icon + label nav, glass style
- [ ] Top bar: search, notification bell, admin profile menu
- [ ] Responsive: sidebar → bottom nav on mobile
- [ ] Roles: `SUPER_ADMIN` (all), `MANAGER` (no settings), `FULFILLMENT` (orders/shipping only)

### Step 3.2 — Dashboard Overview
- [ ] KPI cards: Total Revenue (₹), Orders Today, New Customers, Avg. Order Value, COD vs Prepaid split
- [ ] Revenue chart: 7d / 30d / 90d / custom range
- [ ] Orders by status doughnut chart
- [ ] Recent orders live table
- [ ] Top selling products table
- [ ] Low stock alerts panel (click → go to variant)
- [ ] Abandoned carts count + recovery rate widget
- [ ] Real-time notification feed (new order, payment failed, new return request)

### Step 3.3 — Announcement Bar Manager
- [ ] Create/edit/delete sitewide announcement bars
- [ ] Fields: text, background color, text color, optional link URL
- [ ] Schedule: start + end date/time
- [ ] Multiple bars: only one active at a time (toggle active)

### Step 3.4 — Order Management
- [ ] Orders list: filters (status, date, payment method, COD/prepaid, fulfillment)
- [ ] Status pipeline: Pending → Confirmed → Processing → Shipped → Delivered → Cancelled → Returned → Refunded
- [ ] Bulk actions: confirm, mark shipped, export CSV, print packing slips
- [ ] Order detail:
  - Customer info, items, variants, quantities
  - Payment status (Razorpay link for prepaid; COD collection status)
  - Shipping timeline with courier events
  - Assign tracking number + carrier (or push to Shiprocket)
  - Edit shipping address (before dispatch only)
  - Internal admin notes
  - Manual refund initiation
  - Print invoice PDF (GST-compliant)
  - Send manual WhatsApp/SMS from order page

### Step 3.5 — COD Management
- [ ] COD orders list (separate filter view)
- [ ] COD amount expected vs collected tracking
- [ ] Mark COD as collected when courier reports delivery
- [ ] COD reconciliation report (weekly/monthly collected amounts)
- [ ] Configure COD availability by pincode / order value threshold
- [ ] COD charge setting (flat fee per order, e.g., ₹50)

### Step 3.6 — Returns & Exchanges (Admin Side)
- [ ] Returns queue: all pending requests in one view
- [ ] Return request detail:
  - Order items, return reason, customer-uploaded photos
  - Approve / Reject with reason (customer notified via WhatsApp + email)
  - If approved: schedule pickup via Shiprocket reverse logistics OR manual instructions
  - Mark item as received (triggers refund/exchange flow)
- [ ] Refund processing:
  - Razorpay API refund (auto back to original payment method)
  - Store credit issuance (added to customer account)
  - COD orders: bank transfer refund (collect UPI/account details from customer)
- [ ] Exchange processing:
  - Confirm new variant availability
  - Create new shipment for exchange item
  - Link to original order
- [ ] Returns analytics: return rate, top return reasons, top returned products

### Step 3.7 — Product Management
- [ ] Product list: search, filter by category/status/collection, sort
- [ ] Add/Edit product:
  - Rich text description editor
  - Multiple image + video upload (drag-and-drop → Cloudinary)
  - Category + Collection assignment
  - Variant builder (color × size × fabric matrix)
  - Pricing: price, compare-at, cost price
  - SEO fields (meta title, description, slug)
  - Tags, fabric type, care instructions
  - Per-variant: SKU, stock, low-stock threshold, weight
  - Product FAQ section (add Q&A pairs)
  - Status: Active / Draft / Archived
- [ ] Duplicate product
- [ ] Bulk CSV import
- [ ] Bulk edit: price, stock, status

### Step 3.8 — Inventory Management
- [ ] Stock levels per variant, color-coded (green / amber / red)
- [ ] Low-stock alerts with configurable threshold per variant
- [ ] Stock adjustment log (reason: received, damaged, returned, manual correction)
- [ ] Back-in-stock: when variant restocked, auto-notify subscribers (SMS + WhatsApp + email)
- [ ] Export inventory report (CSV)

### Step 3.9 — Category & Collection Management
- [ ] CRUD categories + subcategories (Hijabs → Jersey Hijabs / Chiffon Hijabs)
- [ ] CRUD collections (Eid Collection 2025, Ramadan Essentials, etc.)
- [ ] Drag-and-drop sort order
- [ ] Category + banner images

### Step 3.10 — Customer Management
- [ ] Customer list: name, email, phone, orders, total spent, join date
- [ ] Customer detail: profile, full order history, addresses, return history, admin notes
- [ ] Export CSV

### Step 3.11 — Discount & Promotions
- [ ] Discount codes: %, fixed amount, free shipping, BOGO
- [ ] Conditions: min order value, specific products/categories, first-time customers
- [ ] Usage limits: total + per customer; validity dates
- [ ] Auto-generate bulk codes (influencer campaigns)
- [ ] Usage analytics per code (used / remaining / revenue generated)
- [ ] Flash sale banner management (links to Announcement Bar)

### Step 3.12 — Shipping Management
- [ ] Shiprocket integration: create shipment, get rates, track orders
- [ ] Shipping zones by state/pincode with flat/weight-based rates
- [ ] Free shipping threshold (e.g., free over ₹999)
- [ ] COD pincode availability (Shiprocket serviceability API)
- [ ] Manual tracking number entry (for non-Shiprocket shipments)
- [ ] Estimated delivery date display at checkout

### Step 3.13 — Analytics & Reports
- [ ] Sales report: revenue by day/week/month/year, COD vs prepaid split
- [ ] Product performance: views, add-to-cart rate, conversion
- [ ] Customer report: new vs returning, LTV
- [ ] Abandoned cart report: abandonment rate, recovery rate, recovered revenue
- [ ] Returns report: return rate, top reasons, refunded amounts
- [ ] Discount performance: usage, revenue impact
- [ ] Inventory value report
- [ ] GST report (output tax by month for filing)
- [ ] Export all as CSV

### Step 3.14 — Store Settings
- [ ] Store info: name, logo, favicon, address, GST number, contact, currency (`INR`)
- [ ] Email notification templates (order confirmed, shipped, delivered)
- [ ] Razorpay key configuration
- [ ] MSG91 API key + WhatsApp template management
- [ ] Tax settings: GST rates (5% / 12% / 18%) per category
- [ ] Return policy window setting (default 7 days)
- [ ] COD settings: charge, availability toggle, max order value for COD
- [ ] Maintenance mode toggle
- [ ] Admin user management: invite by email, assign roles
- [ ] Theme accent color picker

### Step 3.15 — Audit Log
- [ ] Every admin action logged: who, what, when, before/after values, IP address
- [ ] Searchable, filterable log viewer
- [ ] Retention: 90 days

---

## Phase 4 — Performance, SEO & Accessibility

### Step 4.1 — Performance
- [ ] Next.js Image with WebP/AVIF, Cloudinary CDN
- [ ] React Suspense + streaming for product pages
- [ ] ISR for product listings (revalidate every 60s)
- [ ] CSS containment on glass cards (`contain: layout style`)
- [ ] Bundle analyzer — no dead code
- [ ] Core Web Vitals target: LCP < 2.5s, CLS < 0.1, INP < 200ms

### Step 4.2 — SEO
- [ ] Dynamic meta tags per page (Next.js Metadata API)
- [ ] Open Graph + Twitter Card images
- [ ] JSON-LD: Product, BreadcrumbList, Organization
- [ ] XML sitemap auto-generation, robots.txt, canonical URLs

### Step 4.3 — Accessibility (Site-Wide)

**Contrast compliance (all pages)**
- [ ] Body text on glass panels: ≥ 7:1 (WCAG AAA target)
- [ ] UI component text: ≥ 4.5:1 (WCAG AA)
- [ ] Interactive UI borders/icons: ≥ 3:1
- [ ] Color never sole indicator — always paired with icon/text
- [ ] Automated `axe-core` checks in CI

**Interactive elements**
- [ ] Full keyboard navigation (Tab, Shift+Tab, Enter, Space, Arrow keys)
- [ ] `focus-visible` rings: `2px solid #C9A96E`, `outline-offset: 3px`
- [ ] Skip-to-content link on every page
- [ ] All icon-only buttons have `aria-label`
- [ ] Mobile touch targets ≥ 44×44px

**Semantic structure**
- [ ] Logical `<h1>`→`<h2>`→`<h3>` hierarchy everywhere
- [ ] Landmark regions: `<header>`, `<nav>`, `<main>`, `<footer>`
- [ ] Product lists as `<ul><li>` for screen reader list counts
- [ ] Admin tables: `<th scope>` + `<caption>`

**Dynamic content**
- [ ] Cart drawer: `aria-expanded`, `aria-controls`, focus management
- [ ] Toast: `role="status"` or `role="alert"` by urgency
- [ ] Filter changes: `aria-live="polite"` announces updated count
- [ ] Modals: `role="dialog"`, `aria-modal="true"`, Escape closes

**Glass-specific**
- [ ] `@supports` opaque fallback for browsers without `backdrop-filter`
- [ ] Text always on a minimum-opacity backing layer, never raw glass
- [ ] `prefers-contrast: more`: increase glass opacity + border weight

### Step 4.4 — Security
- [ ] CAPTCHA (Cloudflare Turnstile) on login, register, checkout, contact form
- [ ] API rate limiting middleware (Upstash Redis) on all routes
- [ ] Razorpay + Shiprocket webhook signature verification
- [ ] CSP, X-Frame-Options, HSTS, X-Content-Type-Options headers
- [ ] CSRF protection on all state-changing API routes
- [ ] Input sanitization + Prisma parameterized queries (SQL injection prevention)
- [ ] Automated daily DB backups (Supabase PITR)

### Step 4.5 — India Market (Compliance & Configuration)
- [ ] Currency: `INR` (₹) with Indian number formatting (₹1,99,999)
- [ ] GST: 5%/12%/18% rates per category, GST-compliant invoice PDF
- [ ] Indian address format: line1, line2, city, state (dropdown of 28 states + 8 UTs), pincode
- [ ] Pincode validation (6-digit Indian postcodes)
- [ ] Timezone: IST (Asia/Kolkata) for all order timestamps + admin reports
- [ ] Shiprocket: Indian courier network (BlueDart, Delhivery, DTDC, Ecom Express, India Post)
- [ ] COD availability check via Shiprocket pincode serviceability API
- [ ] Cookie consent banner (India PDPA / DPDP Act 2023 compliance)
- [ ] "Delete my account" + "Export my data" in account settings

### Step 4.6 — Mobile Experience
- [ ] Touch-optimized swipe: product gallery, cart drawer, filter panel
- [ ] Bottom navigation bar on mobile storefront
- [ ] PWA manifest + service worker
- [ ] Add to Home Screen support

---

## Phase 5 — Testing & Deployment

### Step 5.1 — Testing
- [ ] Unit tests: price calculations, GST logic, discount engine, COD charge calc
- [ ] Integration tests: checkout flow, Razorpay webhook handling, return workflow
- [ ] E2E (Playwright): homepage → PDP → cart → checkout (UPI + COD paths)
- [ ] Admin tests: product CRUD, order fulfillment, return approval flow
- [ ] Abandoned cart: verify email/WhatsApp sequences fire correctly

### Step 5.2 — Deployment
- [ ] Vercel production deployment
- [ ] Supabase production DB + PgBouncer connection pooling
- [ ] Cloudinary production account
- [ ] Razorpay live mode activation
- [ ] MSG91 live account + WhatsApp Business template approvals
- [ ] Shiprocket production account
- [ ] Custom domain (nayabicollection.com or .in)
- [ ] Sentry error monitoring
- [ ] Uptime monitoring (Better Uptime or UptimeRobot)

---

## Glassmorphic UI Specification

### Color Palette
| Token | Value | Usage |
|---|---|---|
| `--color-primary` | `#C9A96E` (warm gold) | CTAs, highlights, links |
| `--color-secondary` | `#8B5E9B` (amethyst) | Accents, badges |
| `--color-bg-dark` | `#0A0A14` (deep navy) | Page background |
| `--color-bg-mid` | `#12122A` | Section backgrounds |
| `--glass-bg` | `rgba(255,255,255,0.07)` | Card backgrounds |
| `--glass-border` | `rgba(255,255,255,0.14)` | Card borders |
| `--glass-blur` | `blur(24px)` | Backdrop filter |
| `--glow-gold` | `0 0 40px rgba(201,169,110,0.3)` | Hover glows |
| `--glow-purple` | `0 0 40px rgba(139,94,155,0.3)` | Accent glows |

### Typography
- Display: **Playfair Display** (headings)
- Body: **Inter** (all body text)
- Fallback stack: system-ui, sans-serif

### Animation Principles
- Transitions: `cubic-bezier(0.4, 0, 0.2, 1)`
- Duration: `150ms` micro / `300ms` standard / `500ms` page
- GPU-only: `transform` + `opacity` (no layout-triggering properties)
- Stagger: 50ms per item on list entrances

---

## Delivery Milestones

| Milestone | Deliverable |
|---|---|
| **M1** | Design system + DB schema + project scaffold + CI |
| **M2** | Homepage + PLP + PDP (with video, social proof, back-in-stock) |
| **M3** | Cart + Abandoned cart tracking + Checkout (Razorpay + COD) + Payment webhooks |
| **M4** | Full Auth suite (7 pages) + Email templates + SMS/WhatsApp setup (MSG91) |
| **M5** | Customer account + Returns portal (customer side) + Search + Static pages |
| **M6** | Admin: Dashboard + Announcement bar + Orders + COD management + Returns workflow |
| **M7** | Admin: Products + Inventory + Categories + Customers + Discounts + Shipping |
| **M8** | Admin: Analytics + GST reports + Store settings + Audit log |
| **M9** | Abandoned cart email/WhatsApp sequences + Back-in-stock notifications |
| **M10** | Performance + SEO + Accessibility + Security hardening + India compliance |
| **M11** | E2E testing + Deployment + Domain + Monitoring |

---

## Backlog (Post-Launch Additions)

These are genuine features but not needed for launch. Can be added later without breaking anything.

| Feature | Why Deferred |
|---|---|
| Loyalty / rewards points program | Needs separate engine; adds significant complexity |
| Referral program ("Give ₹100, Get ₹100") | Depends on user base first |
| Live chat / WhatsApp chat widget | Can use simple WhatsApp click-to-chat link at launch |
| Product comparison feature | Nice-to-have, low conversion impact at early stage |
| Blog / content marketing section | DB model exists; just needs frontend + admin editor |
| Affiliate / influencer portal | Discount code tracking covers this at launch |
| Google Analytics 4 + Meta Pixel + TikTok Pixel | Add via GTM post-launch without code changes |
| Google Shopping product feed | After catalogue is established |
| Urdu / Hindi language support | After English site is stable |
| 2FA for admin (full TOTP) | Stub exists; implement when team grows |
| Subscription / repeat orders | Not relevant for current product range |
| Multi-vendor / marketplace | Not in scope |
| Native mobile app (React Native) | PWA covers mobile at launch |
| ERP / accounting integration (Tally, Zoho) | Can export CSV reports at launch |
| Virtual try-on / AR | Future technology investment |
| Customer segmentation & email campaigns | Use Mailchimp integration post-launch |

---

*Ready for your final review and approval. Reply "approved" to begin building from M1.*
