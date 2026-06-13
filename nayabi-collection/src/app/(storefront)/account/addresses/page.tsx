import { auth } from "@/lib/auth";
import { db } from "@/lib/db";
import { GlassCard } from "@/components/ui";
import { AddressList } from "./address-list";

export const metadata = { title: "My Addresses" };

export default async function AddressesPage() {
  const session = await auth();
  let addresses: Awaited<ReturnType<typeof fetchAddresses>> = [];
  try {
    addresses = await fetchAddresses(session!.user.id);
  } catch {
    addresses = [];
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Saved Addresses</h2>
      <AddressList addresses={addresses} />
    </div>
  );
}

function fetchAddresses(userId: string) {
  return db.address.findMany({
    where: { userId },
    orderBy: [{ isDefault: "desc" }, { id: "asc" }],
  });
}
