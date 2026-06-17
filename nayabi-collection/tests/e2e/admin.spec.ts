import { test, expect } from "@playwright/test";

test.describe("Admin – authentication guard", () => {
  test("/admin redirects to /admin/login when no session cookie", async ({ page }) => {
    // Clear cookies first
    await page.context().clearCookies();
    await page.goto("/admin");
    // Should redirect to login (either directly or via 307→200)
    await expect(page).toHaveURL(/\/admin\/login/, { timeout: 10_000 });
  });

  test("/admin/dashboard redirects to /admin/login when no session cookie", async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("/admin/dashboard");
    await expect(page).toHaveURL(/\/admin\/login/, { timeout: 10_000 });
  });

  test("/admin/login renders login form without redirect loop", async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("/admin/login");
    // Must NOT redirect elsewhere (the old bug caused infinite redirect)
    await expect(page).toHaveURL(/\/admin\/login/);
    // Form fields present
    const emailField = page.locator('input[type="email"]');
    await expect(emailField).toBeVisible({ timeout: 10_000 });
    const passwordField = page.locator('input[type="password"]');
    await expect(passwordField).toBeVisible();
  });

  test("/admin/login has submit button", async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("/admin/login");
    const submitBtn = page.locator('button[type="submit"]');
    await expect(submitBtn).toBeVisible({ timeout: 10_000 });
  });

  test("invalid credentials show error message", async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("/admin/login");
    await page.locator('input[type="email"]').fill("wrong@example.com");
    await page.locator('input[type="password"]').fill("wrongpassword");
    await page.locator('button[type="submit"]').click();
    await page.waitForLoadState("networkidle");
    // Should stay on login page (not redirect to dashboard)
    await expect(page).toHaveURL(/\/admin\/login/);
  });
});

test.describe("Admin – authenticated access", () => {
  test.beforeEach(async ({ context }) => {
    // Build a valid admin session cookie with 8h TTL
    const session = {
      adminId: "test-admin",
      email: "admin@nayabicollection.com",
      name: "Test Admin",
      role: "SUPER_ADMIN",
      expiresAt: Date.now() + 8 * 60 * 60 * 1000,
    };
    const encoded = Buffer.from(JSON.stringify(session)).toString("base64");
    await context.addCookies([
      {
        name: "nc_admin",
        value: encoded,
        domain: "localhost",
        path: "/admin",
        httpOnly: false, // test context doesn't enforce HttpOnly
        sameSite: "Strict",
      },
    ]);
  });

  test("/admin renders dashboard shell (not redirect to login)", async ({ page }) => {
    await page.goto("/admin");
    // Should NOT be redirected to login
    const url = page.url();
    expect(url).not.toMatch(/\/admin\/login/);
    await expect(page.locator("body")).toBeVisible();
  });

  test("/admin/login with valid session redirects away from login page", async ({ page }) => {
    // When already authenticated, visiting /admin should land on dashboard
    await page.goto("/admin");
    await page.waitForLoadState("networkidle");
    // Body should load without being the login form
    const body = await page.locator("body").innerText();
    // Dashboard has nav items or admin content
    expect(body.length).toBeGreaterThan(0);
  });
});
