import { NextRequest, NextResponse } from "next/server";
import { requireAdminSession } from "@/lib/admin-auth";
import crypto from "crypto";

const CLOUD_NAME = () => process.env.CLOUDINARY_CLOUD_NAME;
const API_KEY = () => process.env.CLOUDINARY_API_KEY;
const API_SECRET = () => process.env.CLOUDINARY_API_SECRET;

export async function POST(req: NextRequest) {
  try {
    await requireAdminSession();
  } catch {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const cloud = CLOUD_NAME();
  const key = API_KEY();
  const secret = API_SECRET();
  if (!cloud || !key || !secret) {
    return NextResponse.json({ error: "Cloudinary not configured" }, { status: 503 });
  }

  const form = await req.formData();
  const file = form.get("file") as File | null;
  if (!file) return NextResponse.json({ error: "No file provided" }, { status: 400 });

  const maxBytes = 10 * 1024 * 1024; // 10 MB
  if (file.size > maxBytes) {
    return NextResponse.json({ error: "File too large (max 10 MB)" }, { status: 413 });
  }

  const allowed = ["image/jpeg", "image/png", "image/webp", "image/gif"];
  if (!allowed.includes(file.type)) {
    return NextResponse.json({ error: "Only JPEG, PNG, WebP, or GIF allowed" }, { status: 415 });
  }

  const timestamp = Math.floor(Date.now() / 1000);
  const folder = "nayabi-collection/products";
  const paramsToSign = `folder=${folder}&timestamp=${timestamp}`;
  const signature = crypto
    .createHash("sha1")
    .update(paramsToSign + secret)
    .digest("hex");

  const upload = new FormData();
  upload.append("file", file);
  upload.append("api_key", key);
  upload.append("timestamp", String(timestamp));
  upload.append("signature", signature);
  upload.append("folder", folder);

  const res = await fetch(`https://api.cloudinary.com/v1_1/${cloud}/image/upload`, {
    method: "POST",
    body: upload,
  });

  if (!res.ok) {
    const err = await res.json();
    console.error("Cloudinary upload failed:", err);
    return NextResponse.json({ error: "Upload failed" }, { status: 502 });
  }

  const data = await res.json();
  return NextResponse.json({ url: data.secure_url as string });
}
