/* Cloudflare Turnstile server-side verification.
   Returns true (allow) if the secret key is not configured (graceful degradation). */

export async function verifyTurnstile(token: string | null): Promise<boolean> {
  const secret = process.env.TURNSTILE_SECRET_KEY;
  if (!secret) return true; // not configured — skip verification

  if (!token) return false;

  try {
    const body = new URLSearchParams({ secret, response: token });
    const res = await fetch("https://challenges.cloudflare.com/turnstile/v0/siteverify", {
      method: "POST",
      body,
    });
    const data: { success: boolean } = await res.json();
    return data.success;
  } catch {
    return true; // fail-open: don't block users on network error
  }
}
