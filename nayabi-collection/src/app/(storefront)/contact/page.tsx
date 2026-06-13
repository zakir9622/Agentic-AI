"use client";

import * as React from "react";
import { GlassCard, GlassInput, GlassButton } from "@/components/ui";
import { Mail, Phone, MapPin, MessageSquare } from "lucide-react";

const contactDetails = [
  {
    icon: Mail,
    label: "Email",
    value: "support@nayabicollection.com",
    href: "mailto:support@nayabicollection.com",
  },
  {
    icon: Phone,
    label: "WhatsApp",
    value: "+91 98765 43210",
    href: "https://wa.me/919876543210",
  },
  {
    icon: MapPin,
    label: "Location",
    value: "Mumbai, Maharashtra, India",
    href: undefined,
  },
];

export default function ContactPage() {
  const [submitted, setSubmitted] = React.useState(false);
  const [pending, setPending] = React.useState(false);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setPending(true);
    setTimeout(() => {
      setSubmitted(true);
      setPending(false);
    }, 800);
  }

  return (
    <div className="mesh-bg min-h-screen">
      <div className="mx-auto max-w-4xl px-4 sm:px-6 py-16">
        <div className="mb-10 text-center">
          <h1 className="text-4xl text-[var(--color-text-primary)]">
            Contact <span className="gradient-text">Us</span>
          </h1>
          <p className="mt-3 text-[var(--color-text-secondary)]">
            We typically reply within 24 hours, Insha&apos;Allah.
          </p>
        </div>

        <div className="grid gap-8 lg:grid-cols-[1fr_320px]">
          {/* Form */}
          <GlassCard padding="lg">
            {submitted ? (
              <div className="flex flex-col items-center gap-4 text-center py-8">
                <MessageSquare
                  className="h-12 w-12 text-[var(--color-gold)]"
                  aria-hidden="true"
                />
                <div>
                  <p className="text-lg font-semibold text-[var(--color-text-primary)]">
                    Message received!
                  </p>
                  <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
                    We&apos;ll get back to you soon.
                  </p>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <GlassInput label="Name" name="name" required />
                  <GlassInput label="Email" name="email" type="email" required />
                </div>
                <GlassInput
                  label="Mobile number (optional)"
                  name="phone"
                  type="tel"
                  placeholder="9876543210"
                />
                <GlassInput label="Subject" name="subject" required />
                <div className="flex flex-col gap-1.5">
                  <label
                    htmlFor="message"
                    className="text-sm font-medium text-[var(--color-text-primary)]"
                  >
                    Message <span className="text-[var(--color-error)]" aria-hidden="true">*</span>
                  </label>
                  <textarea
                    id="message"
                    name="message"
                    required
                    rows={5}
                    placeholder="How can we help you?"
                    className="glass-input resize-none"
                  />
                </div>
                <GlassButton type="submit" size="lg" fullWidth loading={pending}>
                  Send message
                </GlassButton>
              </form>
            )}
          </GlassCard>

          {/* Contact info */}
          <div className="flex flex-col gap-4">
            {contactDetails.map((c) => {
              const Icon = c.icon;
              return (
                <GlassCard key={c.label} padding="md">
                  <div className="flex items-center gap-3">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-gold)]/15">
                      <Icon className="h-4 w-4 text-[var(--color-gold)]" aria-hidden="true" />
                    </div>
                    <div>
                      <p className="text-xs text-[var(--color-text-muted)]">{c.label}</p>
                      {c.href ? (
                        <a
                          href={c.href}
                          className="text-sm text-[var(--color-text-primary)] hover:text-[var(--color-gold)] transition-colors"
                        >
                          {c.value}
                        </a>
                      ) : (
                        <p className="text-sm text-[var(--color-text-primary)]">{c.value}</p>
                      )}
                    </div>
                  </div>
                </GlassCard>
              );
            })}

            <GlassCard padding="md">
              <p className="text-xs text-[var(--color-text-muted)] mb-1">Business hours</p>
              <p className="text-sm text-[var(--color-text-primary)]">Monday – Saturday</p>
              <p className="text-sm text-[var(--color-text-secondary)]">10:00 AM – 7:00 PM IST</p>
            </GlassCard>
          </div>
        </div>
      </div>
    </div>
  );
}
