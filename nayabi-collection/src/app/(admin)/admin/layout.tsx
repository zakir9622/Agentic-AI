import { getAdminSession } from "@/lib/admin-auth";
import { AdminLayout } from "@/components/admin/admin-layout";

export default async function AdminDashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // The /admin/login page lives under this segment but must render WITHOUT the
  // authenticated shell. Every other /admin route is gated by proxy.ts (which
  // requires a valid, unexpired session cookie), so a missing session here means
  // we're on the login page — render it bare instead of redirecting to itself.
  const session = await getAdminSession();
  if (!session) return <>{children}</>;

  return (
    <AdminLayout adminName={session.name} adminRole={session.role}>
      {children}
    </AdminLayout>
  );
}
