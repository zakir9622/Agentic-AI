import { db } from "@/lib/db";
import { GlassCard } from "@/components/ui";

export const metadata = { title: "Audit Log | Admin" };

export default async function AdminAuditPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const { page: pageStr } = await searchParams;
  const page = Math.max(1, parseInt(pageStr ?? "1", 10));
  const perPage = 30;

  const [logs, total] = await Promise.all([
    db.auditLog.findMany({
      include: { adminUser: { select: { name: true, email: true } } },
      orderBy: { createdAt: "desc" },
      take: perPage,
      skip: (page - 1) * perPage,
    }),
    db.auditLog.count(),
  ]);

  const totalPages = Math.ceil(total / perPage);

  return (
    <div className="flex flex-col gap-6">
      <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Audit Log</h2>

      <GlassCard padding="md">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Audit log table">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                {["Time", "Admin", "Action", "Entity", "IP"].map((h) => (
                  <th key={h} scope="col" className="pb-2 pr-4 last:pr-0 text-left text-xs font-medium text-[var(--color-text-muted)]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} className="border-b border-[var(--color-glass-border)] last:border-0 hover:bg-[var(--color-glass-bg)]">
                  <td className="py-2.5 pr-4 text-xs text-[var(--color-text-muted)] whitespace-nowrap">
                    {new Date(log.createdAt).toLocaleString("en-IN")}
                  </td>
                  <td className="py-2.5 pr-4 text-[var(--color-text-secondary)]">
                    {log.adminUser.name}
                  </td>
                  <td className="py-2.5 pr-4 font-medium text-[var(--color-text-primary)]">
                    {log.action}
                  </td>
                  <td className="py-2.5 pr-4 text-[var(--color-text-secondary)]">
                    {log.entityType}
                    {log.entityId && (
                      <span className="text-xs text-[var(--color-text-muted)] ml-1 font-mono">
                        #{log.entityId.slice(0, 8)}
                      </span>
                    )}
                  </td>
                  <td className="py-2.5 text-xs text-[var(--color-text-muted)] font-mono">
                    {log.ip ?? "—"}
                  </td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                    No audit events yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-[var(--color-glass-border)]">
            <p className="text-xs text-[var(--color-text-muted)]">
              Showing {(page - 1) * perPage + 1}–{Math.min(page * perPage, total)} of {total}
            </p>
            <div className="flex gap-2">
              {page > 1 && (
                <a href={`/admin/audit?page=${page - 1}`} className="px-3 py-1.5 text-xs glass rounded-lg text-[var(--color-text-secondary)]">Previous</a>
              )}
              {page < totalPages && (
                <a href={`/admin/audit?page=${page + 1}`} className="px-3 py-1.5 text-xs glass rounded-lg text-[var(--color-text-secondary)]">Next</a>
              )}
            </div>
          </div>
        )}
      </GlassCard>
    </div>
  );
}
