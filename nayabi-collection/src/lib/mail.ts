import { Resend } from "resend";

let _resend: Resend | null = null;
function resend() {
  if (!_resend) _resend = new Resend(process.env.RESEND_API_KEY ?? "re_placeholder");
  return _resend;
}

const FROM = () =>
  `${process.env.RESEND_FROM_NAME ?? "Nayabi Collection"} <${process.env.RESEND_FROM_EMAIL ?? "onboarding@resend.dev"}>`;

const APP_URL = () => process.env.NEXT_PUBLIC_APP_URL ?? "http://localhost:3000";

/* Shared glass-styled HTML shell for all transactional emails */
function shell(title: string, bodyHtml: string) {
  return `<!DOCTYPE html><html><body style="margin:0;background:#0A0A14;font-family:Arial,Helvetica,sans-serif;padding:32px 16px;">
  <table role="presentation" width="100%" style="max-width:480px;margin:0 auto;background:#16162e;border:1px solid rgba(255,255,255,0.14);border-radius:20px;">
    <tr><td style="padding:32px;">
      <p style="font-size:20px;color:#F0EEF8;margin:0 0 4px;">Nayabi <span style="color:#C9A96E;">Collection</span></p>
      <h1 style="font-size:18px;color:#F0EEF8;margin:24px 0 12px;">${title}</h1>
      ${bodyHtml}
      <p style="font-size:11px;color:#7A7690;margin-top:32px;border-top:1px solid rgba(255,255,255,0.1);padding-top:16px;">
        Nayabi Collection · Premium modest wear, delivered across India.<br/>
        If you didn't expect this email you can safely ignore it.
      </p>
    </td></tr>
  </table></body></html>`;
}

function button(href: string, label: string) {
  return `<a href="${href}" style="display:inline-block;background:#C9A96E;color:#0A0A14;font-weight:bold;font-size:14px;padding:12px 28px;border-radius:12px;text-decoration:none;margin:16px 0;">${label}</a>`;
}

const p = (text: string) =>
  `<p style="font-size:14px;color:#B8B4D0;line-height:1.6;margin:8px 0;">${text}</p>`;

async function send(to: string, subject: string, html: string, text: string) {
  try {
    await resend().emails.send({ from: FROM(), to, subject, html, text });
  } catch (e) {
    console.error(`Email "${subject}" to ${to} failed:`, e);
  }
}

/* ── Auth emails ────────────────────────────────────────────────────────────── */

export async function sendVerificationEmail(to: string, token: string) {
  const url = `${APP_URL()}/verify-email?token=${token}`;
  await send(
    to,
    "Verify your email — Nayabi Collection",
    shell("Verify your email address", p("Tap the button below to confirm your email and activate your account.") + button(url, "Verify Email") + p(`Or paste this link into your browser:<br/><span style="color:#C9A96E;word-break:break-all;">${url}</span>`)),
    `Verify your email: ${url}`
  );
}

export async function sendPasswordResetEmail(to: string, token: string) {
  const url = `${APP_URL()}/reset-password?token=${token}`;
  await send(
    to,
    "Reset your password — Nayabi Collection",
    shell("Reset your password", p("You requested a password reset. This link expires in <strong style='color:#F0EEF8'>1 hour</strong> and can be used once.") + button(url, "Reset Password") + p("If you didn't request this, your account is still secure — no action needed.")),
    `Reset your password: ${url} (expires in 1 hour)`
  );
}

export async function sendPasswordChangedEmail(to: string) {
  await send(
    to,
    "Your password was changed — Nayabi Collection",
    shell("Password changed", p("Your account password was just changed.") + p(`Wasn't you? <a href="${APP_URL()}/contact" style="color:#C9A96E;">Contact us immediately</a>.`)),
    "Your Nayabi Collection password was changed. If this wasn't you, contact us immediately."
  );
}

export async function sendWelcomeEmail(to: string, name: string) {
  await send(
    to,
    "Welcome to Nayabi Collection ✨",
    shell(`Assalamu alaikum, ${name}!`, p("Welcome to the family. Discover beautifully crafted Hijabs, Abayas and Namaz Scarfs — with free shipping above ₹999.") + button(`${APP_URL()}/shop`, "Start Shopping")),
    `Welcome to Nayabi Collection, ${name}! Shop now: ${APP_URL()}/shop`
  );
}

/* ── Order emails ───────────────────────────────────────────────────────────── */

export async function sendOrderConfirmationEmail(params: {
  to: string;
  orderNumber: string;
  items: { name: string; qty: number; price: string }[];
  total: string;
  isCOD: boolean;
}) {
  const rows = params.items
    .map(
      (i) =>
        `<tr><td style="font-size:13px;color:#B8B4D0;padding:6px 0;">${i.name} × ${i.qty}</td><td style="font-size:13px;color:#F0EEF8;text-align:right;">${i.price}</td></tr>`
    )
    .join("");
  await send(
    params.to,
    `Order confirmed: ${params.orderNumber}`,
    shell(
      "Thank you for your order!",
      p(`Order <strong style="color:#C9A96E;">${params.orderNumber}</strong> is confirmed${params.isCOD ? ` — please keep <strong style="color:#F0EEF8;">${params.total}</strong> ready for delivery (Cash on Delivery)` : ""}.`) +
        `<table role="presentation" width="100%" style="margin:16px 0;border-top:1px solid rgba(255,255,255,0.1);">${rows}
        <tr><td style="font-size:14px;color:#F0EEF8;font-weight:bold;padding-top:10px;border-top:1px solid rgba(255,255,255,0.1);">Total</td><td style="font-size:14px;color:#C9A96E;font-weight:bold;text-align:right;padding-top:10px;border-top:1px solid rgba(255,255,255,0.1);">${params.total}</td></tr></table>` +
        button(`${APP_URL()}/order-confirmation/${params.orderNumber}`, "View Order")
    ),
    `Order ${params.orderNumber} confirmed. Total: ${params.total}`
  );
}

export async function sendAbandonedCartEmail(params: {
  to: string;
  sequence: 1 | 2 | 3;
  items: { name: string; price: string }[];
  discountCode?: string;
}) {
  const titles = {
    1: "You left something behind",
    2: "Your cart is still waiting",
    3: "Here's 5% off to complete your order",
  } as const;
  const rows = params.items
    .map((i) => p(`• ${i.name} — <span style="color:#C9A96E;">${i.price}</span>`))
    .join("");
  await send(
    params.to,
    `${titles[params.sequence]} — Nayabi Collection`,
    shell(
      titles[params.sequence],
      rows +
        (params.discountCode
          ? p(`Use code <strong style="color:#C9A96E;font-size:16px;">${params.discountCode}</strong> at checkout for 5% off.`)
          : "") +
        button(`${APP_URL()}/checkout`, "Complete My Order")
    ),
    `${titles[params.sequence]} — complete your order at ${APP_URL()}/checkout`
  );
}
