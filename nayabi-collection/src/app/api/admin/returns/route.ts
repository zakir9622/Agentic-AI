import { NextRequest, NextResponse } from "next/server";
import { getAdminSession } from "@/lib/admin-auth";
import { db } from "@/lib/db";
import type { ReturnStatus } from "@/generated/prisma/client";

const VALID_STATUSES: ReturnStatus[] = [
  "PENDING", "APPROVED", "REJECTED", "PICKUP_SCHEDULED", "RECEIVED", "REFUNDED", "EXCHANGED",
];

export async function GET(req: NextRequest) {
  const session = await getAdminSession();
  if (!session) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const statusParam = req.nextUrl.searchParams.get("status");
  const validStatus = VALID_STATUSES.includes(statusParam as ReturnStatus)
    ? (statusParam as ReturnStatus)
    : undefined;

  const returns = await db.returnRequest.findMany({
    where: validStatus ? { status: validStatus } : {},
    include: {
      order: { select: { orderNumber: true } },
      user: { select: { name: true, email: true } },
      items: {
        include: {
          orderItem: { select: { productName: true, variantDetails: true } },
        },
      },
    },
    orderBy: { createdAt: "desc" },
    take: 50,
  });

  const formatted = returns.map((r) => ({
    id: r.id,
    orderId: r.orderId,
    orderNumber: r.order.orderNumber,
    type: r.type,
    status: r.status,
    createdAt: r.createdAt,
    userName: r.user.name,
    userEmail: r.user.email,
    refundMethod: r.refundMethod,
    items: r.items.map((item) => ({
      id: item.id,
      productName: item.orderItem.productName,
      variantDetails: item.orderItem.variantDetails,
      quantity: item.quantity,
      reason: item.reason,
    })),
  }));

  return NextResponse.json(formatted);
}
