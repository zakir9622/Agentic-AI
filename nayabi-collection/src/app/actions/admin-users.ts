"use server";

import { z } from "zod";
import bcrypt from "bcryptjs";
import { db } from "@/lib/db";
import { requireAdminSession } from "@/lib/admin-auth";
import { revalidatePath } from "next/cache";

const createAdminSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters").max(100),
  email: z.email("Valid email required"),
  password: z
    .string()
    .min(8, "At least 8 characters")
    .regex(/[a-zA-Z]/, "Must include a letter")
    .regex(/[0-9]/, "Must include a number"),
  role: z.enum(["SUPER_ADMIN", "MANAGER", "FULFILLMENT"]),
});

export interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

export async function createAdminUserAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const session = await requireAdminSession();
  if (session.role !== "SUPER_ADMIN") {
    return { ok: false, message: "Only Super Admins can create admin accounts." };
  }

  const parsed = createAdminSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const key = String(issue.path[0] ?? "form");
      if (!errors[key]) errors[key] = issue.message;
    }
    return { ok: false, errors };
  }

  const { name, email, password, role } = parsed.data;
  const emailLc = email.toLowerCase();

  const existing = await db.adminUser.findUnique({ where: { email: emailLc } });
  if (existing) return { ok: false, errors: { email: "An admin with this email already exists." } };

  const hashedPassword = await bcrypt.hash(password, 12);
  await db.adminUser.create({
    data: { name, email: emailLc, hashedPassword, role },
  });

  revalidatePath("/admin/settings/admins");
  return { ok: true, message: `Admin account created for ${name}.` };
}

export async function deactivateAdminUserAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const session = await requireAdminSession();
  if (session.role !== "SUPER_ADMIN") {
    return { ok: false, message: "Only Super Admins can deactivate admin accounts." };
  }

  const adminId = formData.get("adminId") as string;
  if (!adminId) return { ok: false, message: "Missing admin ID." };
  if (adminId === session.adminId) return { ok: false, message: "You cannot deactivate your own account." };

  // Lock account with a far-future date to effectively disable it
  await db.adminUser.update({
    where: { id: adminId },
    data: { lockedUntil: new Date("2099-01-01") },
  });

  revalidatePath("/admin/settings/admins");
  return { ok: true, message: "Admin account deactivated." };
}

export async function reactivateAdminUserAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const session = await requireAdminSession();
  if (session.role !== "SUPER_ADMIN") {
    return { ok: false, message: "Only Super Admins can reactivate admin accounts." };
  }

  const adminId = formData.get("adminId") as string;
  if (!adminId) return { ok: false, message: "Missing admin ID." };

  await db.adminUser.update({
    where: { id: adminId },
    data: { lockedUntil: null, loginAttempts: 0 },
  });

  revalidatePath("/admin/settings/admins");
  return { ok: true, message: "Admin account reactivated." };
}
