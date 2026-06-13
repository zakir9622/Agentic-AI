import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { db } from "@/lib/db";

const schema = z.object({
  variantId: z.string().min(1),
  email: z.email(),
  phone: z.string().optional(),
});

export async function POST(req: NextRequest) {
  try {
    const body = schema.safeParse(await req.json());
    if (!body.success) {
      return NextResponse.json({ success: false, error: "Invalid input" }, { status: 400 });
    }
    const { variantId, email, phone } = body.data;

    await db.backInStockAlert.upsert({
      where: { variantId_email: { variantId, email } },
      create: { variantId, email, phone },
      update: { phone, notifiedAt: null },
    });

    return NextResponse.json({ success: true });
  } catch {
    return NextResponse.json({ success: false, error: "Server error" }, { status: 500 });
  }
}
