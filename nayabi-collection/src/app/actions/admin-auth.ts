"use server";

import { z } from "zod";
import bcrypt from "bcryptjs";
import { db } from "@/lib/db";
import { createAdminSession, buildAdminCookie, clearAdminCookie } from "@/lib/admin-auth";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

const loginSchema = z.object({
  email: z.email("Enter valid email"),
  password: z.string().min(1, "Password is required"),
});

export async function adminLoginAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const parsed = loginSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    return { errors: { form: "Invalid credentials" } };
  }

  const { email, password } = parsed.data;

  const admin = await db.adminUser.findUnique({ where: { email } });
  if (!admin) return { errors: { form: "Invalid credentials" } };

  if (admin.lockedUntil && admin.lockedUntil > new Date()) {
    return { errors: { form: "Account temporarily locked. Try again later." } };
  }

  const valid = await bcrypt.compare(password, admin.hashedPassword);
  if (!valid) {
    const attempts = admin.loginAttempts + 1;
    const lockUntil = attempts >= 5 ? new Date(Date.now() + 15 * 60 * 1000) : null;
    await db.adminUser.update({
      where: { id: admin.id },
      data: { loginAttempts: attempts, lockedUntil: lockUntil },
    });
    return { errors: { form: "Invalid credentials" } };
  }

  await db.adminUser.update({
    where: { id: admin.id },
    data: { loginAttempts: 0, lockedUntil: null, lastLoginAt: new Date() },
  });

  const session = createAdminSession(admin);
  const jar = await cookies();
  jar.set({
    name: "nc_admin",
    value: Buffer.from(JSON.stringify(session)).toString("base64"),
    httpOnly: true,
    sameSite: "strict",
    path: "/admin",
    maxAge: 8 * 60 * 60,
  });

  redirect("/admin");
}

export async function adminLogoutAction() {
  const jar = await cookies();
  jar.delete("nc_admin");
  redirect("/admin/login");
}
