import { test, expect } from "@playwright/test";

test.describe("Navigation – desktop header", () => {
  test("nav link 'Shop All' routes to /shop", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("link", { name: "Shop All" }).first().click();
    await expect(page).toHaveURL(/\/shop/);
  });

  test("category nav link routes with category param", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("link", { name: "Abayas" }).first().click();
    await expect(page).toHaveURL(/\/shop\?category=abayas/);
  });

  test("logo returns to home from an inner page", async ({ page }) => {
    await page.goto("/shop");
    // Both the navbar and footer carry the brand link — use the first (navbar)
    await page.getByRole("link", { name: /Nayabi Collection.*home/i }).first().click();
    await expect(page).toHaveURL("http://localhost:3000/");
  });
});

test.describe("Navigation – search flow", () => {
  test("desktop search affordance opens and submits a query", async ({ page }) => {
    await page.goto("/");
    // The desktop "Search products" pill toggles the search input
    await page.getByRole("button", { name: "Search products" }).first().click();
    const input = page.locator('input[type="search"]');
    await expect(input).toBeVisible({ timeout: 10_000 });
    await input.fill("hijab");
    await input.press("Enter");
    await expect(page).toHaveURL(/\/shop\?q=hijab/);
  });
});

test.describe("Navigation – mobile menu", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  test("hamburger opens the mobile nav and a link navigates", async ({ page }) => {
    await page.goto("/");
    // Desktop nav is hidden at this width; open the mobile menu
    await page.getByRole("button", { name: "Open menu" }).click();
    const mobileNav = page.locator("#mobile-nav");
    await expect(mobileNav).toBeVisible();
    // Tap a link inside the mobile panel
    await mobileNav.getByRole("link", { name: "Shop All" }).click();
    await expect(page).toHaveURL(/\/shop/);
  });

  test("menu button toggles open/closed", async ({ page }) => {
    await page.goto("/");
    const openBtn = page.getByRole("button", { name: "Open menu" });
    await openBtn.click();
    await expect(page.locator("#mobile-nav")).toBeVisible();
    // Now labelled "Close menu" — clicking hides the panel
    await page.getByRole("button", { name: "Close menu" }).click();
    await expect(page.locator("#mobile-nav")).toBeHidden();
  });
});

test.describe("Shopping – cart drawer", () => {
  test("cart button opens the drawer; empty state + close work", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("button", { name: /Shopping bag/ }).click();
    const drawer = page.getByRole("dialog", { name: "Shopping bag" });
    await expect(drawer).toBeVisible();
    // Fresh session → empty bag
    await expect(drawer.getByText(/Your bag is empty/i)).toBeVisible();
    // Escape closes the drawer
    await page.keyboard.press("Escape");
    await expect(drawer).toBeHidden();
  });
});

test.describe("Shopping – quick view", () => {
  test("quick-view button opens a product modal on the shop page", async ({ page }) => {
    await page.goto("/shop");
    await page.waitForLoadState("networkidle");
    const quickView = page.getByRole("button", { name: /Quick view/ }).first();
    // The eye is always visible on touch widths; ensure it exists then click
    await expect(quickView).toBeAttached({ timeout: 10_000 });
    await quickView.click({ force: true });
    // A modal dialog should appear (skeleton → product). Scope to the modal
    // (aria-modal="true") so it isn't confused with the cookie-consent dialog.
    await expect(page.locator('[role="dialog"][aria-modal="true"]')).toBeVisible({
      timeout: 10_000,
    });
  });
});

test.describe("Shopping – product detail page", () => {
  test("a product slug renders the PDP (not a 404)", async ({ page }) => {
    await page.goto("/products/silk-georgette-hijab");
    await page.waitForLoadState("networkidle");
    // Product title heading present
    await expect(
      page.getByRole("heading", { level: 1, name: /Silk Georgette Hijab/i })
    ).toBeVisible({ timeout: 10_000 });
    // Add-to-bag action present, and not the 404 copy
    await expect(page.getByRole("button", { name: /Add to Bag/i })).toBeVisible();
    await expect(page.getByText(/Page not found/i)).toHaveCount(0);
  });

  test("full journey: shop → product card → PDP", async ({ page }) => {
    await page.goto("/shop");
    await page.waitForLoadState("networkidle");
    // Click the first product card link (cards link to /products/[slug])
    await page.locator('a[href^="/products/"]').first().click();
    await expect(page).toHaveURL(/\/products\//);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible({ timeout: 10_000 });
  });
});

test.describe("Shopping – checkout reachability", () => {
  test("/checkout responds with a usable page (not a crash)", async ({ page }) => {
    const res = await page.goto("/checkout");
    // Either renders the checkout page or redirects to cart/home — never 5xx
    expect(res?.status() ?? 200).toBeLessThan(500);
    await expect(page.locator("body")).toBeVisible();
  });
});
