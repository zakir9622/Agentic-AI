"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput } from "@/components/ui";
import { saveAnnouncementAction } from "@/app/actions/admin-settings";

interface Announcement {
  id: string;
  text: string;
  bgColor: string;
  textColor: string;
  linkUrl: string | null;
  linkText: string | null;
  isActive: boolean;
}

interface ActionState {
  ok?: boolean;
  message?: string;
}

export default function AnnouncementAdminPage() {
  const [announcements, setAnnouncements] = React.useState<Announcement[]>([]);
  const [editing, setEditing] = React.useState<Partial<Announcement> | null>(null);
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    saveAnnouncementAction,
    {}
  );

  React.useEffect(() => {
    fetch("/api/admin/announcements")
      .then((r) => r.json())
      .then(setAnnouncements)
      .catch(() => {});
  }, [state]);

  const [lastState, setLastState] = React.useState(state);
  if (state !== lastState) {
    setLastState(state);
    if (state.ok) setEditing(null);
  }

  return (
    <div className="flex flex-col gap-6 max-w-2xl">
      <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">
        Announcement Bar
      </h2>

      <GlassCard padding="md">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">
            Announcements
          </h3>
          <GlassButton type="button" size="sm" onClick={() => setEditing({})}>
            + New
          </GlassButton>
        </div>
        <ul className="flex flex-col divide-y divide-[var(--color-glass-border)]" role="list">
          {announcements.map((a) => (
            <li key={a.id} className="flex items-center justify-between gap-4 py-3">
              <div>
                <p className="text-sm text-[var(--color-text-primary)]">{a.text}</p>
                <span
                  className={`text-xs ${a.isActive ? "text-[var(--color-success)]" : "text-[var(--color-text-muted)]"}`}
                >
                  {a.isActive ? "Active" : "Inactive"}
                </span>
              </div>
              <GlassButton type="button" size="sm" variant="ghost" onClick={() => setEditing(a)}>
                Edit
              </GlassButton>
            </li>
          ))}
          {announcements.length === 0 && (
            <li className="py-6 text-center text-sm text-[var(--color-text-muted)]">
              No announcements yet
            </li>
          )}
        </ul>
      </GlassCard>

      {editing !== null && (
        <GlassCard padding="md">
          <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            {editing.id ? "Edit announcement" : "New announcement"}
          </h3>
          <form action={formAction} className="flex flex-col gap-4">
            {editing.id && <input type="hidden" name="id" value={editing.id} />}
            <GlassInput label="Text" name="text" required defaultValue={editing.text ?? ""} />
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[var(--color-text-primary)]">
                  Background color
                </label>
                <input
                  type="color"
                  name="bgColor"
                  defaultValue={editing.bgColor ?? "#C9A96E"}
                  className="h-10 w-full rounded-lg border border-[var(--color-glass-border)] bg-transparent cursor-pointer"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-[var(--color-text-primary)]">
                  Text color
                </label>
                <input
                  type="color"
                  name="textColor"
                  defaultValue={editing.textColor ?? "#0A0A14"}
                  className="h-10 w-full rounded-lg border border-[var(--color-glass-border)] bg-transparent cursor-pointer"
                />
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <GlassInput
                label="Link URL (optional)"
                name="linkUrl"
                defaultValue={editing.linkUrl ?? ""}
              />
              <GlassInput
                label="Link text (optional)"
                name="linkText"
                defaultValue={editing.linkText ?? ""}
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] cursor-pointer">
              <input
                type="checkbox"
                name="isActive"
                defaultChecked={editing.isActive}
                className="h-4 w-4 rounded accent-[var(--color-gold)]"
              />
              Active (show on site)
            </label>
            {state.message && (
              <p className={`text-sm ${state.ok ? "text-[var(--color-success)]" : "text-[var(--color-error)]"}`}>
                {state.message}
              </p>
            )}
            <div className="flex gap-3">
              <GlassButton type="submit" size="sm" loading={pending}>
                Save
              </GlassButton>
              <GlassButton
                type="button"
                size="sm"
                variant="ghost"
                onClick={() => setEditing(null)}
              >
                Cancel
              </GlassButton>
            </div>
          </form>
        </GlassCard>
      )}
    </div>
  );
}
