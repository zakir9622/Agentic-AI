import Link from "next/link";
import { GlassCard } from "@/components/ui";
import { Megaphone, Truck, Percent, ShieldCheck } from "lucide-react";

export const metadata = { title: "Settings | Admin" };

const settingsLinks = [
  {
    href: "/admin/settings/announcement",
    icon: Megaphone,
    label: "Announcement Bar",
    description: "Manage the sitewide banner shown to all visitors",
  },
  {
    href: "/admin/settings/shipping",
    icon: Truck,
    label: "Shipping Zones",
    description: "Configure flat rates and free shipping thresholds per region",
  },
  {
    href: "/admin/settings/tax",
    icon: Percent,
    label: "Tax & GST",
    description: "GST rate and invoice settings",
  },
  {
    href: "/admin/audit",
    icon: ShieldCheck,
    label: "Audit Log",
    description: "Review admin actions and changes",
  },
];

export default function AdminSettingsPage() {
  return (
    <div className="flex flex-col gap-6 max-w-2xl">
      <h2 className="text-2xl font-semibold text-[var(--color-text-primary)]">Settings</h2>
      <ul className="flex flex-col gap-3" role="list">
        {settingsLinks.map((item) => {
          const Icon = item.icon;
          return (
            <li key={item.href}>
              <Link href={item.href}>
                <GlassCard padding="md" hoverable>
                  <div className="flex items-center gap-4">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--color-gold)]/15">
                      <Icon className="h-5 w-5 text-[var(--color-gold)]" aria-hidden="true" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-[var(--color-text-primary)]">
                        {item.label}
                      </p>
                      <p className="text-xs text-[var(--color-text-secondary)]">
                        {item.description}
                      </p>
                    </div>
                  </div>
                </GlassCard>
              </Link>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
