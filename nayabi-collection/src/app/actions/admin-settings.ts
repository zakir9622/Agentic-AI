"use server";

import { db } from "@/lib/db";
import { requireAdminSession } from "@/lib/admin-auth";
import { revalidatePath } from "next/cache";

interface ActionState {
  ok?: boolean;
  message?: string;
}

export async function saveAnnouncementAction(
  _prev: ActionState,
  formData: FormData
): Promise<ActionState> {
  await requireAdminSession();

  const id = formData.get("id") as string | null;
  const data = {
    text: formData.get("text") as string,
    bgColor: (formData.get("bgColor") as string) || "#C9A96E",
    textColor: (formData.get("textColor") as string) || "#0A0A14",
    linkUrl: (formData.get("linkUrl") as string) || null,
    linkText: (formData.get("linkText") as string) || null,
    isActive: formData.get("isActive") === "on",
  };

  if (!data.text?.trim()) return { message: "Text is required" };

  if (id) {
    await db.announcementBar.update({ where: { id }, data });
  } else {
    await db.announcementBar.create({ data });
  }

  revalidatePath("/");
  revalidatePath("/admin/settings/announcement");
  return { ok: true, message: "Saved successfully" };
}

export async function deleteAnnouncementAction(id: string): Promise<ActionState> {
  await requireAdminSession();
  await db.announcementBar.delete({ where: { id } });
  revalidatePath("/");
  return { ok: true, message: "Deleted" };
}
