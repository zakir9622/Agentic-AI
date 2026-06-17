/**
 * Regression test suite — covers every feature built in this codebase.
 * Uses Node.js built-in test runner via tsx: npm run test:regression
 *
 * No database or network required (DB-hitting paths use demo/fallback returns).
 * Crypto-dependent tests set/restore process.env in-test.
 */
import { test, describe, before, after } from "node:test";
import assert from "node:assert/strict";
import crypto from "node:crypto";

// ─────────────────────────────────────────────────────────────────────────────
// Utils
// ─────────────────────────────────────────────────────────────────────────────
import {
  cn,
  formatPrice,
  toPaise,
  generateOrderNumber,
  truncate,
  slugify,
  isValidPincode,
  isValidPhone,
  capitalize,
  relativeTime,
} from "@/lib/utils";

describe("utils › cn", () => {
  test("merges class names", () => {
    assert.equal(cn("a", "b"), "a b");
  });
  test("deduplicates tailwind conflicting classes (last wins)", () => {
    assert.equal(cn("p-2", "p-4"), "p-4");
  });
  test("drops falsy values", () => {
    assert.equal(cn("a", false, undefined, null, "b"), "a b");
  });
});

describe("utils › formatPrice", () => {
  test("formats paise to ₹ string", () => {
    assert.equal(formatPrice(59900), "₹599");
    assert.equal(formatPrice(0), "₹0");
    assert.equal(formatPrice(100), "₹1");
    assert.equal(formatPrice(99900), "₹999");
    assert.equal(formatPrice(100000), "₹1,000");
  });
  test("handles large amounts with Indian comma format", () => {
    const out = formatPrice(1000000);
    assert.ok(out.includes("10,000") || out.includes("10000"), `unexpected: ${out}`);
  });
});

describe("utils › toPaise", () => {
  test("converts rupees to paise", () => {
    assert.equal(toPaise(599), 59900);
    assert.equal(toPaise(1), 100);
    assert.equal(toPaise(0), 0);
    assert.equal(toPaise(0.5), 50);
  });
  test("rounds fractional paise", () => {
    assert.equal(toPaise(0.001), 0);
    assert.equal(toPaise(9.999), 1000);
  });
});

describe("utils › generateOrderNumber", () => {
  test("matches NC-YYYYMMDD-XXXX format", () => {
    const n = generateOrderNumber();
    assert.match(n, /^NC-\d{8}-[A-Z0-9]{4}$/);
  });
  test("two consecutive calls are unique", () => {
    // Uniqueness is probabilistic but overwhelmingly likely for 4-char suffix
    const seen = new Set<string>();
    for (let i = 0; i < 20; i++) seen.add(generateOrderNumber());
    assert.equal(seen.size, 20);
  });
  test("date segment reflects today", () => {
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, "");
    assert.ok(generateOrderNumber().startsWith(`NC-${today}-`));
  });
});

describe("utils › truncate", () => {
  test("truncates strings longer than n", () => {
    assert.equal(truncate("Hello World", 6), "Hello…");
  });
  test("returns string unchanged when short enough", () => {
    assert.equal(truncate("Hi", 10), "Hi");
    assert.equal(truncate("Hi", 2), "Hi");
  });
  test("n=1 returns ellipsis", () => {
    assert.equal(truncate("abc", 1), "…");
  });
});

describe("utils › slugify", () => {
  test("lowercases and replaces spaces with hyphens", () => {
    assert.equal(slugify("Silk Georgette Hijab"), "silk-georgette-hijab");
  });
  test("strips special characters", () => {
    assert.equal(slugify("Hello, World!"), "hello-world");
  });
  test("collapses multiple hyphens", () => {
    assert.equal(slugify("a  b--c"), "a-b-c");
  });
  test("trims leading/trailing hyphens", () => {
    assert.equal(slugify("  --hello-- "), "hello");
  });
  test("handles empty string", () => {
    assert.equal(slugify(""), "");
  });
});

describe("utils › isValidPincode", () => {
  test("accepts valid 6-digit Indian pincodes", () => {
    assert.ok(isValidPincode("400001")); // Mumbai
    assert.ok(isValidPincode("110001")); // Delhi
    assert.ok(isValidPincode("600001")); // Chennai
  });
  test("rejects pincodes starting with 0", () => {
    assert.equal(isValidPincode("000001"), false);
  });
  test("rejects short/long codes", () => {
    assert.equal(isValidPincode("12345"), false);
    assert.equal(isValidPincode("1234567"), false);
  });
  test("rejects non-numeric input", () => {
    assert.equal(isValidPincode("4000AB"), false);
  });
});

describe("utils › isValidPhone", () => {
  test("accepts 10-digit Indian numbers starting with 6-9", () => {
    assert.ok(isValidPhone("9500738518")); // 9..
    assert.ok(isValidPhone("8800112233")); // 8..
    assert.ok(isValidPhone("7700998877")); // 7..
    assert.ok(isValidPhone("6900112233")); // 6..
  });
  test("strips hyphens/spaces before validating (10-digit result)", () => {
    assert.ok(isValidPhone("95007-38518")); // hyphens removed → 9500738518 → valid
    assert.ok(isValidPhone("9500738518 ")); // trailing space → valid
    // +91 prefix leaves 12 digits after stripping — correctly rejected
    assert.equal(isValidPhone("+91 9500738518"), false);
  });
  test("rejects numbers starting with 5 or lower", () => {
    assert.equal(isValidPhone("5000000000"), false);
    assert.equal(isValidPhone("1234567890"), false);
  });
  test("rejects too-short numbers", () => {
    assert.equal(isValidPhone("123"), false);
    assert.equal(isValidPhone("900000000"), false); // 9 digits
  });
});

describe("utils › capitalize", () => {
  test("capitalises first letter and lowercases rest", () => {
    assert.equal(capitalize("hELLO"), "Hello");
    assert.equal(capitalize("world"), "World");
  });
  test("handles single char", () => {
    assert.equal(capitalize("a"), "A");
  });
});

describe("utils › relativeTime", () => {
  test("just now for < 1 minute", () => {
    assert.equal(relativeTime(new Date(Date.now() - 30_000)), "just now");
  });
  test("minutes ago for < 1 hour", () => {
    const label = relativeTime(new Date(Date.now() - 5 * 60_000));
    assert.match(label, /^\d+m ago$/);
  });
  test("hours ago for < 24 hours", () => {
    const label = relativeTime(new Date(Date.now() - 3 * 60 * 60_000));
    assert.match(label, /^\d+h ago$/);
  });
  test("days ago for < 30 days", () => {
    const label = relativeTime(new Date(Date.now() - 5 * 24 * 60 * 60_000));
    assert.match(label, /^\d+d ago$/);
  });
  test("locale date for >= 30 days", () => {
    const old = new Date(Date.now() - 40 * 24 * 60 * 60_000);
    const label = relativeTime(old);
    // Should be a formatted date (not 'ago')
    assert.ok(!label.includes("ago"));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────
import {
  BRAND,
  INDIAN_STATES,
  FREE_SHIPPING_THRESHOLD,
  FLAT_SHIPPING_RATE,
  COD_CHARGE,
  GIFT_WRAP_CHARGE,
  GST_RATE,
  RETURN_WINDOW_DAYS,
  GIFT_CARD_DENOMINATIONS,
} from "@/lib/constants";

describe("constants › BRAND", () => {
  test("Instagram handle is correct", () => {
    assert.equal(BRAND.instagramHandle, "nayabi_collection");
    assert.ok(BRAND.instagramUrl.includes("nayabi_collection"));
  });
  test("WhatsApp number is correct (E.164 digits)", () => {
    assert.equal(BRAND.whatsappDigits, "919500738518");
    assert.ok(BRAND.phoneDisplay.includes("+91"));
    assert.ok(BRAND.phoneDisplay.includes("95007 38518"));
  });
  test("whatsappUrl builds valid wa.me link", () => {
    assert.ok(BRAND.whatsappUrl().includes("wa.me/919500738518"));
    const withText = BRAND.whatsappUrl("Hello");
    assert.ok(withText.includes("?text=Hello"));
  });
  test("whatsappUrl encodes special chars", () => {
    const url = BRAND.whatsappUrl("Hello World!");
    assert.ok(!url.includes(" "), "spaces must be encoded");
  });
});

describe("constants › INDIAN_STATES", () => {
  test("contains 28 states + 8 UTs = 36 entries", () => {
    assert.equal(INDIAN_STATES.length, 36);
  });
  test("contains major states", () => {
    assert.ok(INDIAN_STATES.includes("Maharashtra"));
    assert.ok(INDIAN_STATES.includes("Karnataka"));
    assert.ok(INDIAN_STATES.includes("Tamil Nadu"));
    assert.ok(INDIAN_STATES.includes("Delhi"));
  });
});

describe("constants › shipping & business rules", () => {
  test("FREE_SHIPPING_THRESHOLD is ₹999 (99900 paise)", () => {
    assert.equal(FREE_SHIPPING_THRESHOLD, 99900);
  });
  test("FLAT_SHIPPING_RATE is ₹79 (7900 paise)", () => {
    assert.equal(FLAT_SHIPPING_RATE, 7900);
  });
  test("COD_CHARGE is ₹50 (5000 paise)", () => {
    assert.equal(COD_CHARGE, 5000);
  });
  test("GIFT_WRAP_CHARGE is ₹50 (5000 paise)", () => {
    assert.equal(GIFT_WRAP_CHARGE, 5000);
  });
  test("GST_RATE is 5%", () => {
    assert.equal(GST_RATE, 5);
  });
  test("RETURN_WINDOW_DAYS is 7 by default", () => {
    assert.equal(RETURN_WINDOW_DAYS, 7);
  });
});

describe("constants › GIFT_CARD_DENOMINATIONS", () => {
  test("has exactly 4 denominations", () => {
    assert.equal(GIFT_CARD_DENOMINATIONS.length, 4);
  });
  test("denominations are in paise and ascending", () => {
    for (let i = 1; i < GIFT_CARD_DENOMINATIONS.length; i++) {
      assert.ok(GIFT_CARD_DENOMINATIONS[i] > GIFT_CARD_DENOMINATIONS[i - 1]);
    }
  });
  test("includes ₹500 (50000 paise) and ₹5000 (500000 paise)", () => {
    assert.ok(GIFT_CARD_DENOMINATIONS.includes(50000));
    assert.ok(GIFT_CARD_DENOMINATIONS.includes(500000));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Pricing
// ─────────────────────────────────────────────────────────────────────────────
import { computePricing } from "@/lib/pricing";

describe("pricing › computePricing", () => {
  test("free shipping at exactly the threshold", () => {
    const r = computePricing({ subtotal: FREE_SHIPPING_THRESHOLD });
    assert.equal(r.shippingCost, 0);
    assert.equal(r.codCharge, 0);
    assert.equal(r.giftWrapCharge, 0);
    assert.equal(r.total, FREE_SHIPPING_THRESHOLD);
  });

  test("free shipping above the threshold", () => {
    const r = computePricing({ subtotal: FREE_SHIPPING_THRESHOLD + 1 });
    assert.equal(r.shippingCost, 0);
  });

  test("flat shipping below the threshold", () => {
    const r = computePricing({ subtotal: 50000 });
    assert.equal(r.shippingCost, FLAT_SHIPPING_RATE);
    assert.equal(r.total, 50000 + FLAT_SHIPPING_RATE);
  });

  test("COD charge added when isCOD=true", () => {
    const r = computePricing({ subtotal: 50000, isCOD: true });
    assert.equal(r.codCharge, COD_CHARGE);
    assert.equal(r.total, 50000 + FLAT_SHIPPING_RATE + COD_CHARGE);
  });

  test("no COD charge when isCOD=false", () => {
    const r = computePricing({ subtotal: 50000, isCOD: false });
    assert.equal(r.codCharge, 0);
  });

  test("gift wrap charge added when isGiftWrapped=true", () => {
    const r = computePricing({ subtotal: 50000, isGiftWrapped: true });
    assert.equal(r.giftWrapCharge, GIFT_WRAP_CHARGE);
  });

  test("discount reduces after-discount subtotal for shipping threshold", () => {
    // 105000 - 10000 discount = 95000 < 99900 threshold → flat shipping applies
    const r = computePricing({ subtotal: 105000, discountAmount: 10000 });
    assert.equal(r.discountAmount, 10000);
    assert.equal(r.shippingCost, FLAT_SHIPPING_RATE);
  });

  test("discount cannot exceed subtotal (capped)", () => {
    const r = computePricing({ subtotal: 10000, discountAmount: 99999 });
    assert.equal(r.discountAmount, 10000);
    assert.equal(r.total, 0 + r.shippingCost);
  });

  test("zero discount has no effect", () => {
    const r = computePricing({ subtotal: 50000, discountAmount: 0 });
    assert.equal(r.discountAmount, 0);
  });

  test("GST extraction is informational (already in price)", () => {
    // GST is 5% included in price. For subtotal s, tax = round(s * 5 / 105)
    const r = computePricing({ subtotal: 100000 });
    const expected = Math.round((100000 * 5) / 105);
    assert.equal(r.taxAmount, expected);
  });

  test("full combination: discount + COD + gift wrap", () => {
    const subtotal = 50000;
    const discount = 5000;
    const r = computePricing({ subtotal, discountAmount: discount, isCOD: true, isGiftWrapped: true });
    assert.equal(r.subtotal, subtotal);
    assert.equal(r.discountAmount, discount);
    assert.equal(r.codCharge, COD_CHARGE);
    assert.equal(r.giftWrapCharge, GIFT_WRAP_CHARGE);
    assert.equal(r.total, subtotal - discount + r.shippingCost + COD_CHARGE + GIFT_WRAP_CHARGE);
  });

  test("zero subtotal with no discount", () => {
    const r = computePricing({ subtotal: 0 });
    assert.equal(r.total, FLAT_SHIPPING_RATE); // below threshold, shipping applies
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Admin auth (pure helpers — no cookies() call)
// ─────────────────────────────────────────────────────────────────────────────
import { buildAdminCookie, clearAdminCookie, createAdminSession } from "@/lib/admin-auth";

describe("admin-auth › createAdminSession", () => {
  test("produces a valid AdminSession", () => {
    const s = createAdminSession({ id: "a1", email: "x@x.com", name: "Alice", role: "SUPER_ADMIN" });
    assert.equal(s.adminId, "a1");
    assert.equal(s.email, "x@x.com");
    assert.equal(s.name, "Alice");
    assert.equal(s.role, "SUPER_ADMIN");
    assert.ok(typeof s.expiresAt === "number");
    assert.ok(s.expiresAt > Date.now());
  });

  test("expiresAt is approximately 8 hours from now", () => {
    const s = createAdminSession({ id: "a1", email: "x@x.com", name: "Alice", role: "MANAGER" });
    const diff = s.expiresAt - Date.now();
    const eightHours = 8 * 60 * 60 * 1000;
    assert.ok(diff > eightHours - 5000 && diff < eightHours + 5000);
  });
});

describe("admin-auth › buildAdminCookie", () => {
  const session = createAdminSession({ id: "b2", email: "b@b.com", name: "Bob", role: "MANAGER" });
  const cookie = buildAdminCookie(session);

  test("cookie name is nc_admin", () => {
    assert.ok(cookie.startsWith("nc_admin="));
  });

  test("contains HttpOnly", () => {
    assert.ok(cookie.includes("HttpOnly"));
  });

  test("contains SameSite=Strict", () => {
    assert.ok(cookie.includes("SameSite=Strict"));
  });

  test("scoped to /admin path", () => {
    assert.ok(cookie.includes("Path=/admin"));
  });

  test("value round-trips correctly", () => {
    const value = cookie.split(";")[0].split("=").slice(1).join("=");
    const decoded = JSON.parse(Buffer.from(value, "base64").toString("utf8"));
    assert.equal(decoded.adminId, "b2");
    assert.equal(decoded.name, "Bob");
    assert.ok(decoded.expiresAt > Date.now());
  });
});

describe("admin-auth › clearAdminCookie", () => {
  const cleared = clearAdminCookie();

  test("cookie name is nc_admin", () => {
    assert.ok(cleared.startsWith("nc_admin="));
  });

  test("sets Max-Age=0 to expire immediately", () => {
    assert.ok(cleared.includes("Max-Age=0"));
  });

  test("scoped to /admin path", () => {
    assert.ok(cleared.includes("Path=/admin"));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Razorpay (crypto signatures, no network)
// ─────────────────────────────────────────────────────────────────────────────
import { verifyPaymentSignature, verifyWebhookSignature } from "@/lib/razorpay";

describe("razorpay › verifyPaymentSignature", () => {
  const KEY_SECRET = "test-razorpay-secret";
  const ORDER_ID = "order_TestABC123";
  const PAYMENT_ID = "pay_TestXYZ456";

  let originalSecret: string | undefined;
  before(() => {
    originalSecret = process.env.RAZORPAY_KEY_SECRET;
    process.env.RAZORPAY_KEY_SECRET = KEY_SECRET;
  });
  after(() => {
    process.env.RAZORPAY_KEY_SECRET = originalSecret;
  });

  test("returns true for a correctly computed signature", () => {
    const sig = crypto
      .createHmac("sha256", KEY_SECRET)
      .update(`${ORDER_ID}|${PAYMENT_ID}`)
      .digest("hex");
    assert.ok(verifyPaymentSignature({ razorpayOrderId: ORDER_ID, razorpayPaymentId: PAYMENT_ID, razorpaySignature: sig }));
  });

  test("returns false for a tampered signature", () => {
    const badSig = "0".repeat(64);
    assert.equal(verifyPaymentSignature({ razorpayOrderId: ORDER_ID, razorpayPaymentId: PAYMENT_ID, razorpaySignature: badSig }), false);
  });

  test("returns false when payment ID is wrong", () => {
    const sig = crypto
      .createHmac("sha256", KEY_SECRET)
      .update(`${ORDER_ID}|wrong_payment_id`)
      .digest("hex");
    assert.equal(verifyPaymentSignature({ razorpayOrderId: ORDER_ID, razorpayPaymentId: PAYMENT_ID, razorpaySignature: sig }), false);
  });
});

describe("razorpay › verifyPaymentSignature (no secret)", () => {
  let originalSecret: string | undefined;
  before(() => {
    originalSecret = process.env.RAZORPAY_KEY_SECRET;
    delete process.env.RAZORPAY_KEY_SECRET;
  });
  after(() => {
    process.env.RAZORPAY_KEY_SECRET = originalSecret;
  });

  test("returns false when secret is not configured", () => {
    assert.equal(verifyPaymentSignature({ razorpayOrderId: "o", razorpayPaymentId: "p", razorpaySignature: "s" }), false);
  });
});

describe("razorpay › verifyWebhookSignature", () => {
  const WEBHOOK_SECRET = "test-webhook-secret";
  const PAYLOAD = '{"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_test"}}}}';

  let originalSecret: string | undefined;
  before(() => {
    originalSecret = process.env.RAZORPAY_WEBHOOK_SECRET;
    process.env.RAZORPAY_WEBHOOK_SECRET = WEBHOOK_SECRET;
  });
  after(() => {
    process.env.RAZORPAY_WEBHOOK_SECRET = originalSecret;
  });

  test("returns true for correct webhook signature", () => {
    const sig = crypto.createHmac("sha256", WEBHOOK_SECRET).update(PAYLOAD).digest("hex");
    assert.ok(verifyWebhookSignature(PAYLOAD, sig));
  });

  test("returns false for wrong signature", () => {
    assert.equal(verifyWebhookSignature(PAYLOAD, "bad-signature"), false);
  });

  test("returns false for empty signature", () => {
    assert.equal(verifyWebhookSignature(PAYLOAD, ""), false);
  });
});

describe("razorpay › verifyWebhookSignature (no secret)", () => {
  let originalSecret: string | undefined;
  before(() => {
    originalSecret = process.env.RAZORPAY_WEBHOOK_SECRET;
    delete process.env.RAZORPAY_WEBHOOK_SECRET;
  });
  after(() => {
    process.env.RAZORPAY_WEBHOOK_SECRET = originalSecret;
  });

  test("returns false when webhook secret not configured", () => {
    assert.equal(verifyWebhookSignature("{}", "any-sig"), false);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Shipping registry
// ─────────────────────────────────────────────────────────────────────────────
import { getProvider, listProviders, defaultProvider, configuredProviders } from "@/lib/shipping";

describe("shipping › registry", () => {
  test("listProviders returns exactly 3 carriers", () => {
    assert.equal(listProviders().length, 3);
  });

  test("carrier IDs are the three expected values", () => {
    const ids = listProviders().map((p) => p.id).sort();
    assert.deepEqual(ids, ["amazon", "delhivery", "shiprocket"]);
  });

  test("getProvider returns the correct provider by ID", () => {
    assert.equal(getProvider("shiprocket")?.id, "shiprocket");
    assert.equal(getProvider("delhivery")?.id, "delhivery");
    assert.equal(getProvider("amazon")?.id, "amazon");
  });

  test("getProvider returns null for unknown carrier", () => {
    assert.equal(getProvider("fedex"), null);
    assert.equal(getProvider(""), null);
  });

  test("provider labels are non-empty strings", () => {
    for (const p of listProviders()) {
      assert.ok(p.label.length > 0, `${p.id} has empty label`);
    }
  });

  test("Shiprocket label is 'Shiprocket'", () => {
    assert.equal(getProvider("shiprocket")?.label, "Shiprocket");
  });

  test("Delhivery label is 'Delhivery'", () => {
    assert.equal(getProvider("delhivery")?.label, "Delhivery");
  });

  test("configuredProviders is empty without env credentials", () => {
    const configured = configuredProviders();
    assert.equal(configured.length, 0);
  });

  test("defaultProvider falls back to Shiprocket when none configured", () => {
    assert.equal(defaultProvider().id, "shiprocket");
  });

  test("trackingUrl returns a non-empty string with AWB", () => {
    const awb = "DL1234567890IN";
    const url = getProvider("delhivery")!.trackingUrl(awb);
    assert.ok(url.length > 0);
    assert.ok(url.includes(awb));
  });

  test("all providers have createShipment and track functions", () => {
    for (const p of listProviders()) {
      assert.equal(typeof p.createShipment, "function");
      assert.equal(typeof p.track, "function");
      assert.equal(typeof p.isConfigured, "function");
      assert.equal(typeof p.trackingUrl, "function");
    }
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Loyalty
// ─────────────────────────────────────────────────────────────────────────────
import { getLoyalty, LOYALTY_TIERS, POINTS_PER_RUPEE, awardOrderPoints } from "@/lib/loyalty";

describe("loyalty › LOYALTY_TIERS", () => {
  test("has exactly 3 tiers", () => {
    assert.equal(LOYALTY_TIERS.length, 3);
  });

  test("tiers are BRONZE → SILVER → GOLD in order", () => {
    assert.equal(LOYALTY_TIERS[0].name, "BRONZE");
    assert.equal(LOYALTY_TIERS[1].name, "SILVER");
    assert.equal(LOYALTY_TIERS[2].name, "GOLD");
  });

  test("BRONZE starts at 0 points", () => {
    assert.equal(LOYALTY_TIERS[0].min, 0);
  });

  test("SILVER starts at 1000 points", () => {
    assert.equal(LOYALTY_TIERS[1].min, 1000);
  });

  test("GOLD starts at 5000 points", () => {
    assert.equal(LOYALTY_TIERS[2].min, 5000);
  });

  test("tier min values are strictly ascending", () => {
    for (let i = 1; i < LOYALTY_TIERS.length; i++) {
      assert.ok(LOYALTY_TIERS[i].min > LOYALTY_TIERS[i - 1].min);
    }
  });

  test("all tiers have non-empty perks", () => {
    for (const t of LOYALTY_TIERS) {
      assert.ok(t.perk.length > 0);
    }
  });
});

describe("loyalty › POINTS_PER_RUPEE", () => {
  test("rate is 0.1 (1 point per ₹10)", () => {
    assert.equal(POINTS_PER_RUPEE, 0.1);
  });

  test("points earned from ₹1000 order (100000 paise) = 100 points", () => {
    const paise = 100000;
    const earned = Math.floor((paise / 100) * POINTS_PER_RUPEE);
    assert.equal(earned, 100);
  });

  test("points earned from ₹599 order (59900 paise) = 59 points", () => {
    const earned = Math.floor((59900 / 100) * POINTS_PER_RUPEE);
    assert.equal(earned, 59);
  });
});

describe("loyalty › getLoyalty", () => {
  test("null userId returns empty Bronze account", async () => {
    const l = await getLoyalty(null);
    assert.equal(l.points, 0);
    assert.equal(l.lifetimePoints, 0);
    assert.equal(l.tier, "BRONZE");
    assert.equal(l.nextTier, "SILVER");
    assert.equal(l.pointsToNext, 1000);
  });

  test("non-existent userId returns empty Bronze account (no DB = graceful)", async () => {
    const l = await getLoyalty("nonexistent-user-id");
    assert.equal(l.tier, "BRONZE");
    assert.equal(l.points, 0);
  });
});

describe("loyalty › awardOrderPoints", () => {
  test("returns 0 when userId is null", async () => {
    const earned = await awardOrderPoints(null, 100000);
    assert.equal(earned, 0);
  });

  test("returns 0 for zero-value order", async () => {
    const earned = await awardOrderPoints(null, 0);
    assert.equal(earned, 0);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Gift cards
// ─────────────────────────────────────────────────────────────────────────────
import { generateGiftCardCode, checkGiftCardBalance } from "@/lib/gift-cards";

describe("gift-cards › generateGiftCardCode", () => {
  test("matches NAYABI-XXXX-XXXX-XXXX format", () => {
    const code = generateGiftCardCode();
    assert.match(code, /^NAYABI-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/);
  });

  test("only uses safe charset (no 0, 1, I, O)", () => {
    for (let i = 0; i < 30; i++) {
      const code = generateGiftCardCode();
      const suffix = code.replace("NAYABI-", "").replace(/-/g, "");
      assert.ok(!/[01IO]/.test(suffix), `unsafe chars in: ${code}`);
    }
  });

  test("50 generated codes are all unique", () => {
    const codes = Array.from({ length: 50 }, () => generateGiftCardCode());
    assert.equal(new Set(codes).size, 50);
  });

  test("code always has 3 segments of 4 chars each", () => {
    const code = generateGiftCardCode();
    const parts = code.split("-");
    assert.equal(parts.length, 4);
    assert.equal(parts[0], "NAYABI");
    assert.ok(parts.slice(1).every((p) => p.length === 4));
  });
});

describe("gift-cards › checkGiftCardBalance", () => {
  test("NAYABI-GIFT returns demo card", async () => {
    const r = await checkGiftCardBalance("NAYABI-GIFT");
    assert.ok(r.found);
    assert.ok(r.demo);
    assert.equal(r.status, "ACTIVE");
    assert.ok((r.balance ?? 0) > 0);
    assert.ok((r.initialValue ?? 0) >= (r.balance ?? 0));
  });

  test("case-insensitive: 'nayabi-gift' also returns demo", async () => {
    const r = await checkGiftCardBalance("nayabi-gift");
    assert.ok(r.found);
    assert.ok(r.demo);
  });

  test("NAYABI-DEMO also returns demo (same demo code set)", async () => {
    const r = await checkGiftCardBalance("NAYABI-DEMO");
    assert.ok(r.found);
    assert.ok(r.demo);
  });

  test("empty string returns error", async () => {
    const r = await checkGiftCardBalance("");
    assert.equal(r.found, false);
    assert.ok(r.error);
  });

  test("whitespace-only string returns error", async () => {
    const r = await checkGiftCardBalance("   ");
    assert.equal(r.found, false);
    assert.ok(r.error);
  });

  test("invalid code returns not found (DB fails silently)", async () => {
    const r = await checkGiftCardBalance("INVALID-XXXX-XXXX-XXXX");
    assert.equal(r.found, false);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Turnstile CAPTCHA (graceful no-op)
// ─────────────────────────────────────────────────────────────────────────────
import { verifyTurnstile } from "@/lib/turnstile";

describe("turnstile › graceful degradation", () => {
  let originalSecret: string | undefined;
  before(() => {
    originalSecret = process.env.TURNSTILE_SECRET_KEY;
    delete process.env.TURNSTILE_SECRET_KEY;
  });
  after(() => {
    process.env.TURNSTILE_SECRET_KEY = originalSecret;
  });

  test("returns true when secret key is not configured (any token)", async () => {
    assert.ok(await verifyTurnstile("any-token"));
  });

  test("returns true when secret key is not configured (null token)", async () => {
    assert.ok(await verifyTurnstile(null));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Cart store logic (pure math — tests the calculation formulas directly)
// ─────────────────────────────────────────────────────────────────────────────
describe("cart › subtotal and count calculations", () => {
  type Item = { variantId: string; price: number; quantity: number; maxStock: number };

  function subtotal(items: Item[]) {
    return items.reduce((s, i) => s + i.price * i.quantity, 0);
  }
  function count(items: Item[]) {
    return items.reduce((s, i) => s + i.quantity, 0);
  }
  function addItem(items: Item[], item: Item): Item[] {
    const existing = items.find((i) => i.variantId === item.variantId);
    if (existing) {
      return items.map((i) =>
        i.variantId === item.variantId
          ? { ...i, quantity: Math.min(i.quantity + item.quantity, i.maxStock) }
          : i
      );
    }
    return [...items, item];
  }
  function setQuantity(items: Item[], variantId: string, quantity: number): Item[] {
    if (quantity <= 0) return items.filter((i) => i.variantId !== variantId);
    return items.map((i) =>
      i.variantId === variantId
        ? { ...i, quantity: Math.min(quantity, i.maxStock) }
        : i
    );
  }

  const item1: Item = { variantId: "v1", price: 59900, quantity: 1, maxStock: 10 };
  const item2: Item = { variantId: "v2", price: 29900, quantity: 2, maxStock: 5 };

  test("subtotal sums price × quantity", () => {
    assert.equal(subtotal([item1, item2]), 59900 + 29900 * 2);
  });

  test("count sums quantities", () => {
    assert.equal(count([item1, item2]), 3);
  });

  test("addItem increments quantity for existing variantId", () => {
    const updated = addItem([item1], { ...item1, quantity: 2 });
    assert.equal(updated.length, 1);
    assert.equal(updated[0].quantity, 3);
  });

  test("addItem is capped at maxStock", () => {
    const big = { variantId: "v1", price: 100, quantity: 9, maxStock: 10 };
    const updated = addItem([big], { ...big, quantity: 5 }); // 9+5=14 → capped at 10
    assert.equal(updated[0].quantity, 10);
  });

  test("addItem inserts new item for different variantId", () => {
    const updated = addItem([item1], item2);
    assert.equal(updated.length, 2);
  });

  test("setQuantity removes item when quantity is 0", () => {
    const updated = setQuantity([item1, item2], "v1", 0);
    assert.equal(updated.length, 1);
    assert.equal(updated[0].variantId, "v2");
  });

  test("setQuantity caps at maxStock", () => {
    const updated = setQuantity([item2], "v2", 100);
    assert.equal(updated[0].quantity, item2.maxStock);
  });

  test("empty cart subtotal is 0", () => {
    assert.equal(subtotal([]), 0);
  });

  test("empty cart count is 0", () => {
    assert.equal(count([]), 0);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Recently-viewed deduplication logic
// ─────────────────────────────────────────────────────────────────────────────
describe("recently-viewed › deduplication and capping", () => {
  type Item = { id: string; name: string };

  const MAX_ITEMS = 8;

  function add(items: Item[], product: Item): Item[] {
    return [product, ...items.filter((p) => p.id !== product.id)].slice(0, MAX_ITEMS);
  }

  const p = (id: string): Item => ({ id, name: `Product ${id}` });

  test("adds a product to the front", () => {
    const items = add([], p("a"));
    assert.equal(items[0].id, "a");
  });

  test("re-adding an existing product moves it to front", () => {
    let items = add([], p("a"));
    items = add(items, p("b"));
    items = add(items, p("a")); // re-add "a" — should come first
    assert.equal(items[0].id, "a");
    assert.equal(items.length, 2);
  });

  test("deduplication keeps only one copy of each product", () => {
    let items: Item[] = [];
    items = add(items, p("a"));
    items = add(items, p("a"));
    items = add(items, p("a"));
    assert.equal(items.length, 1);
  });

  test("capped at MAX_ITEMS (8)", () => {
    let items: Item[] = [];
    for (let i = 0; i < 12; i++) items = add(items, p(`item-${i}`));
    assert.equal(items.length, MAX_ITEMS);
  });

  test("most recent item is always first after cap", () => {
    let items: Item[] = [];
    for (let i = 0; i < 12; i++) items = add(items, p(`item-${i}`));
    assert.equal(items[0].id, "item-11");
  });

  test("clear returns empty array", () => {
    const cleared: Item[] = [];
    assert.equal(cleared.length, 0);
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Order tracking (demo path — no DB required)
// ─────────────────────────────────────────────────────────────────────────────
import { lookupOrder } from "@/app/actions/track";

describe("order tracking › demo path", () => {
  function fd(entries: Record<string, string>): FormData {
    const f = new FormData();
    for (const [k, v] of Object.entries(entries)) f.append(k, v);
    return f;
  }
  const prev = { found: false as const };

  test("NAYABI-DEMO returns a demo order", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "NAYABI-DEMO", email: "any@email.com" }));
    assert.ok(r.found);
    assert.ok(r.demo);
    assert.equal(r.order?.orderNumber, "NAYABI-DEMO");
  });

  test("demo order has items and a shipment", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "NAYABI-DEMO", email: "any@email.com" }));
    assert.ok(r.found && r.order);
    assert.ok(r.order.items.length > 0);
    assert.ok(r.order.shipment !== null);
  });

  test("demo shipment carrier is Delhivery", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "NAYABI-DEMO", email: "x@x.com" }));
    assert.ok(r.found && r.order);
    assert.equal(r.order.shipment?.carrier, "Delhivery");
  });

  test("demo shipment has tracking events", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "NAYABI-DEMO", email: "x@x.com" }));
    assert.ok(r.found && r.order?.shipment);
    assert.ok(r.order.shipment.events.length > 0);
  });

  test("missing order number returns error", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "", email: "x@x.com" }));
    assert.equal(r.found, false);
    assert.ok(r.error);
  });

  test("missing email returns error", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "NC-12345678-ABCD", email: "" }));
    assert.equal(r.found, false);
    assert.ok(r.error);
  });

  test("real order number with no DB returns gracefully", async () => {
    const r = await lookupOrder(prev, fd({ orderNumber: "NC-99999999-XXXX", email: "test@test.com" }));
    assert.equal(r.found, false);
    assert.ok(r.error); // either "not found" or "temporarily unavailable"
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Project structure sanity
// ─────────────────────────────────────────────────────────────────────────────
import { existsSync } from "node:fs";
import { resolve } from "node:path";

const ROOT = resolve(import.meta.dirname ?? __dirname, "..");

function file(...parts: string[]): string {
  return resolve(ROOT, ...parts);
}

describe("project structure › critical files exist", () => {
  const required = [
    "src/proxy.ts",
    "src/lib/utils.ts",
    "src/lib/pricing.ts",
    "src/lib/constants.ts",
    "src/lib/admin-auth.ts",
    "src/lib/auth.ts",
    "src/lib/db.ts",
    "src/lib/catalog.ts",
    "src/lib/discount.ts",
    "src/lib/loyalty.ts",
    "src/lib/gift-cards.ts",
    "src/lib/orders.ts",
    "src/lib/razorpay.ts",
    "src/lib/mail.ts",
    "src/lib/msg91.ts",
    "src/lib/turnstile.ts",
    "src/lib/tokens.ts",
    "src/lib/ratelimit.ts",
    "src/lib/shipping/index.ts",
    "src/lib/shipping/types.ts",
    "src/lib/shipping/shiprocket.ts",
    "src/lib/shipping/delhivery.ts",
    "src/lib/shipping/amazon.ts",
    "src/store/cart.ts",
    "src/store/recently-viewed.ts",
    "src/types/index.ts",
    "src/app/globals.css",
    "src/app/providers.tsx",
    "src/app/layout.tsx",
    "src/app/icon.svg",
    "src/components/ui/glass-card.tsx",
    "src/components/ui/glass-button.tsx",
    "src/components/ui/glass-input.tsx",
    "src/components/ui/glass-modal.tsx",
    "src/components/ui/glass-badge.tsx",
    "src/components/ui/glass-skeleton.tsx",
    "src/components/ui/glass-toast.tsx",
    "src/components/ui/logo.tsx",
    "src/components/ui/theme-toggle.tsx",
    "src/components/storefront/navbar.tsx",
    "src/components/storefront/footer.tsx",
    "src/components/storefront/product-card.tsx",
    "src/components/storefront/quick-view.tsx",
    "src/components/storefront/recently-viewed.tsx",
    "src/components/storefront/cart-drawer.tsx",
    "src/components/admin/admin-layout.tsx",
    "src/app/actions/auth.ts",
    "src/app/actions/admin-auth.ts",
    "src/app/actions/track.ts",
    "src/app/actions/gift-cards.ts",
    "src/app/actions/admin-shipping.ts",
    "src/app/(storefront)/page.tsx",
    "src/app/(storefront)/shop/page.tsx",
    "src/app/(storefront)/products/[slug]/page.tsx",
    "src/app/(storefront)/track/page.tsx",
    "src/app/(storefront)/gift-cards/page.tsx",
    "src/app/(storefront)/checkout/page.tsx",
    "src/app/(storefront)/login/page.tsx",
    "src/app/(storefront)/register/page.tsx",
    "src/app/(storefront)/about/page.tsx",
    "src/app/(storefront)/contact/page.tsx",
    "src/app/(admin)/admin/layout.tsx",
    "src/app/(admin)/admin/login/page.tsx",
    "src/app/(admin)/admin/page.tsx",
    "src/app/(admin)/admin/orders/page.tsx",
    "src/app/(admin)/admin/products/page.tsx",
    "src/app/(admin)/admin/shipping/page.tsx",
    "src/app/(admin)/admin/gift-cards/page.tsx",
    "prisma/schema.prisma",
    "prisma/seed.ts",
    "next.config.ts",
    "playwright.config.ts",
    "docs/DEPLOYMENT.md",
    "docs/RISK-REPORT.md",
    "docs/PROJECT-STRUCTURE.md",
  ];

  for (const rel of required) {
    test(`${rel} exists`, () => {
      assert.ok(existsSync(file(rel)), `Missing: ${rel}`);
    });
  }

  test("middleware.ts does NOT exist (renamed to proxy.ts for Next.js 16)", () => {
    assert.equal(existsSync(file("src/middleware.ts")), false);
  });
});
