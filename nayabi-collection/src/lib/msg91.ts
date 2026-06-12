/* MSG91 SMS + WhatsApp helpers.
   All functions fail silently (log only) so notification issues never break checkout. */

const AUTH_KEY = () => process.env.MSG91_AUTH_KEY;

export async function sendSMS(phone: string, templateId: string, vars: Record<string, string>) {
  const key = AUTH_KEY();
  if (!key || !templateId) return;
  try {
    await fetch("https://control.msg91.com/api/v5/flow/", {
      method: "POST",
      headers: { authkey: key, "Content-Type": "application/json" },
      body: JSON.stringify({
        template_id: templateId,
        recipients: [{ mobiles: `91${phone.replace(/\D/g, "").slice(-10)}`, ...vars }],
      }),
    });
  } catch (e) {
    console.error("MSG91 SMS failed:", e);
  }
}

export async function sendWhatsApp(phone: string, templateName: string, bodyParams: string[]) {
  const key = AUTH_KEY();
  const from = process.env.MSG91_WHATSAPP_NUMBER;
  if (!key || !from) return;
  try {
    await fetch("https://control.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/", {
      method: "POST",
      headers: { authkey: key, "Content-Type": "application/json" },
      body: JSON.stringify({
        integrated_number: from,
        content_type: "template",
        payload: {
          messaging_product: "whatsapp",
          type: "template",
          template: {
            name: templateName,
            language: { code: "en", policy: "deterministic" },
            namespace: null,
            to_and_components: [
              {
                to: [`91${phone.replace(/\D/g, "").slice(-10)}`],
                components: {
                  body_1: { type: "text", value: bodyParams[0] ?? "" },
                  body_2: { type: "text", value: bodyParams[1] ?? "" },
                  body_3: { type: "text", value: bodyParams[2] ?? "" },
                },
              },
            ],
          },
        },
      }),
    });
  } catch (e) {
    console.error("MSG91 WhatsApp failed:", e);
  }
}

/* ── Event helpers ──────────────────────────────────────────────────────────── */

export async function notifyOrderPlaced(phone: string | null, orderNumber: string, totalRupees: string, isCOD: boolean) {
  if (!phone) return;
  await Promise.all([
    sendSMS(phone, process.env.MSG91_SMS_TEMPLATE_ORDER_PLACED ?? "", {
      var1: orderNumber,
      var2: totalRupees,
    }),
    sendWhatsApp(phone, isCOD ? "order_placed_cod" : "order_placed", [orderNumber, totalRupees]),
  ]);
}

export async function notifyOrderShipped(phone: string | null, orderNumber: string, trackingUrl: string) {
  if (!phone) return;
  await Promise.all([
    sendSMS(phone, process.env.MSG91_SMS_TEMPLATE_SHIPPED ?? "", {
      var1: orderNumber,
      var2: trackingUrl,
    }),
    sendWhatsApp(phone, "order_shipped", [orderNumber, trackingUrl]),
  ]);
}
