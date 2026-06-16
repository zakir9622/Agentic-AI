"use server";

import { checkGiftCardBalance, type GiftCardBalance } from "@/lib/gift-cards";

export async function checkBalanceAction(
  _prev: GiftCardBalance,
  formData: FormData
): Promise<GiftCardBalance> {
  return checkGiftCardBalance(String(formData.get("code") ?? ""));
}
