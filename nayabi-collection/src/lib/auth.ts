import NextAuth, { type DefaultSession } from "next-auth";
import Credentials from "next-auth/providers/credentials";
import Google from "next-auth/providers/google";
import bcrypt from "bcryptjs";
import { db } from "@/lib/db";

declare module "next-auth" {
  interface Session {
    user: { id: string } & DefaultSession["user"];
  }
}

/* Lightweight per-instance login attempt limiter (5 tries / 15 min per email) */
const attempts = new Map<string, { count: number; resetAt: number }>();
function isLockedOut(email: string): boolean {
  const a = attempts.get(email);
  if (!a) return false;
  if (Date.now() > a.resetAt) {
    attempts.delete(email);
    return false;
  }
  return a.count >= 5;
}
function recordFailure(email: string) {
  const a = attempts.get(email);
  if (a && Date.now() <= a.resetAt) a.count++;
  else attempts.set(email, { count: 1, resetAt: Date.now() + 15 * 60 * 1000 });
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  secret: process.env.NEXTAUTH_SECRET ?? process.env.AUTH_SECRET,
  trustHost: true,
  session: { strategy: "jwt" },
  pages: { signIn: "/login", error: "/login" },
  providers: [
    Google({
      clientId: process.env.GOOGLE_CLIENT_ID ?? "",
      clientSecret: process.env.GOOGLE_CLIENT_SECRET ?? "",
      allowDangerousEmailAccountLinking: true,
    }),
    Credentials({
      credentials: { email: {}, password: {} },
      authorize: async (creds) => {
        const email = String(creds?.email ?? "").toLowerCase().trim();
        const password = String(creds?.password ?? "");
        if (!email || !password || isLockedOut(email)) return null;

        const user = await db.user.findUnique({ where: { email } });
        if (!user?.hashedPassword) {
          recordFailure(email);
          return null;
        }
        const valid = await bcrypt.compare(password, user.hashedPassword);
        if (!valid) {
          recordFailure(email);
          return null;
        }
        attempts.delete(email);
        return { id: user.id, name: user.name, email: user.email, image: user.image };
      },
    }),
  ],
  callbacks: {
    async signIn({ user, account, profile }) {
      if (account?.provider === "google" && profile?.email) {
        const dbUser = await db.user.upsert({
          where: { email: profile.email.toLowerCase() },
          create: {
            email: profile.email.toLowerCase(),
            name: profile.name ?? "Customer",
            image: typeof profile.picture === "string" ? profile.picture : null,
            emailVerified: new Date(),
          },
          update: { emailVerified: new Date() },
        });
        user.id = dbUser.id;
      }
      return true;
    },
    async jwt({ token, user }) {
      if (user?.id) token.id = user.id;
      return token;
    },
    async session({ session, token }) {
      if (session.user && typeof token.id === "string") session.user.id = token.id;
      return session;
    },
  },
});
