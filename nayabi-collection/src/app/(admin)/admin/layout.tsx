import { requireAdminSession } from "@/lib/admin-auth";
import { AdminLayout } from "@/components/admin/admin-layout";

export default async function AdminDashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await requireAdminSession();

  return (
    <AdminLayout adminName={session.name} adminRole={session.role}>
      {children}
    </AdminLayout>
  );
}
