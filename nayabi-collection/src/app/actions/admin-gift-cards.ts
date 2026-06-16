"use server";

import { z } from "zod";
import { db } from "@/lib/db";
import { requireAdminSession } from "@/lib/admin-auth";
import { revalidatePath } from "next/cache";
import { generateGiftCardCode } from "@/lib/gift-cards";

export interface IssueState {
  ok?: boolean;
  message?: string;
  code?: string;
}

const schema = z.object({
  amountRupees: z.coerce.number().int().min(1).max(100000),
  recipientEmail: z.string().email().optional().or(z.literal("")),
  recipientName: z.string().optional(),
  message: z.string().optional(),
});

/** Admin: issue a new gift card with a generated code and an ISSUE transaction. */
export async function issueGiftCardAction(
  _prev: IssueState,
  formData: FormData
): Promise<IssueState> {
  const session = await requireAdminSession();

  const parsed = schema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) return { message: "Enter a valid amount (₹1–₹1,00,000)." };

  const { amountRupees, recipientEmail, recipientName, message } = parsed.data;
  const paise = Math.round(amountRupees * 100);
  const code = generateGiftCardCode();

  try {
    await db.giftCard.create({
      data: {
        code,
        initialValue: paise,
        balance: paise,
        status: "ACTIVE",
        recipientEmail: recipientEmail || null,
        recipientName: recipientName || null,
        message: message || null,
        issuedBy: session.name ?? session.email ?? "admin",
        transactions: {
          create: { amount: paise, type: "ISSUE", note: "Issued by admin" },
        },
      },
    });
    revalidatePath("/admin/gift-cards");
    return { ok: true, message: `Issued ₹${amountRupees} gift card.`, code };
  } catch {
    return { message: "Couldn't issue gift card — database unavailable." };
  }
}
