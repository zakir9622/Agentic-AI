/**
 * Unit + integration tests for core business logic. Run with: npm test
 * (tsx executes Node's built-in test runner; the @/ alias resolves via tsconfig).
 */
import { test } from "node:test";
import assert from "node:assert/strict";

import {
  formatPrice,
  toPaise,
  slugify,
  isValidPincode,
  isValidPhone,
  generateOrderNumber,
} from "@/lib/utils";
import { computePricing } from "@/lib/pricing";
import { FREE_SHIPPING_THRESHOLD, GIFT_CARD_DENOMINATIONS, BRAND } from "@/lib/constants";
import {
  getProvider,
  listProviders,
  defaultProvider,
  configuredProviders,
} from "@/lib/shipping";
import { getLoyalty, LOYALTY_TIERS, POINTS_PER_RUPEE } from "@/lib/loyalty";
import { generateGiftCardCode, checkGiftCardBalance } from "@/lib/gift-cards";

// ── utils ─────────────────────────────────────────────────────────────────────
test("formatPrice renders paise as ₹ rupees", () => {
  assert.equal(formatPrice(59900), "₹599");
  assert.equal(formatPrice(0), "₹0");
});

test("toPaise round-trips rupees", () => {
  assert.equal(toPaise(599), 59900);
});

test("slugify normalizes text", () => {
  assert.equal(slugify("Silk Georgette Hijab"), "silk-georgette-hijab");
});

test("pincode and phone validators", () => {
  assert.equal(isValidPincode("400001"), true);
  assert.equal(isValidPincode("12"), false);
  assert.equal(isValidPhone("9500738518"), true);
  assert.equal(isValidPhone("123"), false);
});

test("generateOrderNumber has the NC-<date>- format", () => {
  assert.match(generateOrderNumber(), /^NC-\d{8}-/);
});

// ── pricing ─────────────────────────────────────────────────────────────────────
test("computePricing: free shipping above threshold", () => {
  const r = computePricing({ subtotal: FREE_SHIPPING_THRESHOLD });
  assert.equal(r.shippingCost, 0);
  assert.equal(r.total, FREE_SHIPPING_THRESHOLD);
});

test("computePricing: flat shipping below threshold + COD charge", () => {
  const r = computePricing({ subtotal: 50000, isCOD: true });
  assert.ok(r.shippingCost > 0, "expected flat shipping");
  assert.ok(r.codCharge > 0, "expected COD charge");
  assert.equal(r.total, 50000 + r.shippingCost + r.codCharge);
});

test("computePricing: discount never exceeds subtotal", () => {
  const r = computePricing({ subtotal: 10000, discountAmount: 99999 });
  assert.equal(r.discountAmount, 10000);
  assert.equal(r.total, 0 + r.shippingCost);
});

// ── shipping registry ──────────────────────────────────────────────────────────
test("shipping registry exposes the three carriers", () => {
  const ids = listProviders()
    .map((p) => p.id)
    .sort();
  assert.deepEqual(ids, ["amazon", "delhivery", "shiprocket"]);
});

test("getProvider returns null for unknown carrier", () => {
  assert.equal(getProvider("fedex"), null);
  assert.equal(getProvider("delhivery")?.label, "Delhivery");
});

test("providers report unconfigured without env, defaultProvider still resolves", () => {
  assert.equal(configuredProviders().length, 0);
  assert.equal(defaultProvider().id, "shiprocket");
});

// ── loyalty ──────────────────────────────────────────────────────────────────────
test("getLoyalty(null) is an empty Bronze account", async () => {
  const l = await getLoyalty(null);
  assert.equal(l.points, 0);
  assert.equal(l.tier, "BRONZE");
  assert.equal(l.nextTier, "SILVER");
});

test("loyalty tiers are ascending and points rate is sane", () => {
  for (let i = 1; i < LOYALTY_TIERS.length; i++) {
    assert.ok(LOYALTY_TIERS[i].min > LOYALTY_TIERS[i - 1].min);
  }
  assert.ok(POINTS_PER_RUPEE > 0);
});

// ── gift cards ───────────────────────────────────────────────────────────────────
test("generateGiftCardCode matches NAYABI-XXXX-XXXX-XXXX", () => {
  assert.match(generateGiftCardCode(), /^NAYABI-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$/);
});

test("checkGiftCardBalance returns the demo card for NAYABI-GIFT", async () => {
  const r = await checkGiftCardBalance("nayabi-gift");
  assert.equal(r.found, true);
  assert.equal(r.demo, true);
  assert.equal(r.status, "ACTIVE");
  assert.ok((r.balance ?? 0) > 0);
});

test("checkGiftCardBalance rejects empty input", async () => {
  const r = await checkGiftCardBalance("  ");
  assert.equal(r.found, false);
  assert.ok(r.error);
});

// ── brand constants ────────────────────────────────────────────────────────────
test("BRAND wires the real contact details", () => {
  assert.equal(BRAND.instagramHandle, "nayabi_collection");
  assert.ok(BRAND.whatsappUrl("hi").includes("919500738518"));
  assert.equal(GIFT_CARD_DENOMINATIONS.length, 4);
});
