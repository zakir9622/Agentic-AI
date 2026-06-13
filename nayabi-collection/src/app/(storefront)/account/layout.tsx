import Link from "next/link";
import { redirect } from "next/navigation";
import { auth } from "@/lib/auth";

const accountNav = [
  { label: "Overview", href: "/account" },
  { label: "Orders", href: "/account/orders" },
  { label: "Wishlist", href: "/account/wishlist" },
  { label: "Addresses", href: "/account/addresses" },
  { label: "Security", href: "/account/security" },
];

export default async function AccountLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const session = await auth();
  if (!session?.user) redirect("/login?callbackUrl=/account");

  return (
    <div className="mx-auto max-w-6xl px-4 sm:px-6 py-10">
      <h1 className="text-3xl text-[var(--color-text-primary)]">My Account</h1>
      <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
        Assalamu alaikum, {session.user.name ?? "there"}
      </p>

      <div className="mt-8 grid gap-8 lg:grid-cols-[200px_1fr]">
        <nav aria-label="Account navigation">
          <ul className="flex gap-2 overflow-x-auto lg:flex-col" role="list">
            {accountNav.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className="block whitespace-nowrap rounded-lg px-3 py-2 text-sm text-[var(--color-text-secondary)] transition-colors hover:bg-[var(--color-glass-bg)] hover:text-[var(--color-text-primary)]"
                >
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        <div>{children}</div>
      </div>
    </div>
  );
}
