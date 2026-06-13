import { nanoid } from "nanoid";
import { db } from "@/lib/db";

export async function createVerificationToken(userId: string): Promise<string> {
  const token = nanoid(48);
  await db.verificationToken.create({
    data: { token, userId, expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000) },
  });
  return token;
}

export async function consumeVerificationToken(
  token: string
): Promise<{ ok: true; userId: string } | { ok: false; reason: "invalid" | "expired" | "used" }> {
  const row = await db.verificationToken.findUnique({ where: { token } });
  if (!row) return { ok: false, reason: "invalid" };
  if (row.usedAt) return { ok: false, reason: "used" };
  if (row.expiresAt < new Date()) return { ok: false, reason: "expired" };

  await db.$transaction([
    db.verificationToken.update({ where: { token }, data: { usedAt: new Date() } }),
    db.user.update({ where: { id: row.userId }, data: { emailVerified: new Date() } }),
  ]);
  return { ok: true, userId: row.userId };
}

export async function createPasswordResetToken(userId: string): Promise<string> {
  const token = nanoid(48);
  await db.passwordResetToken.create({
    data: { token, userId, expiresAt: new Date(Date.now() + 60 * 60 * 1000) }, // 1 hour
  });
  return token;
}

export async function checkPasswordResetToken(
  token: string
): Promise<{ ok: true; userId: string } | { ok: false; reason: "invalid" | "expired" | "used" }> {
  const row = await db.passwordResetToken.findUnique({ where: { token } });
  if (!row) return { ok: false, reason: "invalid" };
  if (row.usedAt) return { ok: false, reason: "used" };
  if (row.expiresAt < new Date()) return { ok: false, reason: "expired" };
  return { ok: true, userId: row.userId };
}

export async function markPasswordResetTokenUsed(token: string) {
  await db.passwordResetToken.update({ where: { token }, data: { usedAt: new Date() } });
}
