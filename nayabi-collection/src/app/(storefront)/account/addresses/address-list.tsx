"use client";

import * as React from "react";
import { GlassCard, GlassButton, GlassInput, GlassBadge } from "@/components/ui";
import { saveAddressAction, deleteAddressAction, type ActionState } from "@/app/actions/account";
import { INDIAN_STATES } from "@/lib/constants";
import { MapPin, Plus, Trash2, Pencil, Star } from "lucide-react";

interface Address {
  id: string;
  name: string;
  phone: string;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  pincode: string;
  isDefault: boolean;
}

interface AddressListProps {
  addresses: Address[];
}

export function AddressList({ addresses: initialAddresses }: AddressListProps) {
  const [addresses, setAddresses] = React.useState(initialAddresses);
  const [editing, setEditing] = React.useState<Address | null>(null);
  const [adding, setAdding] = React.useState(false);
  const [state, formAction, pending] = React.useActionState<ActionState, FormData>(
    saveAddressAction,
    {}
  );
  const [deleteMsg, setDeleteMsg] = React.useState("");

  const [lastState, setLastState] = React.useState(state);
  if (state !== lastState) {
    setLastState(state);
    if (state.ok) {
      setEditing(null);
      setAdding(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm("Delete this address?")) return;
    const result = await deleteAddressAction(id);
    if (result.ok) {
      setAddresses((prev) => prev.filter((a) => a.id !== id));
    } else {
      setDeleteMsg(result.message ?? "Failed to delete");
    }
  }

  const showForm = adding || editing !== null;

  return (
    <div className="flex flex-col gap-4">
      {deleteMsg && (
        <p role="alert" className="text-sm text-[var(--color-error)]">{deleteMsg}</p>
      )}

      {addresses.length === 0 && !showForm && (
        <GlassCard padding="lg" className="flex flex-col items-center gap-4 text-center">
          <MapPin className="h-10 w-10 text-[var(--color-text-muted)]" aria-hidden="true" />
          <div>
            <p className="text-[var(--color-text-primary)] font-medium">No saved addresses</p>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              Add an address to speed up checkout.
            </p>
          </div>
        </GlassCard>
      )}

      <ul className="flex flex-col gap-3" role="list">
        {addresses.map((addr) => (
          <li key={addr.id}>
            <GlassCard padding="md">
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-3">
                  <MapPin className="h-4 w-4 mt-0.5 shrink-0 text-[var(--color-gold)]" aria-hidden="true" />
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                        {addr.name}
                      </p>
                      {addr.isDefault && (
                        <GlassBadge variant="gold">
                          <Star className="h-3 w-3 mr-0.5" aria-hidden="true" />
                          Default
                        </GlassBadge>
                      )}
                    </div>
                    <address className="not-italic mt-1 text-xs text-[var(--color-text-secondary)] leading-relaxed">
                      {addr.line1}
                      {addr.line2 && `, ${addr.line2}`}
                      <br />
                      {addr.city}, {addr.state} – {addr.pincode}
                      <br />
                      +91 {addr.phone}
                    </address>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    type="button"
                    onClick={() => { setAdding(false); setEditing(addr); }}
                    aria-label={`Edit address for ${addr.name}`}
                    className="p-1.5 text-[var(--color-text-muted)] hover:text-[var(--color-gold)] transition-colors rounded"
                  >
                    <Pencil className="h-4 w-4" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(addr.id)}
                    aria-label={`Delete address for ${addr.name}`}
                    className="p-1.5 text-[var(--color-text-muted)] hover:text-[var(--color-error)] transition-colors rounded"
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </button>
                </div>
              </div>
            </GlassCard>
          </li>
        ))}
      </ul>

      {showForm ? (
        <GlassCard padding="md">
          <h3 className="mb-4 text-sm font-semibold text-[var(--color-text-primary)]">
            {editing ? "Edit address" : "New address"}
          </h3>
          <form action={formAction} className="flex flex-col gap-4">
            {editing && <input type="hidden" name="addressId" value={editing.id} />}
            <div className="grid gap-4 sm:grid-cols-2">
              <GlassInput
                label="Full name" name="name" required
                defaultValue={editing?.name}
                error={state.errors?.name}
              />
              <GlassInput
                label="Mobile number" name="phone" type="tel" required
                defaultValue={editing?.phone}
                error={state.errors?.phone}
              />
            </div>
            <GlassInput
              label="Address line 1" name="line1" required
              placeholder="Flat / House no., Building, Street"
              defaultValue={editing?.line1}
              error={state.errors?.line1}
            />
            <GlassInput
              label="Address line 2 (optional)" name="line2"
              placeholder="Area, Colony, Landmark"
              defaultValue={editing?.line2 ?? ""}
            />
            <div className="grid gap-4 sm:grid-cols-3">
              <GlassInput
                label="City" name="city" required
                defaultValue={editing?.city}
                error={state.errors?.city}
              />
              <div className="flex flex-col gap-1.5">
                <label htmlFor="state-select" className="text-sm font-medium text-[var(--color-text-primary)]">
                  State <span className="text-[var(--color-error)]" aria-hidden="true">*</span>
                </label>
                <select
                  id="state-select"
                  name="state"
                  required
                  defaultValue={editing?.state ?? ""}
                  className="glass-input"
                >
                  <option value="">Select state</option>
                  {INDIAN_STATES.map((s) => (
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                {state.errors?.state && (
                  <p role="alert" className="text-xs text-[var(--color-error)]">{state.errors.state}</p>
                )}
              </div>
              <GlassInput
                label="Pincode" name="pincode" required maxLength={6}
                defaultValue={editing?.pincode}
                error={state.errors?.pincode}
              />
            </div>
            <label className="flex items-center gap-2 text-sm text-[var(--color-text-secondary)] cursor-pointer">
              <input
                type="checkbox"
                name="isDefault"
                defaultChecked={editing?.isDefault}
                className="h-4 w-4 rounded border-[var(--color-glass-border)] accent-[var(--color-gold)]"
              />
              Set as default address
            </label>

            {state.message && !state.ok && (
              <p role="alert" className="text-sm text-[var(--color-error)]">{state.message}</p>
            )}

            <div className="flex gap-3">
              <GlassButton type="submit" size="sm" loading={pending}>
                Save address
              </GlassButton>
              <GlassButton
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => { setEditing(null); setAdding(false); }}
              >
                Cancel
              </GlassButton>
            </div>
          </form>
        </GlassCard>
      ) : (
        <GlassButton
          type="button"
          variant="secondary"
          size="sm"
          onClick={() => { setEditing(null); setAdding(true); }}
          className="self-start"
        >
          <Plus className="h-4 w-4 mr-1.5" aria-hidden="true" />
          Add new address
        </GlassButton>
      )}
    </div>
  );
}
