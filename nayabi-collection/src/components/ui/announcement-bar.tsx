import * as React from "react";
import Link from "next/link";
import { db } from "@/lib/db";

export async function AnnouncementBar() {
  let bar;
  try {
    const now = new Date();
    bar = await db.announcementBar.findFirst({
      where: {
        isActive: true,
        OR: [
          { startsAt: null },
          { startsAt: { lte: now } },
        ],
        AND: [
          {
            OR: [
              { expiresAt: null },
              { expiresAt: { gte: now } },
            ],
          },
        ],
      },
      orderBy: { createdAt: "desc" },
    });
  } catch {
    return null;
  }

  if (!bar) return null;

  return (
    <div
      className="announcement-bar"
      style={{ backgroundColor: bar.bgColor, color: bar.textColor }}
      role="banner"
    >
      {bar.linkUrl ? (
        <Link
          href={bar.linkUrl}
          className="hover:underline underline-offset-2"
          style={{ color: bar.textColor }}
        >
          {bar.text}
          {bar.linkText && (
            <span className="ml-1.5 font-semibold">{bar.linkText} →</span>
          )}
        </Link>
      ) : (
        bar.text
      )}
    </div>
  );
}
