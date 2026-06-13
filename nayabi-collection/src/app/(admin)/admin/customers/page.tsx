import { db } from "@/lib/db";
import { GlassCard } from "@/components/ui";

export const metadata = { title: "Customers | Admin" };

export default async function AdminCustomersPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string; page?: string }>;
}) {
  const { q, page: pageStr } = await searchParams;
  const page = Math.max(1, parseInt(pageStr ?? "1", 10));
  const perPage = 25;

  const where = q
    ? {
        OR: [
          { name: { contains: q, mode: "insensitive" as const } },
          { email: { contains: q, mode: "insensitive" as const } },
        ],
      }
    : {};

  const [customers, total] = await Promise.all([
    db.user.findMany({
      where,
      include: {
        _count: { select: { orders: true } },
      },
      orderBy: { createdAt: "desc" },
      take: perPage,
      skip: (page - 1) * perPage,
    }),
    db.user.count({ where }),
  ]);

  const totalPages = Math.ceil(total / perPage);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">
          Customers ({total})
        </h2>
        <form method="GET" className="flex gap-2">
          <input name="q" defaultValue={q} placeholder="Search by name or email…" className="glass-input text-sm w-56" />
          <button type="submit" className="glass px-3 py-2 text-sm rounded-lg text-[var(--color-text-primary)]">
            Search
          </button>
        </form>
      </div>

      <GlassCard padding="md">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" aria-label="Customers table">
            <thead>
              <tr className="border-b border-[var(--color-glass-border)]">
                {["Name", "Email", "Phone", "Orders", "Joined", "Verified"].map((h) => (
                  <th key={h} scope="col" className="pb-2 pr-4 last:pr-0 text-left text-xs font-medium text-[var(--color-text-muted)]">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {customers.map((c) => (
                <tr key={c.id} className="border-b border-[var(--color-glass-border)] last:border-0 hover:bg-[var(--color-glass-bg)]">
                  <td className="py-3 pr-4 font-medium text-[var(--color-text-primary)]">
                    {c.name}
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)]">{c.email}</td>
                  <td className="py-3 pr-4 text-[var(--color-text-secondary)]">
                    {c.phone ?? "—"}
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-primary)]">
                    {c._count.orders}
                  </td>
                  <td className="py-3 pr-4 text-[var(--color-text-muted)] text-xs">
                    {new Date(c.createdAt).toLocaleDateString("en-IN")}
                  </td>
                  <td className="py-3 pr-4">
                    <span className={`text-xs ${c.emailVerified ? "text-[var(--color-success)]" : "text-[var(--color-warning)]"}`}>
                      {c.emailVerified ? "Yes" : "No"}
                    </span>
                  </td>
                </tr>
              ))}
              {customers.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-sm text-[var(--color-text-muted)]">
                    No customers found
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
                <a href={`/admin/customers?${q ? `q=${q}&` : ""}page=${page - 1}`}
                   className="px-3 py-1.5 text-xs glass rounded-lg text-[var(--color-text-secondary)]">
                  Previous
                </a>
              )}
              {page < totalPages && (
                <a href={`/admin/customers?${q ? `q=${q}&` : ""}page=${page + 1}`}
                   className="px-3 py-1.5 text-xs glass rounded-lg text-[var(--color-text-secondary)]">
                  Next
                </a>
              )}
            </div>
          </div>
        )}
      </GlassCard>
    </div>
  );
}
