import { NextRequest, NextResponse } from "next/server";
import { z } from "zod";
import { validateDiscountCode } from "@/lib/discount";

const schema = z.object({
  code: z.string().min(1).max(40),
  subtotal: z.number().int().positive(),
});

export async function POST(req: NextRequest) {
  try {
    const body = schema.safeParse(await req.json());
    if (!body.success)
      return NextResponse.json({ valid: false, reason: "Invalid request" }, { status: 400 });

    const result = await validateDiscountCode(body.data.code, body.data.subtotal);
    return NextResponse.json(result);
  } catch {
    return NextResponse.json({ valid: false, reason: "Server error" }, { status: 500 });
  }
}
