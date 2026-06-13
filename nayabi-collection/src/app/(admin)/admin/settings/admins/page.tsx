"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput, GlassBadge } from "@/components/ui";
import {
  createAdminUserAction,
  deactivateAdminUserAction,
  reactivateAdminUserAction,
  type ActionState,
} from "@/app/actions/admin-users";
import { UserPlus, ShieldOff, ShieldCheck } from "lucide-react";

interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: string;
  lockedUntil: string | null;
  lastLoginAt: string | null;
  createdAt: string;
}

export default function AdminUsersPage() {
  const [users, setUsers] = React.useState<AdminUser[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [showForm, setShowForm] = React.useState(false);
  const [createState, createAction, createPending] = React.useActionState<ActionState, FormData>(
    createAdminUserAction,
    {}
  );
  const [deactivateState, deactivateAction] = React.useActionState<ActionState, FormData>(
    deactivateAdminUserAction,
    {}
  );
  const [reactivateState, reactivateAction] = React.useActionState<ActionState, FormData>(
    reactivateAdminUserAction,
    {}
  );

  const prevCreateOk = React.useRef(createState.ok);
  React.useEffect(() => {
    let active = true;
    fetch("/api/admin/users")
      .then((r) => r.json())
      .then((data) => {
        if (active) {
          setUsers(data);
          setLoading(false);
          if (createState.ok && !prevCreateOk.current) setShowForm(false);
          prevCreateOk.current = createState.ok;
        }
      })
      .catch(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [createState, deactivateState, reactivateState, createState.ok]);

  const isLocked = (u: AdminUser) =>
    u.lockedUntil && new Date(u.lockedUntil) > new Date();

  return (
    <div className="flex flex-col gap-6 max-w-3xl">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Admin Users</h2>
        <GlassButton size="sm" onClick={() => setShowForm((v) => !v)}>
          <UserPlus className="h-4 w-4 mr-1.5" aria-hidden="true" />
          {showForm ? "Cancel" : "Add admin"}
        </GlassButton>
      </div>

      {/* Create form */}
      {showForm && (
        <GlassCard padding="md">
          <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            New admin account
          </h3>
          <form action={createAction} className="flex flex-col gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <GlassInput
                label="Full name" name="name" required
                error={createState.errors?.name}
              />
              <GlassInput
                label="Email" name="email" type="email" required
                error={createState.errors?.email}
              />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <GlassInput
                label="Password" name="password" type="password" required
                hint="Min 8 chars, include a letter and a number"
                error={createState.errors?.password}
              />
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[var(--color-text-primary)]">Role</label>
                <select name="role" defaultValue="MANAGER" className="glass-input">
                  <option value="SUPER_ADMIN">Super Admin</option>
                  <option value="MANAGER">Manager</option>
                  <option value="FULFILLMENT">Fulfillment</option>
                </select>
              </div>
            </div>
            {createState.message && (
              <p className={`text-sm ${createState.ok ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
                {createState.message}
              </p>
            )}
            <GlassButton type="submit" size="sm" loading={createPending}>
              Create admin
            </GlassButton>
          </form>
        </GlassCard>
      )}

      {/* Users list */}
      <GlassCard padding="md">
        {loading ? (
          <p className="text-sm text-[var(--color-text-muted)] py-4 text-center">Loading…</p>
        ) : users.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)] py-4 text-center">No admin users found.</p>
        ) : (
          <ul className="divide-y divide-[var(--color-glass-border)]" role="list">
            {users.map((u) => (
              <li key={u.id} className="flex flex-col gap-2 py-4 sm:flex-row sm:items-center sm:justify-between first:pt-0 last:pb-0">
                <div>
                  <p className="text-sm font-medium text-[var(--color-text-primary)]">{u.name}</p>
                  <p className="text-xs text-[var(--color-text-muted)]">{u.email}</p>
                  <div className="mt-1 flex items-center gap-2">
                    <GlassBadge variant="info">{u.role}</GlassBadge>
                    {isLocked(u) ? (
                      <GlassBadge variant="error">Deactivated</GlassBadge>
                    ) : (
                      <GlassBadge variant="success">Active</GlassBadge>
                    )}
                  </div>
                  {u.lastLoginAt && (
                    <p className="mt-1 text-xs text-[var(--color-text-muted)]">
                      Last login: {new Date(u.lastLoginAt).toLocaleDateString("en-IN")}
                    </p>
                  )}
                </div>
                <div className="flex gap-2 shrink-0">
                  {isLocked(u) ? (
                    <form action={reactivateAction}>
                      <input type="hidden" name="adminId" value={u.id} />
                      <GlassButton type="submit" size="sm" variant="ghost">
                        <ShieldCheck className="h-3.5 w-3.5 mr-1" aria-hidden="true" />
                        Reactivate
                      </GlassButton>
                    </form>
                  ) : (
                    <form action={deactivateAction}>
                      <input type="hidden" name="adminId" value={u.id} />
                      <GlassButton
                        type="submit"
                        size="sm"
                        variant="ghost"
                        className="text-[var(--color-error)]"
                        onClick={(e) => {
                          if (!confirm(`Deactivate ${u.name}?`)) e.preventDefault();
                        }}
                      >
                        <ShieldOff className="h-3.5 w-3.5 mr-1" aria-hidden="true" />
                        Deactivate
                      </GlassButton>
                    </form>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </GlassCard>

      {(deactivateState.message || reactivateState.message) && (
        <p className={`text-sm ${(deactivateState.ok || reactivateState.ok) ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
          {deactivateState.message ?? reactivateState.message}
        </p>
      )}
    </div>
  );
}
