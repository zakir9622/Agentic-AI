"use server";

import { z } from "zod";
import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { isValidPincode, isValidPhone } from "@/lib/utils";
import { INDIAN_STATES, RETURN_WINDOW_DAYS } from "@/lib/constants";
import { revalidatePath } from "next/cache";

export interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

/* ── Address Management ──────────────────────────────────────────────────────── */

const addressSchema = z.object({
  name: z.string().min(2, "Name is required"),
  phone: z.string().refine((v) => isValidPhone(v), "Enter valid 10-digit mobile number"),
  line1: z.string().min(5, "Address line 1 is required"),
  line2: z.string().optional(),
  city: z.string().min(2, "City is required"),
  state: z.string().refine((v) => INDIAN_STATES.some((s) => s === v), "Select a valid state"),
  pincode: z.string().refine((v) => isValidPincode(v), "Enter valid 6-digit pincode"),
  isDefault: z.string().optional(),
});

export async function saveAddressAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const session = await auth();
  if (!session?.user) return { message: "Please log in" };

  const addressId = formData.get("addressId") as string | null;
  const parsed = addressSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const k = String(issue.path[0] ?? "form");
      if (!errors[k]) errors[k] = issue.message;
    }
    return { errors };
  }

  const { isDefault: isDefaultStr, ...data } = parsed.data;
  const isDefault = isDefaultStr === "on";

  if (isDefault) {
    await db.address.updateMany({
      where: { userId: session.user.id },
      data: { isDefault: false },
    });
  }

  if (addressId) {
    const existing = await db.address.findFirst({
      where: { id: addressId, userId: session.user.id },
    });
    if (!existing) return { message: "Address not found" };
    await db.address.update({
      where: { id: addressId },
      data: { ...data, isDefault },
    });
  } else {
    const count = await db.address.count({ where: { userId: session.user.id } });
    await db.address.create({
      data: {
        userId: session.user.id,
        ...data,
        isDefault: count === 0 || isDefault,
      },
    });
  }

  revalidatePath("/account/addresses");
  return { ok: true, message: "Address saved" };
}

export async function deleteAddressAction(addressId: string): Promise<ActionState> {
  const session = await auth();
  if (!session?.user) return { message: "Please log in" };

  const address = await db.address.findFirst({
    where: { id: addressId, userId: session.user.id },
  });
  if (!address) return { message: "Address not found" };

  await db.address.delete({ where: { id: addressId } });

  if (address.isDefault) {
    const next = await db.address.findFirst({
      where: { userId: session.user.id },
      orderBy: { id: "asc" },
    });
    if (next) await db.address.update({ where: { id: next.id }, data: { isDefault: true } });
  }

  revalidatePath("/account/addresses");
  return { ok: true, message: "Address deleted" };
}

/* ── Wishlist ─────────────────────────────────────────────────────────────────── */

export async function toggleWishlistAction(
  productId: string,
  variantId?: string
): Promise<ActionState> {
  const session = await auth();
  if (!session?.user) return { message: "Please log in to save items" };

  const existing = await db.wishlist.findFirst({
    where: variantId
      ? { userId: session.user.id, variantId }
      : { userId: session.user.id, productId, variantId: null },
  });

  if (existing) {
    await db.wishlist.delete({ where: { id: existing.id } });
    revalidatePath("/account/wishlist");
    return { ok: true, message: "Removed from wishlist" };
  }

  await db.wishlist.create({
    data: {
      userId: session.user.id,
      productId,
      variantId: variantId ?? null,
    },
  });
  revalidatePath("/account/wishlist");
  return { ok: true, message: "Added to wishlist" };
}

/* ── Returns ──────────────────────────────────────────────────────────────────── */

const returnItemSchema = z.object({
  orderItemId: z.string(),
  variantId: z.string(),
  quantity: z.coerce.number().min(1),
  reason: z.string().min(5, "Please provide a reason"),
});

const returnRequestSchema = z.object({
  orderId: z.string(),
  type: z.enum(["RETURN", "EXCHANGE"]),
  refundMethod: z.enum(["ORIGINAL_PAYMENT", "STORE_CREDIT", "BANK_TRANSFER"]).optional(),
  items: z.array(returnItemSchema).min(1, "Select at least one item"),
});

export async function createReturnRequestAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const session = await auth();
  if (!session?.user) return { message: "Please log in" };

  const orderId = formData.get("orderId") as string;
  const type = formData.get("type") as string;
  const refundMethod = (formData.get("refundMethod") as string) || undefined;
  const itemsJson = formData.get("items") as string;

  let items: unknown[];
  try {
    items = JSON.parse(itemsJson);
  } catch {
    return { message: "Invalid request data" };
  }

  const parsed = returnRequestSchema.safeParse({ orderId, type, refundMethod, items });
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const k = String(issue.path[0] ?? "form");
      if (!errors[k]) errors[k] = issue.message;
    }
    return { errors };
  }

  const order = await db.order.findFirst({
    where: { id: orderId, userId: session.user.id },
    include: { items: true },
  });
  if (!order) return { message: "Order not found" };

  if (!["DELIVERED"].includes(order.orderStatus)) {
    return { message: "Returns can only be requested for delivered orders" };
  }

  const returnWindowMs = RETURN_WINDOW_DAYS * 24 * 60 * 60 * 1000;
  const deliveredAt = order.updatedAt;
  if (Date.now() - deliveredAt.getTime() > returnWindowMs) {
    return { message: `Return window of ${RETURN_WINDOW_DAYS} days has passed` };
  }

  const existing = await db.returnRequest.findFirst({
    where: { orderId, userId: session.user.id, status: { not: "REJECTED" } },
  });
  if (existing) return { message: "A return request already exists for this order" };

  const { items: returnItems, ...rest } = parsed.data;
  await db.returnRequest.create({
    data: {
      orderId,
      userId: session.user.id,
      type: rest.type,
      refundMethod: rest.refundMethod ?? null,
      status: "PENDING",
      items: {
        create: returnItems.map((item) => ({
          orderItemId: item.orderItemId,
          variantId: item.variantId,
          quantity: item.quantity,
          reason: item.reason,
        })),
      },
    },
  });

  revalidatePath(`/account/orders/${orderId}`);
  return { ok: true, message: "Return request submitted. We'll review it within 24 hours." };
}
