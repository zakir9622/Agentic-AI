"use client";

import * as React from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Check, Banknote, CreditCard, Tag } from "lucide-react";
import { GlassButton, GlassCard, GlassInput, toast } from "@/components/ui";
import { useCart } from "@/store/cart";
import { cn, formatPrice, isValidPhone, isValidPincode } from "@/lib/utils";
import {
  INDIAN_STATES,
  FREE_SHIPPING_THRESHOLD,
  FLAT_SHIPPING_RATE,
  COD_CHARGE,
} from "@/lib/constants";

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

type Step = 1 | 2 | 3;
const stepLabels = ["Contact", "Shipping", "Payment"];

export default function CheckoutPage() {
  const router = useRouter();
  const { items, clear } = useCart();
  const [step, setStep] = React.useState<Step>(1);
  const [placing, setPlacing] = React.useState(false);

  const [contact, setContact] = React.useState({ name: "", email: "", phone: "" });
  const [address, setAddress] = React.useState({
    line1: "", line2: "", city: "", state: "", pincode: "",
  });
  const [payMethod, setPayMethod] = React.useState<"RAZORPAY" | "COD">("RAZORPAY");
  const [notes, setNotes] = React.useState("");
  const [discountInput, setDiscountInput] = React.useState("");
  const [discount, setDiscount] = React.useState<{ code: string; amount: number; freeShipping: boolean } | null>(null);
  const [errors, setErrors] = React.useState<Record<string, string>>({});

  const subtotal = items.reduce((s, i) => s + i.price * i.quantity, 0);
  const discountAmount = discount?.amount ?? 0;
  const afterDiscount = subtotal - discountAmount;
  const shipping = discount?.freeShipping || afterDiscount >= FREE_SHIPPING_THRESHOLD ? 0 : FLAT_SHIPPING_RATE;
  const codCharge = payMethod === "COD" ? COD_CHARGE : 0;
  const total = afterDiscount + shipping + codCharge;

  // Load Razorpay script once
  React.useEffect(() => {
    if (document.getElementById("rzp-script")) return;
    const s = document.createElement("script");
    s.id = "rzp-script";
    s.src = "https://checkout.razorpay.com/v1/checkout.js";
    document.body.appendChild(s);
  }, []);

  if (items.length === 0 && !placing) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24 text-center">
        <h1 className="text-3xl text-[var(--color-text-primary)]">Your bag is empty</h1>
        <Link href="/shop" className="mt-6 inline-block">
          <GlassButton>Browse the collection</GlassButton>
        </Link>
      </div>
    );
  }

  function validateContact() {
    const e: Record<string, string> = {};
    if (contact.name.trim().length < 2) e.name = "Please enter your full name";
    if (!/^\S+@\S+\.\S+$/.test(contact.email)) e.email = "Please enter a valid email";
    if (!isValidPhone(contact.phone)) e.phone = "Enter a valid 10-digit mobile number";
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function validateAddress() {
    const e: Record<string, string> = {};
    if (address.line1.trim().length < 3) e.line1 = "Address is required";
    if (address.city.trim().length < 2) e.city = "City is required";
    if (!address.state) e.state = "Select your state";
    if (!isValidPincode(address.pincode)) e.pincode = "Enter a valid 6-digit pincode";
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function continueFromContact() {
    if (!validateContact()) return;
    setStep(2);
    // Record for abandoned-cart recovery (fire and forget)
    void fetch("/api/cart/abandon", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: contact.email,
        phone: contact.phone,
        items: items.map((i) => ({ variantId: i.variantId, quantity: i.quantity })),
      }),
    }).catch(() => {});
  }

  async function applyDiscount() {
    if (!discountInput.trim()) return;
    const res = await fetch("/api/discount/validate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: discountInput.trim().toUpperCase(), subtotal }),
    });
    const data = await res.json();
    if (data.valid) {
      setDiscount({
        code: discountInput.trim().toUpperCase(),
        amount: data.amount ?? 0,
        freeShipping: data.freeShipping ?? false,
      });
      toast.success("Discount applied", discountInput.trim().toUpperCase());
    } else {
      setDiscount(null);
      toast.error("Can't apply code", data.reason ?? "Invalid code");
    }
  }

  async function placeOrder() {
    if (!validateAddress()) { setStep(2); return; }
    setPlacing(true);
    try {
      const res = await fetch("/api/checkout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          items: items.map((i) => ({ variantId: i.variantId, quantity: i.quantity })),
          contact,
          address: { ...address, line2: address.line2 || undefined },
          paymentMethod: payMethod,
          discountCode: discount?.code,
          notes: notes || undefined,
        }),
      });
      const data = await res.json();
      if (!data.success) {
        toast.error("Order failed", data.error);
        setPlacing(false);
        return;
      }

      if (data.cod) {
        clear();
        router.push(`/order-confirmation/${data.orderNumber}`);
        return;
      }

      // Razorpay flow
      if (!window.Razorpay) {
        toast.error("Payment unavailable", "Please refresh and try again.");
        setPlacing(false);
        return;
      }
      const rzp = new window.Razorpay({
        key: data.keyId,
        amount: data.amount,
        currency: "INR",
        name: "Nayabi Collection",
        description: `Order ${data.orderNumber}`,
        order_id: data.razorpayOrderId,
        prefill: data.prefill,
        theme: { color: "#C9A96E" },
        modal: { ondismiss: () => setPlacing(false) },
        handler: async (response: {
          razorpay_order_id: string;
          razorpay_payment_id: string;
          razorpay_signature: string;
        }) => {
          const verify = await fetch("/api/checkout/verify", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            }),
          });
          const v = await verify.json();
          if (v.success) {
            clear();
            router.push(`/order-confirmation/${v.orderNumber}`);
          } else {
            toast.error("Verification failed", "Contact support if you were charged.");
            setPlacing(false);
          }
        },
      });
      rzp.open();
    } catch {
      toast.error("Something went wrong", "Please try again.");
      setPlacing(false);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 sm:px-6 py-10">
      <h1 className="text-3xl sm:text-4xl text-[var(--color-text-primary)]">Checkout</h1>

      {/* Stepper */}
      <ol className="mt-6 flex items-center gap-2" aria-label="Checkout progress">
        {stepLabels.map((label, i) => {
          const n = (i + 1) as Step;
          const state = n < step ? "done" : n === step ? "current" : "todo";
          return (
            <li key={label} className="flex items-center gap-2">
              <button
                onClick={() => n < step && setStep(n)}
                aria-current={state === "current" ? "step" : undefined}
                disabled={state === "todo"}
                className={cn(
                  "flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs transition-colors",
                  state === "done" && "border-[var(--color-success)] text-[var(--color-success)]",
                  state === "current" && "border-[var(--color-gold)] bg-[rgba(201,169,110,0.12)] text-[var(--color-gold)]",
                  state === "todo" && "border-[var(--color-glass-border)] text-[var(--color-text-muted)]"
                )}
              >
                {state === "done" ? <Check className="h-3.5 w-3.5" aria-hidden="true" /> : `${n}.`}
                {label}
              </button>
              {i < stepLabels.length - 1 && (
                <span aria-hidden="true" className="h-px w-6 bg-[var(--color-glass-border)]" />
              )}
            </li>
          );
        })}
      </ol>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_380px]">
        {/* Steps */}
        <div>
          {step === 1 && (
            <GlassCard padding="lg">
              <h2 className="text-lg font-semibold !font-[var(--font-body)]">Contact details</h2>
              <div className="mt-5 flex flex-col gap-4">
                <GlassInput label="Full name" required autoComplete="name" value={contact.name}
                  error={errors.name}
                  onChange={(e) => setContact({ ...contact, name: e.target.value })} />
                <GlassInput label="Email" type="email" required autoComplete="email" value={contact.email}
                  error={errors.email} hint="Order confirmation will be sent here"
                  onChange={(e) => setContact({ ...contact, email: e.target.value })} />
                <GlassInput label="Mobile number" type="tel" required autoComplete="tel" value={contact.phone}
                  error={errors.phone} hint="For delivery updates via SMS / WhatsApp"
                  onChange={(e) => setContact({ ...contact, phone: e.target.value })} />
                <GlassButton size="lg" onClick={continueFromContact}>Continue to shipping</GlassButton>
              </div>
            </GlassCard>
          )}

          {step === 2 && (
            <GlassCard padding="lg">
              <h2 className="text-lg font-semibold !font-[var(--font-body)]">Shipping address</h2>
              <div className="mt-5 flex flex-col gap-4">
                <GlassInput label="Address line 1" required autoComplete="address-line1" value={address.line1}
                  error={errors.line1} placeholder="House no, building, street"
                  onChange={(e) => setAddress({ ...address, line1: e.target.value })} />
                <GlassInput label="Address line 2 (optional)" autoComplete="address-line2" value={address.line2}
                  placeholder="Area, landmark"
                  onChange={(e) => setAddress({ ...address, line2: e.target.value })} />
                <div className="grid gap-4 sm:grid-cols-2">
                  <GlassInput label="City" required autoComplete="address-level2" value={address.city}
                    error={errors.city}
                    onChange={(e) => setAddress({ ...address, city: e.target.value })} />
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="state" className="text-sm font-medium text-[var(--color-text-primary)]">
                      State<span className="text-[var(--color-error)] ml-0.5">*</span>
                    </label>
                    <select id="state" required value={address.state}
                      aria-invalid={!!errors.state}
                      onChange={(e) => setAddress({ ...address, state: e.target.value })}
                      className="glass-input">
                      <option value="" className="bg-[var(--color-bg-mid)]">Select state</option>
                      {INDIAN_STATES.map((s) => (
                        <option key={s} value={s} className="bg-[var(--color-bg-mid)]">{s}</option>
                      ))}
                    </select>
                    {errors.state && <p role="alert" className="text-xs text-[var(--color-error)]">{errors.state}</p>}
                  </div>
                </div>
                <GlassInput label="Pincode" required inputMode="numeric" autoComplete="postal-code"
                  maxLength={6} value={address.pincode} error={errors.pincode}
                  onChange={(e) => setAddress({ ...address, pincode: e.target.value.replace(/\D/g, "") })} />
                <GlassInput label="Order notes (optional)" value={notes}
                  placeholder="e.g. Call before delivery"
                  onChange={(e) => setNotes(e.target.value)} />
                <GlassButton size="lg" onClick={() => validateAddress() && setStep(3)}>
                  Continue to payment
                </GlassButton>
              </div>
            </GlassCard>
          )}

          {step === 3 && (
            <GlassCard padding="lg">
              <h2 className="text-lg font-semibold !font-[var(--font-body)]">Payment method</h2>
              <div className="mt-5 flex flex-col gap-3" role="radiogroup" aria-label="Payment method">
                <PaymentOption
                  selected={payMethod === "RAZORPAY"}
                  onSelect={() => setPayMethod("RAZORPAY")}
                  icon={<CreditCard className="h-5 w-5" aria-hidden="true" />}
                  title="Pay online"
                  subtitle="UPI · Cards · Net Banking · Wallets"
                />
                <PaymentOption
                  selected={payMethod === "COD"}
                  onSelect={() => setPayMethod("COD")}
                  icon={<Banknote className="h-5 w-5" aria-hidden="true" />}
                  title="Cash on Delivery"
                  subtitle={`Pay when it arrives (+${formatPrice(COD_CHARGE)} COD fee)`}
                />
              </div>
              <GlassButton size="lg" fullWidth className="mt-6" loading={placing} onClick={placeOrder}>
                {payMethod === "COD" ? `Place order — ${formatPrice(total)}` : `Pay ${formatPrice(total)}`}
              </GlassButton>
              <p className="mt-3 text-center text-xs text-[var(--color-text-muted)]">
                Secured by Razorpay · 7-day easy returns
              </p>
            </GlassCard>
          )}
        </div>

        {/* Order summary */}
        <aside aria-label="Order summary">
          <GlassCard padding="md" className="sticky top-24">
            <h2 className="text-base font-semibold !font-[var(--font-body)]">Order summary</h2>
            <ul className="mt-4 flex flex-col gap-3" role="list">
              {items.map((i) => (
                <li key={i.variantId} className="flex items-center gap-3">
                  <div className="relative h-14 w-11 shrink-0 overflow-hidden rounded-md bg-[var(--color-bg-mid)]">
                    {i.imageUrl && <Image src={i.imageUrl} alt="" fill sizes="44px" className="object-cover" />}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm text-[var(--color-text-primary)]">{i.productName}</p>
                    <p className="text-xs text-[var(--color-text-muted)]">{i.variantLabel} × {i.quantity}</p>
                  </div>
                  <p className="text-sm text-[var(--color-text-secondary)]">{formatPrice(i.price * i.quantity)}</p>
                </li>
              ))}
            </ul>

            {/* Discount code */}
            <div className="mt-4 flex gap-2">
              <input
                value={discountInput}
                onChange={(e) => setDiscountInput(e.target.value)}
                placeholder="Discount code"
                aria-label="Discount code"
                className="glass-input !py-2 text-sm"
              />
              <GlassButton variant="secondary" size="sm" onClick={applyDiscount}
                icon={<Tag className="h-3.5 w-3.5" aria-hidden="true" />}>
                Apply
              </GlassButton>
            </div>

            <dl className="mt-4 flex flex-col gap-1.5 border-t border-[var(--color-glass-border)] pt-4 text-sm">
              <Row label="Subtotal" value={formatPrice(subtotal)} />
              {discountAmount > 0 && (
                <Row label={`Discount (${discount!.code})`} value={`−${formatPrice(discountAmount)}`} accent="success" />
              )}
              <Row label="Shipping" value={shipping === 0 ? "Free" : formatPrice(shipping)} accent={shipping === 0 ? "success" : undefined} />
              {codCharge > 0 && <Row label="COD fee" value={formatPrice(codCharge)} />}
              <div className="mt-2 flex justify-between border-t border-[var(--color-glass-border)] pt-3">
                <dt className="font-semibold text-[var(--color-text-primary)]">Total</dt>
                <dd className="text-lg font-bold text-[var(--color-gold)]">{formatPrice(total)}</dd>
              </div>
              <p className="text-xs text-[var(--color-text-muted)]">Inclusive of all taxes (GST)</p>
            </dl>
          </GlassCard>
        </aside>
      </div>
    </div>
  );
}

function Row({ label, value, accent }: { label: string; value: string; accent?: "success" }) {
  return (
    <div className="flex justify-between">
      <dt className="text-[var(--color-text-secondary)]">{label}</dt>
      <dd className={accent === "success" ? "text-[var(--color-success)]" : "text-[var(--color-text-primary)]"}>
        {value}
      </dd>
    </div>
  );
}

function PaymentOption({
  selected, onSelect, icon, title, subtitle,
}: {
  selected: boolean;
  onSelect: () => void;
  icon: React.ReactNode;
  title: string;
  subtitle: string;
}) {
  return (
    <button
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className={cn(
        "flex items-center gap-4 rounded-xl border p-4 text-left transition-colors",
        selected
          ? "border-[var(--color-gold)] bg-[rgba(201,169,110,0.10)]"
          : "border-[var(--color-glass-border)] hover:border-[var(--color-gold)]"
      )}
    >
      <span className={selected ? "text-[var(--color-gold)]" : "text-[var(--color-text-muted)]"}>{icon}</span>
      <span>
        <span className="block text-sm font-medium text-[var(--color-text-primary)]">{title}</span>
        <span className="block text-xs text-[var(--color-text-secondary)]">{subtitle}</span>
      </span>
      <span
        aria-hidden="true"
        className={cn(
          "ml-auto h-4 w-4 rounded-full border-2",
          selected ? "border-[var(--color-gold)] bg-[var(--color-gold)]" : "border-[var(--color-glass-border)]"
        )}
      />
    </button>
  );
}
