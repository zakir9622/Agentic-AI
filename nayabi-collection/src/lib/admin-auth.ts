import { cookies } from "next/headers";

const COOKIE_NAME = "nc_admin";
const SESSION_TTL_MS = 8 * 60 * 60 * 1000; // 8 hours

export interface AdminSession {
  adminId: string;
  email: string;
  name: string;
  role: string;
  expiresAt: number;
}

export async function getAdminSession(): Promise<AdminSession | null> {
  const jar = await cookies();
  const raw = jar.get(COOKIE_NAME)?.value;
  if (!raw) return null;

  let session: AdminSession;
  try {
    session = JSON.parse(Buffer.from(raw, "base64").toString("utf8"));
  } catch {
    return null;
  }

  if (!session.expiresAt || Date.now() > session.expiresAt) return null;
  return session;
}

export async function requireAdminSession(): Promise<AdminSession> {
  const session = await getAdminSession();
  if (!session) {
    const { redirect } = await import("next/navigation");
    redirect("/admin/login");
    // unreachable but satisfies TypeScript
    throw new Error("Unauthenticated");
  }
  return session;
}

export function buildAdminCookie(session: AdminSession): string {
  const encoded = Buffer.from(JSON.stringify(session)).toString("base64");
  const maxAge = Math.floor(SESSION_TTL_MS / 1000);
  return `${COOKIE_NAME}=${encoded}; HttpOnly; SameSite=Strict; Path=/admin; Max-Age=${maxAge}`;
}

export function clearAdminCookie(): string {
  return `${COOKIE_NAME}=; HttpOnly; SameSite=Strict; Path=/admin; Max-Age=0`;
}

export function createAdminSession(admin: {
  id: string;
  email: string;
  name: string;
  role: string;
}): AdminSession {
  return {
    adminId: admin.id,
    email: admin.email,
    name: admin.name,
    role: admin.role,
    expiresAt: Date.now() + SESSION_TTL_MS,
  };
}
