import { NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin-auth";
import { db } from "@/lib/db";

export async function GET() {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const discounts = await db.discountCode.findMany({
    orderBy: { createdAt: "desc" },
  });
  return NextResponse.json(discounts);
}
