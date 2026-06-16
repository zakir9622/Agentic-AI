import { customAlphabet } from "nanoid";
import { db } from "@/lib/db";

const gen = customAlphabet("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", 12);

/** Human-friendly, hard-to-guess gift card code, e.g. NAYABI-AB3C-D4EF-GH5J. */
export function generateGiftCardCode(): string {
  const raw = gen();
  return `NAYABI-${raw.slice(0, 4)}-${raw.slice(4, 8)}-${raw.slice(8, 12)}`;
}

export interface GiftCardBalance {
  found: boolean;
  code?: string;
  balance?: number; // paise
  initialValue?: number; // paise
  status?: string;
  expiresAt?: string | null;
  error?: string;
  demo?: boolean;
}

/** Look up a gift card's balance. `NAYABI-GIFT` returns a sample for preview. */
export async function checkGiftCardBalance(codeRaw: string): Promise<GiftCardBalance> {
  const code = codeRaw.trim().toUpperCase();
  if (!code) return { found: false, error: "Please enter a gift card code." };

  if (code === "NAYABI-GIFT" || code === "NAYABI-DEMO") {
    return {
      found: true,
      demo: true,
      code,
      balance: 100000,
      initialValue: 150000,
      status: "ACTIVE",
      expiresAt: null,
    };
  }

  try {
    const card = await db.giftCard.findUnique({ where: { code } });
    if (!card) return { found: false, error: "No gift card found with that code." };
    return {
      found: true,
      code: card.code,
      balance: card.balance,
      initialValue: card.initialValue,
      status: card.status,
      expiresAt: card.expiresAt?.toISOString() ?? null,
    };
  } catch {
    return { found: false, error: "Gift card lookup is temporarily unavailable." };
  }
}
