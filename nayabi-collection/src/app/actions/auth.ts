"use server";

import { z } from "zod";
import bcrypt from "bcryptjs";
import { db } from "@/lib/db";
import { auth } from "@/lib/auth";
import {
  createVerificationToken,
  createPasswordResetToken,
  checkPasswordResetToken,
  markPasswordResetTokenUsed,
} from "@/lib/tokens";
import {
  sendVerificationEmail,
  sendPasswordResetEmail,
  sendPasswordChangedEmail,
  sendWelcomeEmail,
} from "@/lib/mail";

export interface ActionState {
  ok?: boolean;
  message?: string;
  errors?: Record<string, string>;
}

const passwordSchema = z
  .string()
  .min(8, "At least 8 characters")
  .regex(/[a-zA-Z]/, "Must include a letter")
  .regex(/[0-9]/, "Must include a number");

/* ── Register ───────────────────────────────────────────────────────────────── */

const registerSchema = z.object({
  name: z.string().min(2, "Please enter your full name").max(100),
  email: z.email("Please enter a valid email"),
  password: passwordSchema,
  confirmPassword: z.string(),
  terms: z.literal("on", { error: "Please accept the terms to continue" }),
});

export async function registerAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const parsed = registerSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const key = String(issue.path[0] ?? "form");
      if (!errors[key]) errors[key] = issue.message;
    }
    return { ok: false, errors };
  }
  const { name, email, password, confirmPassword } = parsed.data;
  if (password !== confirmPassword) {
    return { ok: false, errors: { confirmPassword: "Passwords don't match" } };
  }

  const emailLc = email.toLowerCase();
  const existing = await db.user.findUnique({ where: { email: emailLc } });
  if (existing) {
    return { ok: false, errors: { email: "An account with this email already exists" } };
  }

  const hashedPassword = await bcrypt.hash(password, 12);
  const user = await db.user.create({
    data: { name, email: emailLc, hashedPassword },
  });

  const token = await createVerificationToken(user.id);
  void Promise.allSettled([
    sendVerificationEmail(emailLc, token),
    sendWelcomeEmail(emailLc, name),
  ]);

  return { ok: true, message: "Account created! Check your inbox to verify your email." };
}

/* ── Resend verification ───────────────────────────────────────────────────── */

export async function resendVerificationAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const email = z.email().safeParse(formData.get("email"));
  // Always report success — never reveal whether an account exists
  if (email.success) {
    const user = await db.user.findUnique({ where: { email: email.data.toLowerCase() } });
    if (user && !user.emailVerified) {
      const token = await createVerificationToken(user.id);
      void sendVerificationEmail(user.email, token);
    }
  }
  return { ok: true, message: "If an unverified account exists, a new link is on its way." };
}

/* ── Forgot password ───────────────────────────────────────────────────────── */

export async function forgotPasswordAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const email = z.email("Please enter a valid email").safeParse(formData.get("email"));
  if (!email.success) {
    return { ok: false, errors: { email: email.error.issues[0].message } };
  }
  const user = await db.user.findUnique({ where: { email: email.data.toLowerCase() } });
  if (user) {
    const token = await createPasswordResetToken(user.id);
    void sendPasswordResetEmail(user.email, token);
  }
  // Same response either way — prevents user enumeration
  return {
    ok: true,
    message: "If an account exists for that email, a reset link is on its way.",
  };
}

/* ── Reset password ────────────────────────────────────────────────────────── */

const resetSchema = z.object({
  token: z.string().min(1),
  password: passwordSchema,
  confirmPassword: z.string(),
});

export async function resetPasswordAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const parsed = resetSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    return { ok: false, errors: { password: parsed.error.issues[0].message } };
  }
  const { token, password, confirmPassword } = parsed.data;
  if (password !== confirmPassword) {
    return { ok: false, errors: { confirmPassword: "Passwords don't match" } };
  }

  const check = await checkPasswordResetToken(token);
  if (!check.ok) {
    const msgs = {
      invalid: "This reset link is invalid.",
      expired: "This reset link has expired. Please request a new one.",
      used: "This link has already been used. Please request a new one.",
    } as const;
    return { ok: false, message: msgs[check.reason] };
  }

  const hashedPassword = await bcrypt.hash(password, 12);
  await db.$transaction([
    db.user.update({ where: { id: check.userId }, data: { hashedPassword } }),
    db.session.deleteMany({ where: { userId: check.userId } }),
  ]);
  await markPasswordResetTokenUsed(token);

  const user = await db.user.findUnique({ where: { id: check.userId } });
  if (user) void sendPasswordChangedEmail(user.email);

  return { ok: true, message: "Password updated! You can now sign in." };
}

/* ── Change password (authenticated) ───────────────────────────────────────── */

const changeSchema = z.object({
  currentPassword: z.string().min(1, "Enter your current password"),
  newPassword: passwordSchema,
  confirmPassword: z.string(),
});

export async function changePasswordAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  const session = await auth();
  if (!session?.user?.id) return { ok: false, message: "Please sign in first." };

  const parsed = changeSchema.safeParse(Object.fromEntries(formData));
  if (!parsed.success) {
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const key = String(issue.path[0] ?? "form");
      if (!errors[key]) errors[key] = issue.message;
    }
    return { ok: false, errors };
  }
  const { currentPassword, newPassword, confirmPassword } = parsed.data;
  if (newPassword !== confirmPassword) {
    return { ok: false, errors: { confirmPassword: "Passwords don't match" } };
  }

  const user = await db.user.findUnique({ where: { id: session.user.id } });
  if (!user?.hashedPassword) {
    return {
      ok: false,
      message: "This account uses Google sign-in and has no password set.",
    };
  }

  const valid = await bcrypt.compare(currentPassword, user.hashedPassword);
  if (!valid) return { ok: false, errors: { currentPassword: "Incorrect current password" } };

  const hashedPassword = await bcrypt.hash(newPassword, 12);
  await db.user.update({ where: { id: user.id }, data: { hashedPassword } });

  if (formData.get("signOutOthers") === "on") {
    await db.session.deleteMany({ where: { userId: user.id } });
  }

  void sendPasswordChangedEmail(user.email);
  return { ok: true, message: "Password updated successfully." };
}
